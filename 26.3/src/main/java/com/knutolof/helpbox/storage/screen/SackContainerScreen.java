package com.knutolof.helpbox.storage.screen;
import com.knutolof.helpbox.storage.StorageInitializer;

import com.daqem.uilib.gui.AbstractContainerScreen;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;

import com.knutolof.helpbox.storage.config.EnhancedStorageConfig;
import com.knutolof.helpbox.storage.config.SackConfig;
import com.knutolof.helpbox.storage.gui.SackOverlayLayout;
import com.knutolof.helpbox.storage.gui.SackOverlayState;
import com.knutolof.helpbox.mixin.AbstractContainerScreenAccessor;
import com.knutolof.helpbox.storage.gui.component.SackCardComponent;
import com.knutolof.helpbox.storage.storage.ContainerContentTracker;
import com.knutolof.helpbox.storage.storage.SackCache;
import com.knutolof.helpbox.storage.storage.SackKey;
import com.knutolof.helpbox.storage.util.TextUtils;
import com.knutolof.helpbox.ui.ModernUiRenderHelper;
import com.knutolof.helpbox.util.HelpBoxLang;
import eu.midnightdust.lib.config.MidnightConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class SackContainerScreen extends AbstractContainerScreen<ChestMenu> implements IHighlightClipProvider {

    private static final long SEARCH_REBUILD_DELAY_MS = 150;
    private final SackKey openKey;
    private final SackOverlayState state = SackOverlayState.session();
    private final SackOverlayLayout layout = new SackOverlayLayout();
    private boolean autoScrolledToOpenCard = false;
    private final Map<Long, int[]> topSlotClipRects = new HashMap<>();
    private long searchRebuildDueAt = -1;

    /** Sack key that we need to auto-click once items arrive in the SACK_INDEX container. */
    private SackKey pendingAutoOpen = null;

    private SackKey draggedCardKey = null;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private boolean isDraggingCard = false;

    public SackContainerScreen(ChestMenu menu, Inventory inventory, Component title, SackKey openKey) {
        super(menu, inventory, title);
        this.openKey = openKey;
    }

    private static long cordKey(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static boolean inRect(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private static void setSlotX(Slot slot, int x) {
        try {
            java.lang.reflect.Field f = Slot.class.getDeclaredField("x");
            f.setAccessible(true);
            f.setInt(slot, x);
        } catch (Exception ignored) {}
    }

    private static void setSlotY(Slot slot, int y) {
        try {
            java.lang.reflect.Field f = Slot.class.getDeclaredField("y");
            f.setAccessible(true);
            f.setInt(slot, y);
        } catch (Exception ignored) {}
    }

    private static void clickContainerSlot(Minecraft mc, int containerId, int slotIndex, int button) {
        if (mc.gameMode == null || mc.player == null) return;
        try {
            Class<?> containerInputClass = Class.forName("net.minecraft.world.inventory.ContainerInput");
            Object pickupAction = containerInputClass.getEnumConstants()[0];
            var handleInputMethod = mc.gameMode.getClass().getMethod("handleContainerInput",
                    int.class, int.class, int.class, containerInputClass,
                    net.minecraft.world.entity.player.Player.class);
            handleInputMethod.invoke(mc.gameMode, containerId, slotIndex, button, pickupAction, mc.player);
        } catch (Exception e) {
            StorageInitializer.LOGGER.error("Failed to click slot {}", slotIndex, e);
        }
    }

    @Override
    protected void init() {
        SackCache.getInstance().pruneInvalidPages();
        super.init();

        layout.build(this, this.font, this.width, this.height, state, openKey, 6,
                this::onPageCardClicked, this::onSearchChanged);

        if (!autoScrolledToOpenCard) {
            scrollLiveCardIntoView();
            autoScrolledToOpenCard = true;
        }

        var inventory = layout.getInventoryPanel();
        int storageLeft = layout.getMainBackgroundX();
        int boxTop = layout.getMainBackgroundY();
        int storageRight = storageLeft + layout.getMainBackgroundWidth();

        int invLeft = inventory.getTotalX();
        int invRight = inventory.getTotalX() + inventory.getWidth();
        int invBottom = inventory.getTotalY() + inventory.getHeight();

        int boxLeft = Math.min(storageLeft, invLeft);
        int boxRight = Math.max(storageRight, invRight);

        var accessor = (AbstractContainerScreenAccessor) this;
        accessor.helpbox$setImageWidth(boxRight - boxLeft);
        accessor.helpbox$setImageHeight(invBottom - boxTop);
        this.leftPos = boxLeft;
        this.topPos = boxTop;

        syncSlotPositions();
        state.onStorageScreenOpened();

        // Consume pending Sack key and store it locally for containerTick to handle
        // (slots are still empty at init time — items arrive from the server a tick later)
        SackKey pending = state.consumePendingSackKey();
        if (pending != null && openKey.type() == SackKey.Type.SACK_INDEX) {
            this.pendingAutoOpen = pending;
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return new ArrayList<>(super.children());
    }

    @Override
    public void removed() {
        super.removed();
        state.onStorageScreenClosed();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return !isInsideOverlay(mx, my);
    }

    private boolean isInsideOverlay(double mx, double my) {
        ScrollContainerWidget pageOverview = layout.getPageOverview();
        if (pageOverview != null && inRect(mx, my,
                pageOverview.getX(), pageOverview.getY(),
                pageOverview.getWidth(), pageOverview.getHeight())) {
            return true;
        }

        var inventory = layout.getInventoryPanel();
        if (inventory != null && inRect(mx, my,
                inventory.getTotalX(), inventory.getTotalY(),
                inventory.getWidth(), inventory.getHeight())) {
            return true;
        }
        return false;
    }

    private void syncSlotPositions() {
        topSlotClipRects.clear();
        if (openKey.type() == SackKey.Type.SACK_INDEX) {
            syncIndexSlotPositions();
        } else {
            syncPageSlotPositions();
        }
        syncPlayerSlotPositions();
    }

    private void syncIndexSlotPositions() {
        int containerSlots = this.menu.getRowCount() * 9;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            setSlotX(slot, -9999);
            setSlotY(slot, -9999);
        }
    }

    private void syncPageSlotPositions() {
        SackCardComponent openCard = layout.getOpenCard();
        if (openCard == null) return;

        ScrollContainerWidget viewport = layout.getPageOverview();
        if (viewport == null) return;

        final int offX = this.leftPos;
        final int offY = this.topPos;

        int rows = this.menu.getRowCount();
        int containerSlots = rows * 9;

        int cardX = openCard.getTotalX();
        int cardY = viewport.getY() - (int) viewport.scrollAmount() + layout.getOpenCardAccumulatedY();

        int originalX = cardX + SackOverlayLayout.CARD_BORDER + 1;
        int originalY = cardY + font.lineHeight + 5;

        // Move all container slots out of view initially
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            setSlotX(slot, -9999);
            setSlotY(slot, -9999);
        }

        int navCount = 0;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            if (stack.getItem().toString().contains("stained_glass_pane")) {
                continue;
            }

            int targetCol;
            int targetRow;

            if (isNavigationItem(stack)) {
                targetCol = getNavTargetCol(stack, navCount++);
                targetRow = 5;
            } else {
                if (i >= 45) continue;
                targetCol = i % 9;
                targetRow = i / 9;
            }

            int absX = originalX + targetCol * 18;
            int absY = originalY + targetRow * 18;

            int clipLeft = Math.max(absX, viewport.getX());
            int clipTop = Math.max(absY, viewport.getY());
            int clipRight = Math.min(absX + 16, viewport.getX() + viewport.getWidth());
            int clipBottom = Math.min(absY + 16, viewport.getY() + viewport.getHeight());

            int relX = absX - offX;
            int relY = absY - offY;
            setSlotX(slot, relX);
            setSlotY(slot, relY);

            topSlotClipRects.put(cordKey(relX, relY), new int[]{clipLeft, clipTop, clipRight, clipBottom});
        }
    }

    public static boolean isNavigationItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(Items.ARROW) || stack.is(Items.BARRIER) || stack.is(Items.CAULDRON) || stack.is(Items.CHEST)) {
            return true;
        }
        String name = TextUtils.stripText(stack.getHoverName()).trim().toLowerCase();
        return name.contains("go back") || name.contains("close") || name.contains("sack storage")
                || name.contains("previous") || name.contains("next") || name.contains("search") || name.contains("sort");
    }

    private static int getNavTargetCol(ItemStack stack, int navIndex) {
        if (stack.is(Items.ARROW)) return 3;
        if (stack.is(Items.BARRIER)) return 4;
        if (stack.is(Items.CAULDRON)) return 5;
        if (stack.is(Items.CHEST)) return 6;
        String name = TextUtils.stripText(stack.getHoverName()).trim().toLowerCase();
        if (name.contains("go back") || name.contains("previous")) return 3;
        if (name.contains("close")) return 4;
        if (name.contains("sort") || name.contains("search")) return 5;
        return Math.min(8, 3 + navIndex);
    }

    private void syncPlayerSlotPositions() {
        var inventory = layout.getInventoryPanel();
        if (inventory == null) return;

        final int offX = this.leftPos;
        final int offY = this.topPos;

        int invX = inventory.getTotalX() + 8;
        int invMainY = inventory.getTotalY() + 14 + 1;
        int hotbarY = invMainY + 3 * 18 + 4;

        int base = this.menu.slots.size() - 36;
        for (int i = 0; i < 27; i++) {
            Slot slot = this.menu.slots.get(base + i);
            setSlotX(slot, (invX + (i % 9) * 18) - offX);
            setSlotY(slot, (invMainY + (i / 9) * 18) - offY);
        }
        for (int i = 0; i < 9; i++) {
            Slot s = this.menu.slots.get(base + 27 + i);
            setSlotX(s, (invX + i * 18) - offX);
            setSlotY(s, hotbarY - offY);
        }
    }

    private void onSearchChanged() {
        searchRebuildDueAt = Util.getMillis() + SEARCH_REBUILD_DELAY_MS;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncSlotPositions();

        // Handle pending auto-open: wait until server sends container items, then auto-click the target sack
        if (pendingAutoOpen != null && ContainerContentTracker.hasReceived(this.menu.containerId)) {
            SackKey target = pendingAutoOpen;
            pendingAutoOpen = null;

            int containerSlots = this.menu.getRowCount() * 9;
            for (int i = 0; i < containerSlots; i++) {
                Slot slot = this.menu.slots.get(i);
                if (matchesSackKey(slot.getItem(), target)) {
                    state.beginNavigation();
                    clickContainerSlot(Minecraft.getInstance(), this.menu.containerId, slot.index, 1);
                    return;
                }
            }
            StorageInitializer.LOGGER.warn("Pending sack {} not found in index slots", target.displayName());
        }

        if (searchRebuildDueAt >= 0 && Util.getMillis() >= searchRebuildDueAt) {
            searchRebuildDueAt = -1;
            this.rebuildWidgets();

            var box = layout.getSearchBox();
            if (box != null) {
                this.setFocused(box);
                box.setFocused(true);
            }
        }

        if (!SackCache.getInstance().allKnown().isEmpty() && layout.getSackCards().isEmpty() && ContainerContentTracker.hasReceived(this.menu.containerId)) {
            this.rebuildWidgets();
        }
    }

    private boolean matchesSackKey(ItemStack stack, SackKey key) {
        if (stack.isEmpty()) return false;
        String name = TextUtils.stripText(stack.getHoverName()).trim().toLowerCase();

        if (name.contains("go back") || name.contains("close") || name.contains("next page") || name.contains("previous page")) {
            return false;
        }

        var parsedOpt = SackKey.fromIndexItem(stack);
        if (parsedOpt.isPresent()) {
            SackKey parsed = parsedOpt.get();
            if (parsed.equals(key)) {
                return true;
            }
        }

        String keyName = key.displayName().toLowerCase();
        if (name.contains(keyName)) return true;
        for (String alias : key.type().aliases) {
            if (name.contains(alias)) return true;
        }
        return false;
    }

    private void onPageCardClicked(SackKey key) {
        if (key.equals(openKey)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int containerSlots = this.menu.getRowCount() * 9;

        // Currently in the SACK_INDEX: directly right-click the target sack item
        if (openKey.type() == SackKey.Type.SACK_INDEX) {
            for (int i = 0; i < containerSlots; i++) {
                Slot slot = this.menu.slots.get(i);
                if (matchesSackKey(slot.getItem(), key)) {
                    state.beginNavigation();
                    clickContainerSlot(mc, this.menu.containerId, slot.index, 1);
                    return;
                }
            }
            StorageInitializer.LOGGER.info("Sack {} not in current container slots", key.displayName());
            return;
        }

        // Currently inside a specific sack. Store the target for auto-open after /sacks reopens the index.
        if (key.type() != SackKey.Type.SACK_INDEX) {
            state.setPendingSackKey(key);
        }

        // Use /sacks command to safely return to index without picking up items onto the cursor
        state.beginNavigation();
        mc.player.connection.sendCommand("sacks");
    }

    public void scrollLiveCardIntoView() {
        ScrollContainerWidget overview = layout.getPageOverview();
        SackCardComponent liveCard = layout.getOpenCard();
        if (overview == null || liveCard == null) return;

        if (SackConfig.autoScrollToOpenSack == EnhancedStorageConfig.AutoScrollMode.OFF) {
            return;
        }

        int cardY = layout.getOpenCardAccumulatedY();
        int cardH = liveCard.getHeight();
        int viewH = overview.getHeight();
        double currentScroll = overview.scrollAmount();

        boolean partlyHidden = cardY < currentScroll || (cardY + cardH) > (currentScroll + viewH);
        boolean fullyHidden = (cardY + cardH) <= currentScroll || cardY >= (currentScroll + viewH);

        if (SackConfig.autoScrollToOpenSack == EnhancedStorageConfig.AutoScrollMode.IF_FULLY_HIDDEN && !fullyHidden) {
            return;
        }

        if (partlyHidden) {
            double targetScroll = Math.max(0, cardY - (viewH - cardH) / 2.0);
            overview.setScrollAmount(targetScroll);
        }
    }

    private void onSettingsClicked() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreenAndShow(new ModernSackSettingsScreen(this));
    }

    private void onFetchSacksClicked() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.closeContainer();
            new Thread(() -> {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {}
                mc.execute(() -> {
                    if (mc.player != null && mc.player.connection != null) {
                        mc.player.connection.sendCommand("sacks");
                    }
                });
            }, "HelpBox-SacksFetch").start();
        }
    }

    private void onInsertInventoryClicked() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int containerSlots = this.menu.getRowCount() * 9;

        // 1. Priority 1: Sandık (CHEST) or name strictly matching "insert"
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            String name = TextUtils.stripText(stack.getHoverName()).trim().toLowerCase();
            if (stack.is(net.minecraft.world.item.Items.CHEST) || name.contains("insert inventory") || name.contains("insert")) {
                StorageInitializer.LOGGER.info("Insert Inventory clicked CHEST slot {} ({})", slot.index, name);
                clickContainerSlot(mc, this.menu.containerId, slot.index, 0);
                return;
            }
        }

        // 2. Priority 2: Bottom row CHEST item fallback
        int startRow = Math.max(0, containerSlots - 9);
        for (int i = startRow; i < containerSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (stack.is(net.minecraft.world.item.Items.CHEST)) {
                StorageInitializer.LOGGER.info("Insert Inventory clicked bottom row CHEST slot {} ({})", slot.index, stack.getItem());
                clickContainerSlot(mc, this.menu.containerId, slot.index, 0);
                return;
            }
        }

        // 3. Priority 3: Fallback for Cauldron/fill if no Chest exists
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            String name = TextUtils.stripText(stack.getHoverName()).trim().toLowerCase();
            if (name.contains("fill") || stack.is(net.minecraft.world.item.Items.CAULDRON)) {
                StorageInitializer.LOGGER.info("Insert Inventory fallback clicked slot {} ({})", slot.index, name);
                clickContainerSlot(mc, this.menu.containerId, slot.index, 0);
                return;
            }
        }

        StorageInitializer.LOGGER.warn("Insert Inventory slot not found in container slots (total {})", containerSlots);
    }

    private boolean isOverPageOverview(double mouseX, double mouseY) {
        ScrollContainerWidget overview = layout.getPageOverview();
        if (overview == null) return false;
        return mouseX >= overview.getX() && mouseX < overview.getX() + overview.getWidth()
                && mouseY >= overview.getY() && mouseY < overview.getY() + overview.getHeight();
    }

    private SackCardComponent cardAt(double mx, double my) {
        if (!isOverPageOverview(mx, my)) return null;
        for (SackCardComponent card : layout.getSackCards()) {
            if (inRect(mx, my, card.getTotalX(), card.getTotalY(), card.getWidth(), card.getHeight())) {
                return card;
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ScrollContainerWidget overview = layout.getPageOverview();
        if (overview != null
                && mouseX >= overview.getX() && mouseX < overview.getX() + overview.getWidth()
                && mouseY >= overview.getY() && mouseY < overview.getY() + overview.getHeight()
                && overview.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        syncSlotPositions();
        super.extractContents(guiGraphics, mouseX, mouseY, a);

        int[] fb = layout.getFetchButtonBounds();
        if (fb != null) {
            boolean hovered = inRect(mouseX, mouseY, fb[0], fb[1], fb[2], fb[3]);
            ModernUiRenderHelper.drawPillButton(guiGraphics, font, fb[0], fb[1], fb[2], fb[3], HelpBoxLang.get("helpbox.ui.sacks.btn_fetch", "🔄 Sacks Verilerini Getir"), 0xFF38BDF8, hovered, false);
        }

        // Real-time Drag Preview & Target Highlight
        if (isDraggingCard && draggedCardKey != null) {
            SackCardComponent targetCard = cardAt(mouseX, mouseY);

            // Highlight drop target card border
            if (targetCard != null && !targetCard.getKey().equals(draggedCardKey) && targetCard.getKey().type() != SackKey.Type.SACK_INDEX) {
                int tx = targetCard.getTotalX();
                int ty = targetCard.getTotalY();
                int tw = targetCard.getWidth();
                int th = targetCard.getHeight();
                guiGraphics.fill(tx - 2, ty - 2, tx + tw + 2, ty, 0xFF00FFCC);
                guiGraphics.fill(tx - 2, ty + th, tx + tw + 2, ty + th + 2, 0xFF00FFCC);
                guiGraphics.fill(tx - 2, ty, tx, ty + th, 0xFF00FFCC);
                guiGraphics.fill(tx + tw, ty, tx + tw + 2, ty + th, 0xFF00FFCC);
            }

            // Floating native drag preview tooltip following the cursor
            String dragName = draggedCardKey.displayName();
            Component targetComp = (targetCard != null && !targetCard.getKey().equals(draggedCardKey) && targetCard.getKey().type() != SackKey.Type.SACK_INDEX)
                    ? Component.translatable("enhanced_storage.ui.drag_swap_with", targetCard.getKey().displayName())
                    : Component.translatable("enhanced_storage.ui.drag_swap_hint");

            List<Component> dragTooltip = List.of(
                    Component.translatable("enhanced_storage.ui.dragging", dragName).withStyle(s -> s.withColor(0x55FF55).withBold(true)),
                    targetComp.copy().withStyle(s -> s.withColor(0x00FFCC).withItalic(false))
            );

            guiGraphics.setTooltipForNextFrame(
                    font,
                    dragTooltip,
                    Optional.empty(),
                    mouseX, mouseY
            );
        }
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (slot.y < -9000) return;
        int containerSlots = this.menu.getRowCount() * 9;
        if (slot.index < containerSlots) {
            ScrollContainerWidget viewport = layout.getPageOverview();
            if (viewport != null) {
                graphics.enableScissor(
                        viewport.getX() - this.leftPos, viewport.getY() - this.topPos,
                        viewport.getX() + viewport.getWidth() - this.leftPos,
                        viewport.getY() + viewport.getHeight() - this.topPos);
                super.extractSlot(graphics, slot, mouseX, mouseY);
                graphics.disableScissor();
                return;
            }
        }
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    @Override
    protected boolean isHovering(int left, int top, int w, int h, double xm, double ym) {
        int[] clip = topSlotClipRects.get(cordKey(left, top));
        if (clip != null) {
            if (clip[2] <= clip[0] || clip[3] <= clip[1]) return false;
            return xm >= clip[0] && xm < clip[2] && ym >= clip[1] && ym < clip[3];
        }
        return super.isHovering(left, top, w, h, xm, ym);
    }

    @Override
    public int[] helpbox$getHighlightClip(Slot slot) {
        return topSlotClipRects.get(cordKey(slot.x, slot.y));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int[] fb = layout.getFetchButtonBounds();
        if (fb != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && inRect(event.x(), event.y(), fb[0], fb[1], fb[2], fb[3])) {
            onFetchSacksClicked();
            return true;
        }

        int[] sb = layout.getSettingsButtonBounds();
        if (sb != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && inRect(event.x(), event.y(), sb[0], sb[1], sb[2], sb[3])) {
            onSettingsClicked();
            return true;
        }



        int[] iib = layout.getInsertInventoryButtonBounds();
        if (iib != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && inRect(event.x(), event.y(), iib[0], iib[1], iib[2], iib[3])) {
            onInsertInventoryClicked();
            return true;
        }

        // Close / Delete Button (✕) click detection
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isOverPageOverview(event.x(), event.y())) {
            SackCardComponent card = cardAt(event.x(), event.y());
            if (card != null && card.getKey().type() != SackKey.Type.SACK_INDEX) {
                int relX = (int) (event.x() - card.getTotalX());
                int relY = (int) (event.y() - card.getTotalY());

                // Click on ✕ button at top-right (relX between width - 34 and width - 18, relY <= 16)
                if (relX >= card.getWidth() - 34 && relX <= card.getWidth() - 18 && relY <= 16) {
                    SackKey toRemove = card.getKey();
                    SackCache.getInstance().removeKnown(toRemove);
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.translatable("message.helpbox.sack_removed", toRemove.displayName()));
                    }
                    this.rebuildWidgets();
                    return true;
                }
            }
        }

        // Dedicated Drag Handle (⋮⋮) click detection
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isOverPageOverview(event.x(), event.y())) {
            SackCardComponent card = cardAt(event.x(), event.y());
            if (card != null && card.getKey().type() != SackKey.Type.SACK_INDEX) {
                int relX = (int) (event.x() - card.getTotalX());
                int relY = (int) (event.y() - card.getTotalY());

                // Initiate drag ONLY if clicking the top right ⋮⋮ drag handle button
                if (relX >= card.getWidth() - 18 && relY <= 16) {
                    draggedCardKey = card.getKey();
                    dragStartX = event.x();
                    dragStartY = event.y();
                    isDraggingCard = true;
                    return true;
                }
            }
        }

        // Direct handling for live sack container slots:
        // Left Click -> Withdraw max (fill inventory)
        // Right Click -> Withdraw 1 stack (64 items)
        Slot liveSlot = getHoveredLiveSlot(event.x(), event.y());
        if (liveSlot != null) {
            ItemStack stack = liveSlot.getItem();
            if (!stack.isEmpty() && !isNavigationItem(stack)) {
                Minecraft mc = Minecraft.getInstance();
                int button = (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) ? 1 : 0;
                clickContainerSlot(mc, this.menu.containerId, liveSlot.index, button);
                if (mc.player != null) {
                    mc.player.containerMenu.setCarried(ItemStack.EMPTY);
                }
                return true;
            }
        }

        // 1. Dispatch to UI widgets
        for (GuiEventListener child : this.children()) {
            if (child.mouseClicked(event, doubleClick)) {
                this.setFocused(child);
                if (!child.isFocused()) child.setFocused(true);
                if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) this.setDragging(true);
                return true;
            }
        }

        // 2. Handle card clicks in page overview
        if (isOverPageOverview(event.x(), event.y())) {
            SackCardComponent card = cardAt(event.x(), event.y());
            if (card != null) {
                if (!card.getKey().equals(openKey)) {
                    onPageCardClicked(card.getKey());
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private Slot getHoveredLiveSlot(double mouseX, double mouseY) {
        if (openKey.type() == SackKey.Type.SACK_INDEX) return null;
        int rows = this.menu.getRowCount();
        int containerSlots = rows * 9;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            if (slot.y < -9000) continue;
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> list = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (!stack.isEmpty() && !isNavigationItem(stack) && openKey.type() != SackKey.Type.SACK_INDEX) {
            Minecraft mc = Minecraft.getInstance();
            double mx = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
            double my = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
            Slot hovered = getHoveredLiveSlot(mx, my);
            if (hovered != null && hovered.getItem() == stack) {
                list.add(Component.empty());
                list.add(Component.translatable("tooltip.helpbox.sack_take_all"));
                list.add(Component.translatable("tooltip.helpbox.sack_take_stack"));
            }
        }
        return list;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggedCardKey != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            isDraggingCard = true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggedCardKey != null) {
            SackKey sourceKey = draggedCardKey;
            boolean wasDragging = isDraggingCard;
            draggedCardKey = null;
            isDraggingCard = false;

            if (wasDragging && isOverPageOverview(event.x(), event.y())) {
                SackCardComponent targetCard = cardAt(event.x(), event.y());
                if (targetCard != null && !targetCard.getKey().equals(sourceKey) && targetCard.getKey().type() != SackKey.Type.SACK_INDEX) {
                    List<SackKey> displayKeys = layout.getSackCards().stream()
                            .map(SackCardComponent::getKey)
                            .filter(k -> k.type() != SackKey.Type.SACK_INDEX)
                            .toList();
                    SackCache.getInstance().swapSacks(sourceKey, targetCard.getKey(), displayKeys);
                    this.rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        var box = layout.getSearchBox();
        if (box != null && box.isFocused() && !event.isEscape()) {
            if (box.keyPressed(event)) return true;
            return true;
        }
        return super.keyPressed(event);
    }
}
