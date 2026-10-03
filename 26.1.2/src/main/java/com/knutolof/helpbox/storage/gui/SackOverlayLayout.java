package com.knutolof.helpbox.storage.gui;
import com.knutolof.helpbox.storage.StorageInitializer;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.api.screen.IScreen;
import com.daqem.uilib.gui.component.EmptyComponent;
import com.daqem.uilib.gui.component.sprite.SpriteComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.component.text.TruncatedTextComponent;
import com.daqem.uilib.gui.widget.EditBoxWidget;
import com.daqem.uilib.gui.widget.ScrollContainerWidget;
import com.knutolof.helpbox.storage.config.EnhancedStorageConfig;
import com.knutolof.helpbox.storage.config.SackConfig;
import com.knutolof.helpbox.storage.gui.component.IconButtonComponent;
import com.knutolof.helpbox.storage.gui.component.ItemButtonComponent;
import com.knutolof.helpbox.storage.gui.component.SackCardComponent;
import com.knutolof.helpbox.storage.gui.component.TooltipItemComponent;
import com.knutolof.helpbox.storage.storage.SackCache;
import com.knutolof.helpbox.storage.storage.SackKey;
import com.knutolof.helpbox.storage.util.ItemSearch;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;

import static com.knutolof.helpbox.storage.StorageInitializer.MOD_ID;

public class SackOverlayLayout {

    public static final int SLOTS_ACROSS = 9;
    public static final int SLOT_SIZE = 18;
    public static final int CARD_BORDER = 3;
    public static final int INNER_PADDING = 6;
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int SCROLLBAR_GAP = 4;
    public static final int UNCACHED_CARD_HEIGHT = 50;

    /** Bottom-left "Sack of Sacks" panel geometry (panel-local). */
    public static final int INDEX_PANEL_WIDTH = 176;
    public static final int INDEX_PANEL_HEIGHT = 96;
    public static final int INDEX_GRID_X = 7;
    public static final int INDEX_GRID_Y = 15;
    public static final int INDEX_COLS = 9;
    public static final int INDEX_ROWS = 4;

    private final List<SackCardComponent> sackCards = new ArrayList<>();
    private int mainBackgroundX;
    private int mainBackgroundY;
    private int mainBackgroundWidth;
    private int mainBackgroundHeight;
    private SackCardComponent openCard;
    private int openCardAccumulatedY = 0;
    private SpriteComponent inventoryPanel;
    private @Nullable SpriteComponent indexPanel;
    private ScrollContainerWidget pageOverview;
    private EditBoxWidget searchBox;
    private IconButtonComponent settingsButton;
    private int @Nullable [] settingsButtonBounds;
    private ItemButtonComponent insertInventoryButton;
    private int @Nullable [] insertInventoryButtonBounds;
    private int @Nullable [] fetchButtonBounds;

    public @Nullable SpriteComponent getIndexPanel() {
        return indexPanel;
    }

    public int @Nullable [] getFetchButtonBounds() {
        return fetchButtonBounds;
    }

    public int @Nullable [] getInsertInventoryButtonBounds() {
        return insertInventoryButtonBounds;
    }

    public static void cycleTheme() {
        SackConfig.sackBackgroundType = switch (SackConfig.sackBackgroundType) {
            case TRANSPARENT -> EnhancedStorageConfig.BackgroundType.DARK;
            case DARK -> EnhancedStorageConfig.BackgroundType.LIGHT;
            case LIGHT -> EnhancedStorageConfig.BackgroundType.TRANSPARENT;
        };
        SackConfig.write(com.knutolof.helpbox.storage.StorageInitializer.SACK_MOD_ID);
    }

    public static int computeColumns(int width) {
        int cardWidth = CARD_BORDER * 2 + SLOTS_ACROSS * SLOT_SIZE;
        int maxAvailableWidth = width - SackConfig.sackHorizontalMargin
                - INNER_PADDING * 2 - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
        return Math.clamp((maxAvailableWidth + SackConfig.sackCardSpacing) / (cardWidth + SackConfig.sackCardSpacing),
                1, SackConfig.sackMaxPagePerRow);
    }

    public static int computeMainBackgroundWidth(int width) {
        int cardWidth = CARD_BORDER * 2 + SLOTS_ACROSS * SLOT_SIZE;
        int columns = computeColumns(width);
        int rowContentWidth = columns * cardWidth + (columns - 1) * SackConfig.sackCardSpacing;
        return rowContentWidth + INNER_PADDING * 2 + SCROLLBAR_WIDTH + SCROLLBAR_GAP;
    }

    public static int computeMainBackgroundHeight(int height) {
        return height - SackConfig.sackOverviewTopMargin - (SackConfig.sackOverviewBottomMargin + 96);
    }

    private static int getTitleTextColor() {
        return switch (SackConfig.sackBackgroundType) {
            case LIGHT -> 0xFF000000;
            default -> 0xFFAAAAAA;
        };
    }

    private static boolean shouldDrawTitleShadow() {
        return SackConfig.sackBackgroundType != EnhancedStorageConfig.BackgroundType.LIGHT;
    }

    private Identifier getMainBackgroundTexture() {
        return switch (SackConfig.sackBackgroundType) {
            case TRANSPARENT -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "transparent/main_panel_trans");
            case DARK -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "dark/main_panel_dark");
            default -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "light/main_panel");
        };
    }

    private Identifier getInventoryTexture() {
        return switch (SackConfig.sackBackgroundType) {
            case TRANSPARENT -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "transparent/storage_inventory_trans");
            case DARK -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "dark/storage_inventory_dark");
            default -> Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "light/storage_inventory");
        };
    }

    public int getMainBackgroundX() {
        return mainBackgroundX;
    }

    public int getMainBackgroundY() {
        return mainBackgroundY;
    }

    public int getMainBackgroundWidth() {
        return mainBackgroundWidth;
    }

    public int getMainBackgroundHeight() {
        return mainBackgroundHeight;
    }

    public SackCardComponent getOpenCard() {
        return openCard;
    }

    public int getOpenCardAccumulatedY() {
        return openCardAccumulatedY;
    }

    public List<SackCardComponent> getSackCards() {
        return sackCards;
    }

    public SpriteComponent getInventoryPanel() {
        return inventoryPanel;
    }

    public ScrollContainerWidget getPageOverview() {
        return pageOverview;
    }

    public EditBoxWidget getSearchBox() {
        return searchBox;
    }

    public int @Nullable [] getSettingsButtonBounds() {
        return settingsButtonBounds;
    }

    public void build(IScreen screen, Font font, int width, int height, SackOverlayState state,
                      @Nullable SackKey liveKey, int liveRows, Consumer<SackKey> onCardClick,
                      Runnable onSearchChanged) {

        this.sackCards.clear();
        this.openCard = null;
        this.openCardAccumulatedY = 0;
        this.settingsButton = null;
        this.settingsButtonBounds = null;
        this.insertInventoryButton = null;
        this.insertInventoryButtonBounds = null;
        this.fetchButtonBounds = null;

        int titleAreaHeight = font.lineHeight + 2;

        Set<SackKey> known = SackCache.getInstance().allKnown();
        Set<SackKey> keysToDisplay = new LinkedHashSet<>();

        if (known.isEmpty()) {
            if (liveKey != null) keysToDisplay.add(SackKey.canonical(liveKey));
            for (SackKey k : SackCache.getInstance().all().keySet()) keysToDisplay.add(SackKey.canonical(k));
        } else {
            for (SackKey k : known) keysToDisplay.add(SackKey.canonical(k));
            if (liveKey != null) keysToDisplay.add(SackKey.canonical(liveKey));
        }

        SackKey canonicalLiveKey = (liveKey != null && liveKey.type() != SackKey.Type.SACK_INDEX) ? SackKey.canonical(liveKey) : null;

        List<SackKey> pageKeys = new ArrayList<>(keysToDisplay.stream()
                .filter(k -> k.type() != SackKey.Type.SACK_INDEX)
                .toList());

        List<String> customOrder = SackCache.getInstance().getCustomOrder();
        if (!customOrder.isEmpty()) {
            pageKeys.sort((a, b) -> {
                int idxA = customOrder.indexOf(a.id());
                int idxB = customOrder.indexOf(b.id());
                if (idxA == -1) idxA = 9999;
                if (idxB == -1) idxB = 9999;
                if (idxA != idxB) return Integer.compare(idxA, idxB);
                return SackKey.DISPLAY_ORDER.compare(a, b);
            });
        } else {
            pageKeys.sort(SackKey.DISPLAY_ORDER);
        }

        if (SackConfig.showSackGoBackCard && liveKey != null && liveKey.type() != SackKey.Type.SACK_INDEX) {
            pageKeys.add(new SackKey(SackKey.Type.SACK_INDEX));
        }

        String query = state.getSearchQuery();
        if (!query.isBlank()) {
            pageKeys = pageKeys.stream()
                    .filter(key -> key.equals(liveKey)
                            || key.type() == SackKey.Type.SACK_INDEX
                            || SackCache.getInstance().get(key)
                            .map(page -> ItemSearch.anyMatch(page.items(), query))
                            .orElse(false))
                    .toList();
        }

        int cardWidth = CARD_BORDER * 2 + SLOTS_ACROSS * SLOT_SIZE;
        int columns = computeColumns(width);
        int rowContentWidth = columns * cardWidth + (columns - 1) * SackConfig.sackCardSpacing;

        int mainBackgroundWidth = computeMainBackgroundWidth(width);
        int mainBackgroundHeight = computeMainBackgroundHeight(height);
        int mainBackgroundX = (width / 2) - (mainBackgroundWidth / 2);
        int mainBackgroundY = SackConfig.sackOverviewTopMargin;

        this.mainBackgroundX = mainBackgroundX;
        this.mainBackgroundY = mainBackgroundY;
        this.mainBackgroundWidth = mainBackgroundWidth;
        this.mainBackgroundHeight = mainBackgroundHeight;

        if (SackConfig.showSackOverviewCard) {
            SpriteComponent mainBackground = new SpriteComponent(mainBackgroundX, mainBackgroundY, mainBackgroundWidth, mainBackgroundHeight, getMainBackgroundTexture());
            screen.addComponent(mainBackground);

            ScrollContainerWidget pageOverview = new ScrollContainerWidget(mainBackgroundWidth - INNER_PADDING * 2, mainBackgroundHeight - INNER_PADDING * 2, SackConfig.sackCardSpacing) {
                @Override
                public boolean isMouseOver(double x, double y) {
                    return this.visible && this.isOverScrollbar(x, y);
                }

                @Override
                public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
                    if (updateScrolling(event)) return true;
                    if (event.x() < getX() || event.x() >= getX() + getWidth()
                            || event.y() < getY() || event.y() >= getY() + getHeight()) {
                        return false;
                    }
                    for (GuiEventListener child : children()) {
                        if (child.mouseClicked(event, doubleClick)) {
                            setFocused(child);
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void setScrollAmount(double amount) {
                    super.setScrollAmount(amount);
                    state.setScrollAmount(scrollAmount());
                }

                @Override
                protected double scrollRate() {
                    return SackConfig.sackOverlayScrollSpeed;
                }

            @Override
            protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.enableScissor(this.getX(), this.getY(),
                        this.getX() + this.getWidth(), this.getY() + this.getHeight());

                List<IComponent> rows = getComponents();
                int currentY = -(int) this.scrollAmount();
                for (int i = 0; i < rows.size(); i++) {
                    IComponent row = rows.get(i);
                    row.setY(currentY);
                    if (currentY + row.getHeight() > 0 && currentY < this.getHeight()) {
                        row.extractRenderStateBase(guiGraphics, mouseX, mouseY, partialTick, this.getWidth(), this.getHeight());
                    }
                    currentY += row.getHeight();
                    if (i < rows.size() - 1) {
                        currentY += getContentSpacing();
                    }
                }

                guiGraphics.disableScissor();
                this.extractScrollbar(guiGraphics, mouseX, mouseY);
            }
        };

        if (pageKeys.isEmpty()) {
            if (query.isBlank()) {
                Component titleMsg = Component.translatable("helpbox.ui.sacks.first_time_title");
                Component descMsg = Component.translatable("helpbox.ui.sacks.first_time_desc");

                int rowHeight = 80;
                int titleW = Math.min(font.width(titleMsg), rowContentWidth);
                int titleX = (rowContentWidth - titleW) / 2;
                int titleY = 16;

                TruncatedTextComponent emptyTitle = new TruncatedTextComponent(
                        titleX, titleY, rowContentWidth - titleX, titleMsg, 0xFF38BDF8);
                emptyTitle.setDrawShadow(true);

                int descW = Math.min(font.width(descMsg), rowContentWidth);
                int descX = (rowContentWidth - descW) / 2;
                int descY = titleY + font.lineHeight + 6;

                TruncatedTextComponent emptyDesc = new TruncatedTextComponent(
                        descX, descY, rowContentWidth - descX, descMsg, 0xFFCBD5E1);
                emptyDesc.setDrawShadow(false);

                EmptyComponent emptyRow = new EmptyComponent(0, 0, rowContentWidth, rowHeight);
                emptyRow.addComponent(emptyTitle);
                emptyRow.addComponent(emptyDesc);
                pageOverview.addComponent(emptyRow);

                int btnW = 170;
                int btnH = 22;
                int btnX = mainBackgroundX + (mainBackgroundWidth - btnW) / 2;
                int btnY = mainBackgroundY + descY + font.lineHeight + 14;
                this.fetchButtonBounds = new int[]{btnX, btnY, btnW, btnH};
            } else {
                Component message = Component.translatable("enhanced_storage.ui.no_sacks_contain", query);
                int rowHeight = titleAreaHeight + 8;
                int textWidth = Math.min(font.width(message), rowContentWidth);
                int textX = (rowContentWidth - textWidth) / 2;
                int textY = (rowHeight - font.lineHeight) / 2 + 20;

                TruncatedTextComponent emptyText = new TruncatedTextComponent(
                        textX, textY, rowContentWidth - textX, message, getTitleTextColor());
                emptyText.setDrawShadow(shouldDrawTitleShadow());

                EmptyComponent emptyRow = new EmptyComponent(0, 0, rowContentWidth, rowHeight);
                emptyRow.addComponent(emptyText);
                pageOverview.addComponent(emptyRow);
            }
        }

        int uniformCardHeight = CARD_BORDER * 2 + titleAreaHeight + 6 * SLOT_SIZE;

        int contentY = 0;
        for (int i = 0; i < pageKeys.size(); i += columns) {
            List<SackKey> rowKeys = pageKeys.subList(i, Math.min(i + columns, pageKeys.size()));

            int rowHeight = uniformCardHeight;

            if (canonicalLiveKey != null && rowKeys.contains(canonicalLiveKey)) {
                this.openCardAccumulatedY = contentY;
            }

            EmptyComponent row = new EmptyComponent(0, 0, rowContentWidth, rowHeight);
            for (int col = 0; col < rowKeys.size(); col++) {
                SackKey key = rowKeys.get(col);
                boolean isLive = key.equals(canonicalLiveKey);
                boolean isSackIndex = key.type() == SackKey.Type.SACK_INDEX;

                Optional<SackCache.CachedSackPage> pageOpt = isSackIndex
                        ? Optional.empty()
                        : SackCache.getInstance().get(key);
                boolean cached = pageOpt.isPresent();
                List<ItemStack> items = isLive ? List.of() : pageOpt.map(SackCache.CachedSackPage::items).orElse(List.of());
                Map<Integer, SackCache.SackItemMeta> metaMap = pageOpt.map(SackCache.CachedSackPage::meta).orElse(Map.of());

                int cardHeight = uniformCardHeight;

                SackCardComponent pageCard = new SackCardComponent(
                        col * (cardWidth + SackConfig.sackCardSpacing), 0,
                        cardWidth, cardHeight,
                        key, state,
                        items, metaMap,
                        isLive || cached, isLive,
                        CARD_BORDER, titleAreaHeight, SLOTS_ACROSS, SLOT_SIZE,
                        onCardClick
                );
                if (isLive) this.openCard = pageCard;
                this.sackCards.add(pageCard);
                row.addComponent(pageCard);
            }
            pageOverview.addComponent(row);
            contentY += rowHeight + pageOverview.getContentSpacing();
        }

        pageOverview.uilib$updateParentPosition(mainBackground.getTotalX() + INNER_PADDING, mainBackground.getTotalY() + INNER_PADDING);
        mainBackground.addWidget(pageOverview);
        this.pageOverview = pageOverview;
        pageOverview.setScrollAmount(state.getScrollAmount());
        } else {
            this.pageOverview = null;
        }

        int inventoryWidth = 176;
        int inventoryHeight = 96;
        int inventoryX = (width / 2) - (inventoryWidth / 2);
        int inventoryY = mainBackgroundY + mainBackgroundHeight;

        boolean showIndexPanel = SackConfig.showSackIndexPanel;
        int indexPanelX = inventoryX - INDEX_PANEL_WIDTH;
        if (showIndexPanel && indexPanelX < mainBackgroundX) {
            int blockX = mainBackgroundX + mainBackgroundWidth / 2 - (inventoryWidth + INDEX_PANEL_WIDTH) / 2;
            indexPanelX = blockX;
            inventoryX = blockX + INDEX_PANEL_WIDTH;
        }

        SpriteComponent inventory = new SpriteComponent(inventoryX, inventoryY, inventoryWidth, inventoryHeight, getInventoryTexture());
        screen.addComponent(inventory);
        this.inventoryPanel = inventory;

        this.indexPanel = null;
        if (showIndexPanel) {
            SpriteComponent panel = new SpriteComponent(indexPanelX, inventoryY, INDEX_PANEL_WIDTH, INDEX_PANEL_HEIGHT, getMainBackgroundTexture());
            screen.addComponent(panel);
            this.indexPanel = panel;

            boolean indexLive = liveKey != null && liveKey.type() == SackKey.Type.SACK_INDEX;
            TextComponent title = new TextComponent(INDEX_GRID_X, 4,
                    Component.translatable("helpbox.ui.sacks.index_panel"),
                    indexLive ? 0xFFFFD24A : getTitleTextColor());
            title.updateParentPosition(panel.getTotalX(), panel.getTotalY(), panel.getWidth(), panel.getHeight());
            title.setDrawShadow(shouldDrawTitleShadow());
            panel.addComponent(title);

            // While inside a specific sack, show the last known Sack of Sacks contents (read-only).
            if (!indexLive) {
                List<ItemStack> snapshot = state.getIndexSnapshot();
                int max = Math.min(snapshot.size(), INDEX_COLS * INDEX_ROWS);
                for (int i = 0; i < max; i++) {
                    ItemStack stack = snapshot.get(i);
                    if (stack.isEmpty()) continue;
                    TooltipItemComponent item = new TooltipItemComponent(
                            INDEX_GRID_X + (i % INDEX_COLS) * SLOT_SIZE + 1,
                            INDEX_GRID_Y + (i / INDEX_COLS) * SLOT_SIZE + 1,
                            stack, true);
                    item.setTooltipEnabled(SackConfig.showItemTooltipsOnCachedSackItems);
                    item.updateParentPosition(panel.getTotalX(), panel.getTotalY(), panel.getWidth(), panel.getHeight());
                    panel.addComponent(item);
                }
            }
        }



        EditBoxWidget searchBox = new EditBoxWidget(font, 70, 1, 100, 12, Component.translatable("enhanced_storage.ui.search_sacks"));
        searchBox.setValue(state.getSearchQuery());
        searchBox.setResponder(text -> {
            if (text.equals(state.getSearchQuery())) return;
            state.setSearchQuery(text);
            onSearchChanged.run();
        });
        searchBox.uilib$updateParentPosition(inventory.getTotalX(), inventory.getTotalY());
        screen.addWidget(searchBox);
        this.searchBox = searchBox;

        final int btnSize = 24;
        final int btnGap = 2;
        final int btnY = inventory.getTotalY() + 2;
        int btnX = inventory.getTotalX() + inventoryWidth + 2;

        if (SackConfig.showSackSettingsButton) {
            int iconSize = 16;
            IconButtonComponent settings = new IconButtonComponent(
                    btnX, btnY, btnSize, btnSize,
                    Identifier.fromNamespaceAndPath(StorageInitializer.TEXTURE_NAMESPACE, "icons/gear_icon"), iconSize,
                    List.of(Component.translatable("enhanced_storage.ui.settings").withStyle(s -> s.withColor(0x55FF55).withItalic(false))));
            settings.updateParentPosition(0, 0, width, height);
            screen.addComponent(settings);
            this.settingsButton = settings;
            this.settingsButtonBounds = new int[]{btnX, btnY, btnSize, btnSize};
            btnX += btnSize + btnGap;
        }



        if (SackConfig.showSackInsertInventoryButton) {
            ItemButtonComponent insertInv = new ItemButtonComponent(
                    btnX, btnY, btnSize, btnSize,
                    new ItemStack(Items.CHEST),
                    List.of(Component.translatable("enhanced_storage.ui.insert_inventory")
                            .withStyle(s -> s.withColor(0x55FF55).withItalic(false))));
            insertInv.updateParentPosition(0, 0, width, height);
            screen.addComponent(insertInv);
            this.insertInventoryButton = insertInv;
            this.insertInventoryButtonBounds = new int[]{btnX, btnY, btnSize, btnSize};
        }
    }
}
