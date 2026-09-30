package com.knutolof.helpbox.inventory.buttons;

import com.knutolof.helpbox.inventory.buttons.model.InventoryButton;
import com.knutolof.helpbox.ui.ModernUiRenderHelper;
import com.knutolof.helpbox.util.HelpBoxLang;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PopupEditor {

    public static boolean isOpen = false;
    private static InventoryButton editingButton = null;
    private static boolean isNewButton = false;

    private static EditBox iconBox;
    private static EditBox commandBox;

    private static int popupX, popupY, popupW, popupH;
    private static boolean hoverSave, hoverCancel, hoverDelete, hoverSize, hoverParse, hoverPreviewCard;

    // Icon Picker Modal State
    public static boolean isIconPickerOpen = false;
    private static EditBox pickerSearchBox;
    private static int pickerPage = 0;
    private static int pickerCategory = 0; // 0: All, 1: Combat & Tools, 2: Blocks, 3: Items, 4: Special
    private static int pickerX, pickerY, pickerW, pickerH;
    private static final int ITEMS_PER_PAGE = 36; // 9 columns x 4 rows
    private static List<ItemStack> ALL_REGISTRY_ITEMS = null;

    private static final int COLOR_ACCENT = 0xFF38BDF8;
    private static final int COLOR_MUTED = 0xFF94A3B8;
    private static final int COLOR_SUCCESS = 0xFF10B981;
    private static final int COLOR_DANGER = 0xFFEF4444;

    private static String statusMsg = "";
    private static int statusColor = 0;
    private static int statusTimer = 0;

    private static void ensureItemRegistryLoaded() {
        if (ALL_REGISTRY_ITEMS != null) return;
        ALL_REGISTRY_ITEMS = new ArrayList<>(1400);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            try {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) {
                    ALL_REGISTRY_ITEMS.add(stack);
                }
            } catch (Exception ignored) {}
        }
    }

    private static boolean matchesCategory(ItemStack stack, int category) {
        if (category == 0) return true;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = (id != null) ? id.getPath().toLowerCase() : "";

        return switch (category) {
            case 1 -> // Combat & Tools
                path.contains("sword") || path.contains("bow") || path.contains("trident")
                || path.contains("shield") || path.contains("axe") || path.contains("pickaxe")
                || path.contains("shovel") || path.contains("hoe") || path.contains("helmet")
                || path.contains("chestplate") || path.contains("leggings") || path.contains("boots")
                || path.contains("arrow") || path.contains("shears") || path.contains("fishing_rod")
                || path.contains("flint_and_steel") || path.contains("mace");
            case 2 -> // Blocks
                (stack.getItem() instanceof BlockItem)
                && !path.contains("head") && !path.contains("skull") && !path.contains("beacon");
            case 3 -> // Items & Materials
                !(stack.getItem() instanceof BlockItem)
                && !path.contains("sword") && !path.contains("bow") && !path.contains("axe")
                && !path.contains("pickaxe") && !path.contains("shovel") && !path.contains("hoe")
                && !path.contains("helmet") && !path.contains("chestplate") && !path.contains("leggings")
                && !path.contains("boots") && !path.contains("shield");
            case 4 -> // Special & Valuables
                path.contains("head") || path.contains("skull") || path.contains("star")
                || path.contains("beacon") || path.contains("totem") || path.contains("elytra")
                || path.contains("enchanted_book") || path.contains("dragon") || path.contains("heart")
                || path.contains("command_block") || path.contains("barrier") || path.contains("structure")
                || path.contains("spawner") || path.contains("conduit");
            default -> true;
        };
    }

    private static List<ItemStack> getFilteredPickerIcons() {
        ensureItemRegistryLoaded();
        String filter = (pickerSearchBox != null) ? pickerSearchBox.getValue().trim().toLowerCase() : "";

        List<ItemStack> list = new ArrayList<>();
        for (ItemStack st : ALL_REGISTRY_ITEMS) {
            if (!matchesCategory(st, pickerCategory)) continue;

            if (!filter.isEmpty()) {
                String name = st.getHoverName().getString().toLowerCase();
                Identifier key = BuiltInRegistries.ITEM.getKey(st.getItem());
                String path = (key != null) ? key.getPath().toLowerCase() : "";
                if (!name.contains(filter) && !path.contains(filter)) {
                    continue;
                }
            }
            list.add(st);
        }
        return list;
    }

    public static void open(InventoryButton btn, boolean isNew, int screenWidth, int screenHeight) {
        editingButton = btn;
        isNewButton = isNew;
        isOpen = true;
        isIconPickerOpen = false;
        pickerPage = 0;
        pickerCategory = 0;
        statusTimer = 0;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        popupW = 310;
        popupH = 210;
        popupX = screenWidth / 2 - popupW / 2;
        popupY = screenHeight / 2 - popupH / 2;

        int prevSize = 36;
        int fieldW = popupW - 32 - prevSize - 10;
        
        iconBox = new EditBox(font, popupX + 20, popupY + 44, fieldW - 8, 16, Component.literal("icon"));
        iconBox.setBordered(false);
        iconBox.setMaxLength(2048);
        String iconInitVal = (ButtonStore.pendingIconStr != null) ? ButtonStore.pendingIconStr : (btn.icon != null ? btn.icon : "");
        iconBox.setValue(iconInitVal);
        iconBox.setCursorPosition(iconInitVal.length());
        iconBox.setHighlightPos(iconInitVal.length());
        iconBox.setTextColor(0xFFFFFFFF);

        commandBox = new EditBox(font, popupX + 20, popupY + 110, popupW - 40, 16, Component.literal("cmd"));
        commandBox.setBordered(false);
        commandBox.setMaxLength(128);
        String cmdInitVal = btn.command != null ? btn.command : "";
        commandBox.setValue(cmdInitVal);
        commandBox.setCursorPosition(cmdInitVal.length());
        commandBox.setHighlightPos(cmdInitVal.length());
        commandBox.setTextColor(0xFFFFFFFF);
        
        // Icon Picker Modal dimensions
        pickerW = 286;
        pickerH = 250;
        pickerX = screenWidth / 2 - pickerW / 2;
        pickerY = screenHeight / 2 - pickerH / 2;

        pickerSearchBox = new EditBox(font, pickerX + 16, pickerY + 30, pickerW - 32, 16, Component.literal("search"));
        pickerSearchBox.setBordered(false);
        pickerSearchBox.setMaxLength(64);
        pickerSearchBox.setHint(Component.translatable("helpbox.ui.picker.search_hint"));
        pickerSearchBox.setTextColor(0xFFFFFFFF);

        // Focus iconBox initially
        iconBox.setFocused(true);
        commandBox.setFocused(false);
    }

    public static void setIconValue(String iconStr) {
        if (iconBox != null && iconStr != null) {
            iconBox.setValue(iconStr);
        }
    }

    public static void close() {
        isOpen = false;
        isIconPickerOpen = false;
        editingButton = null;
        iconBox = null;
        commandBox = null;
        pickerSearchBox = null;
        ButtonStore.pendingIconStr = null;
        ButtonStore.iconPendingStack = null;
    }

    private static void save() {
        if (editingButton == null) return;
        String icon = iconBox != null ? iconBox.getValue().trim() : "";
        String cmd = commandBox != null ? commandBox.getValue().trim() : "";

        if (icon.isEmpty() || cmd.isEmpty()) {
            setStatus(HelpBoxLang.str("helpbox.ui.buttons.err_empty", "İkon ve komut boş olamaz!"), COLOR_DANGER);
            return;
        }

        icon = ButtonRenderer.normalizeIconInput(icon);

        editingButton.icon = icon;
        editingButton.command = cmd.startsWith("/") ? cmd.substring(1) : cmd;

        if (isNewButton) {
            editingButton.id = UUID.randomUUID().toString();
            ButtonStore.buttons.add(editingButton);
        }

        if (ButtonStore.iconPendingStack != null && !ButtonStore.iconPendingStack.isEmpty()) {
            ButtonStore.iconItemCache.put(editingButton.id, ButtonStore.iconPendingStack);
            ButtonStore.iconPendingStack = null;
        }

        ButtonRenderer.clearCache();
        ButtonStore.save();
        close();
    }

    private static void deleteButton() {
        if (editingButton != null && !isNewButton) {
            ButtonStore.buttons.remove(editingButton);
            ButtonStore.save();
            ButtonRenderer.clearCache();
            close();
        }
    }

    public static void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!isOpen) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // Dark background dim
        graphics.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), 0xAA000000);

        // Main Editor Card
        ModernUiRenderHelper.drawModernCard(graphics, popupX, popupY, popupW, popupH, 12, 0xF20F172A, 0x8838BDF8);

        // Header Title
        String title = isNewButton 
            ? HelpBoxLang.str("helpbox.ui.buttons.new_title", "✦ Yeni Buton Oluştur") 
            : HelpBoxLang.str("helpbox.ui.buttons.edit_title", "✦ Buton Ayarları");
        graphics.text(font, title, popupX + 16, popupY + 12, COLOR_ACCENT, false);

        // Status Notification Badge
        if (statusTimer > 0) {
            statusTimer--;
            ModernUiRenderHelper.drawBadge(graphics, font, popupX + popupW - font.width(statusMsg) - 24, popupY + 10, statusMsg, 0x44000000, statusColor);
        }

        // 1. İkon Bölümü
        graphics.text(font, HelpBoxLang.str("helpbox.ui.buttons.icon_id", "İkon ID:"), popupX + 16, popupY + 31, COLOR_MUTED, false);
        
        int prevSize = 36;
        int prevX = popupX + popupW - 16 - prevSize;
        int prevY = popupY + 33;
        int fieldW = popupW - 32 - prevSize - 10;

        int iconInBg = (iconBox != null && iconBox.isFocused()) ? 0xFF1E293B : 0xFF111827;
        int iconInBorder = (iconBox != null && iconBox.isFocused()) ? 0xFF38BDF8 : 0x55475569;
        ModernUiRenderHelper.fillRoundedRect(graphics, popupX + 16, popupY + 42, fieldW, 20, 6, iconInBg);
        ModernUiRenderHelper.drawRoundedOutline(graphics, popupX + 16, popupY + 42, fieldW, 20, 6, iconInBorder);

        // Parse Button
        hoverParse = isHovered(mouseX, mouseY, popupX + 16, popupY + 66, fieldW, 16);
        ModernUiRenderHelper.drawPillButton(graphics, font, popupX + 16, popupY + 66, fieldW, 16, HelpBoxLang.str("helpbox.ui.buttons.parse", "Parse /give"), 0xFF38BDF8, hoverParse, false);

        // Preview Card
        hoverPreviewCard = isHovered(mouseX, mouseY, prevX, prevY, prevSize, prevSize);
        int prevBorder = hoverPreviewCard ? 0xFF38BDF8 : 0x66475569;
        int prevBg = hoverPreviewCard ? 0xFF273549 : 0xFF1E293B;
        ModernUiRenderHelper.drawModernCard(graphics, prevX, prevY, prevSize, prevSize, 8, prevBg, prevBorder);

        String iconVal = iconBox != null ? iconBox.getValue().trim() : "";
        if (iconVal.startsWith("/give") || iconVal.startsWith("give")) {
            String parsed = ButtonRenderer.extractItemIdFromGive(iconVal);
            if (parsed != null) iconVal = parsed;
        }

        ItemStack previewStack = (ButtonStore.iconPendingStack != null && !ButtonStore.iconPendingStack.isEmpty())
                ? ButtonStore.iconPendingStack
                : ButtonRenderer.resolveIcon(iconVal);

        if (previewStack != null && !previewStack.isEmpty()) {
            try { graphics.item(previewStack, prevX + 10, prevY + 10); } catch (Exception ignored) {}
        } else {
            graphics.text(font, "?", prevX + prevSize / 2 - 3, prevY + prevSize / 2 - 4, COLOR_MUTED, false);
        }

        // Tiny hint under preview card
        graphics.centeredText(font, HelpBoxLang.str("helpbox.ui.picker.choose", "İkon Seç"), prevX + prevSize / 2, prevY + prevSize + 3, COLOR_ACCENT);

        if (hoverPreviewCard && !isIconPickerOpen) {
            graphics.setTooltipForNextFrame(font, Component.translatable("helpbox.ui.picker.open_hint"), mouseX, mouseY);
        }

        // 2. Komut Bölümü
        graphics.text(font, HelpBoxLang.str("helpbox.ui.buttons.cmd_macro_label", "Komut (veya @macro:Makroİsmi):"), popupX + 16, popupY + 95, COLOR_MUTED, false);
        int cmdInBg = (commandBox != null && commandBox.isFocused()) ? 0xFF1E293B : 0xFF111827;
        int cmdInBorder = (commandBox != null && commandBox.isFocused()) ? 0xFF38BDF8 : 0x55475569;
        ModernUiRenderHelper.fillRoundedRect(graphics, popupX + 16, popupY + 108, popupW - 32, 20, 6, cmdInBg);
        ModernUiRenderHelper.drawRoundedOutline(graphics, popupX + 16, popupY + 108, popupW - 32, 20, 6, cmdInBorder);

        // 3. Boyut Seçimi (Pill Toggle)
        String sizeVal = (editingButton != null && editingButton.isGigantic)
                ? HelpBoxLang.str("helpbox.ui.buttons.size_big")
                : HelpBoxLang.str("helpbox.ui.buttons.size_normal");
        String sizeTxt = HelpBoxLang.str("helpbox.ui.buttons.size_toggle", sizeVal);
        hoverSize = isHovered(mouseX, mouseY, popupX + 16, popupY + 135, popupW - 32, 18);
        ModernUiRenderHelper.drawPillButton(graphics, font, popupX + 16, popupY + 135, popupW - 32, 18, sizeTxt, 0xFF38BDF8, hoverSize, editingButton != null && editingButton.isGigantic);

        // 4. Eylem Butonları (Kaydet, Sil, İptal)
        int btnY = popupY + 166;
        if (!isNewButton) {
            int gap = 6;
            int bW = (popupW - 32 - gap * 2) / 3;
            int b1X = popupX + 16;
            int b2X = b1X + bW + gap;
            int b3X = b2X + bW + gap;

            hoverSave = isHovered(mouseX, mouseY, b1X, btnY, bW, 22);
            ModernUiRenderHelper.drawPillButton(graphics, font, b1X, btnY, bW, 22, HelpBoxLang.str("helpbox.ui.buttons.save_short", "Kaydet"), COLOR_SUCCESS, hoverSave, false);

            hoverDelete = isHovered(mouseX, mouseY, b2X, btnY, bW, 22);
            ModernUiRenderHelper.drawPillButton(graphics, font, b2X, btnY, bW, 22, HelpBoxLang.str("helpbox.ui.buttons.btn_delete", "Sil"), COLOR_DANGER, hoverDelete, false);

            hoverCancel = isHovered(mouseX, mouseY, b3X, btnY, bW, 22);
            ModernUiRenderHelper.drawPillButton(graphics, font, b3X, btnY, bW, 22, HelpBoxLang.str("helpbox.ui.buttons.cancel_short", "İptal"), 0xFF64748B, hoverCancel, false);
        } else {
            int gap = 8;
            int bW = (popupW - 32 - gap) / 2;
            int b1X = popupX + 16;
            int b2X = b1X + bW + gap;

            hoverSave = isHovered(mouseX, mouseY, b1X, btnY, bW, 22);
            ModernUiRenderHelper.drawPillButton(graphics, font, b1X, btnY, bW, 22, HelpBoxLang.str("helpbox.ui.buttons.save_short", "Kaydet"), COLOR_SUCCESS, hoverSave, false);

            hoverCancel = isHovered(mouseX, mouseY, b2X, btnY, bW, 22);
            ModernUiRenderHelper.drawPillButton(graphics, font, b2X, btnY, bW, 22, HelpBoxLang.str("helpbox.ui.buttons.cancel_short", "İptal"), 0xFF64748B, hoverCancel, false);

            hoverDelete = false;
        }

        // Render input textboxes
        if (iconBox != null) iconBox.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        if (commandBox != null) commandBox.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);

        // 5. ICON PICKER MODAL (Layered on top when open)
        if (isIconPickerOpen) {
            renderIconPicker(graphics, font, mouseX, mouseY, partialTick);
        }
    }

    private static void renderIconPicker(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        // Overlay Dim
        graphics.fill(0, 0, Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight(), 0xCC000000);

        // Modal Frame
        ModernUiRenderHelper.drawModernCard(graphics, pickerX, pickerY, pickerW, pickerH, 12, 0xF80B132B, 0xCC38BDF8);

        // Title & Close Button
        graphics.text(font, HelpBoxLang.str("helpbox.ui.picker.title", "✦ İkon & Blok Seçici"), pickerX + 16, pickerY + 12, COLOR_ACCENT, false);
        boolean closeHov = isHovered(mouseX, mouseY, pickerX + pickerW - 24, pickerY + 10, 14, 14);
        graphics.text(font, "✕", pickerX + pickerW - 20, pickerY + 12, closeHov ? COLOR_DANGER : COLOR_MUTED, false);

        // Search Box Field
        int sBg = (pickerSearchBox != null && pickerSearchBox.isFocused()) ? 0xFF1E293B : 0xFF111827;
        int sBorder = (pickerSearchBox != null && pickerSearchBox.isFocused()) ? 0xFF38BDF8 : 0x55475569;
        ModernUiRenderHelper.fillRoundedRect(graphics, pickerX + 16, pickerY + 28, pickerW - 32, 18, 6, sBg);
        ModernUiRenderHelper.drawRoundedOutline(graphics, pickerX + 16, pickerY + 28, pickerW - 32, 18, 6, sBorder);
        if (pickerSearchBox != null) pickerSearchBox.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);

        // Category Filter Buttons
        int catY = pickerY + 50;
        String[] catLabels = {
                HelpBoxLang.str("helpbox.ui.picker.cat_all", "Tümü"),
                HelpBoxLang.str("helpbox.ui.picker.cat_combat", "Savaş"),
                HelpBoxLang.str("helpbox.ui.picker.cat_blocks", "Bloklar"),
                HelpBoxLang.str("helpbox.ui.picker.cat_items", "Eşyalar"),
                HelpBoxLang.str("helpbox.ui.picker.cat_special", "Özel")
        };
        int catBtnW = (pickerW - 32 - 16) / 5;
        int catBtnH = 15;
        for (int c = 0; c < 5; c++) {
            int cx = pickerX + 16 + c * (catBtnW + 4);
            boolean catActive = (pickerCategory == c);
            boolean catHov = isHovered(mouseX, mouseY, cx, catY, catBtnW, catBtnH);
            ModernUiRenderHelper.drawPillButton(graphics, font, cx, catY, catBtnW, catBtnH, catLabels[c], COLOR_ACCENT, catHov, catActive);
        }

        // Filtered items list
        List<ItemStack> filtered = getFilteredPickerIcons();
        int maxPages = Math.max(1, (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE));
        if (pickerPage >= maxPages) pickerPage = maxPages - 1;
        if (pickerPage < 0) pickerPage = 0;

        int startIndex = pickerPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filtered.size());

        // Grid of items: 9 columns x 4 rows
        int slotW = 24;
        int slotH = 24;
        int gap = 3;
        int gridX = pickerX + 23;
        int gridY = pickerY + 70;

        for (int i = startIndex; i < endIndex; i++) {
            int slotIdx = i - startIndex;
            int col = slotIdx % 9;
            int row = slotIdx / 9;

            int sx = gridX + col * (slotW + gap);
            int sy = gridY + row * (slotH + gap);

            boolean hov = isHovered(mouseX, mouseY, sx, sy, slotW, slotH);
            int slotBg = hov ? 0x9938BDF8 : 0x441E293B;
            int slotBorder = hov ? 0xFF38BDF8 : 0x33475569;

            ModernUiRenderHelper.fillRoundedRect(graphics, sx, sy, slotW, slotH, 6, slotBg);
            ModernUiRenderHelper.drawRoundedOutline(graphics, sx, sy, slotW, slotH, 6, slotBorder);

            ItemStack it = filtered.get(i);
            try {
                graphics.item(it, sx + 4, sy + 4);
            } catch (Exception ignored) {}

            if (hov) {
                graphics.setTooltipForNextFrame(font, it.getHoverName(), mouseX, mouseY);
            }
        }

        // Pagination row at bottom
        int pagY = pickerY + pickerH - 24;
        boolean prevHov = isHovered(mouseX, mouseY, pickerX + 16, pagY, 20, 16);
        ModernUiRenderHelper.drawPillButton(graphics, font, pickerX + 16, pagY, 20, 16, "◀", COLOR_ACCENT, prevHov, false);

        String pageInfo = String.format("%d / %d (%d)", pickerPage + 1, maxPages, filtered.size());
        graphics.centeredText(font, pageInfo, pickerX + pickerW / 2, pagY + 4, COLOR_MUTED);

        boolean nextHov = isHovered(mouseX, mouseY, pickerX + pickerW - 36, pagY, 20, 16);
        ModernUiRenderHelper.drawPillButton(graphics, font, pickerX + pickerW - 36, pagY, 20, 16, "▶", COLOR_ACCENT, nextHov, false);
    }

    public static boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (!isOpen) return false;

        int button = event.button();
        double mouseX = event.x();
        double mouseY = event.y();

        // 1. Handle clicks in Icon Picker Modal if open
        if (isIconPickerOpen) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                // Close button (✕)
                if (isHovered(mouseX, mouseY, pickerX + pickerW - 24, pickerY + 10, 16, 16)) {
                    isIconPickerOpen = false;
                    return true;
                }

                // Search box click
                if (pickerSearchBox != null && isHovered(mouseX, mouseY, pickerX + 16, pickerY + 28, pickerW - 32, 18)) {
                    pickerSearchBox.setFocused(true);
                    pickerSearchBox.mouseClicked(event, handled);
                    return true;
                } else if (pickerSearchBox != null && !isHovered(mouseX, mouseY, pickerX + 16, pickerY + 28, pickerW - 32, 18)) {
                    pickerSearchBox.setFocused(false);
                }

                // Category Buttons click
                int catY = pickerY + 50;
                int catBtnW = (pickerW - 32 - 16) / 5;
                int catBtnH = 15;
                for (int c = 0; c < 5; c++) {
                    int cx = pickerX + 16 + c * (catBtnW + 4);
                    if (isHovered(mouseX, mouseY, cx, catY, catBtnW, catBtnH)) {
                        pickerCategory = c;
                        pickerPage = 0;
                        return true;
                    }
                }

                // Grid click
                List<ItemStack> filtered = getFilteredPickerIcons();
                int startIndex = pickerPage * ITEMS_PER_PAGE;
                int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filtered.size());

                int slotW = 24;
                int slotH = 24;
                int gap = 3;
                int gridX = pickerX + 23;
                int gridY = pickerY + 70;

                for (int i = startIndex; i < endIndex; i++) {
                    int slotIdx = i - startIndex;
                    int col = slotIdx % 9;
                    int row = slotIdx / 9;

                    int sx = gridX + col * (slotW + gap);
                    int sy = gridY + row * (slotH + gap);

                    if (isHovered(mouseX, mouseY, sx, sy, slotW, slotH)) {
                        ItemStack chosen = filtered.get(i);
                        Identifier id = BuiltInRegistries.ITEM.getKey(chosen.getItem());
                        if (id != null) {
                            String iconVal = id.toString();
                            if (iconBox != null) iconBox.setValue(iconVal);
                            ButtonStore.iconPendingStack = chosen.copy();
                            ButtonRenderer.clearCache();
                            setStatus(HelpBoxLang.str("helpbox.ui.picker.selected", chosen.getHoverName().getString()), COLOR_SUCCESS);
                            isIconPickerOpen = false;
                            return true;
                        }
                    }
                }

                // Pagination clicks
                int maxPages = Math.max(1, (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE));
                int pagY = pickerY + pickerH - 24;
                if (isHovered(mouseX, mouseY, pickerX + 16, pagY, 20, 16)) {
                    if (pickerPage > 0) pickerPage--;
                    return true;
                }
                if (isHovered(mouseX, mouseY, pickerX + pickerW - 36, pagY, 20, 16)) {
                    if (pickerPage < maxPages - 1) pickerPage++;
                    return true;
                }

                // Clicking outside picker closes it
                if (!isHovered(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH)) {
                    isIconPickerOpen = false;
                    return true;
                }
            }
            return true;
        }

        // 2. Handle Editor clicks
        int prevSize = 36;
        int fieldW = popupW - 32 - prevSize - 10;
        if (iconBox != null && isHovered(mouseX, mouseY, popupX + 16, popupY + 40, fieldW, 24)) {
            iconBox.setFocused(true);
            if (commandBox != null) commandBox.setFocused(false);
            iconBox.mouseClicked(event, handled);
            return true;
        }
        if (commandBox != null && isHovered(mouseX, mouseY, popupX + 16, popupY + 106, popupW - 32, 24)) {
            if (iconBox != null) iconBox.setFocused(false);
            commandBox.setFocused(true);
            commandBox.mouseClicked(event, handled);
            return true;
        }

        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            // Preview card or "İkon Seç" click -> open picker
            int prevX = popupX + popupW - 16 - prevSize;
            int prevY = popupY + 33;
            if (isHovered(mouseX, mouseY, prevX, prevY, prevSize, prevSize + 12)) {
                isIconPickerOpen = true;
                pickerCategory = 0;
                pickerPage = 0;
                if (pickerSearchBox != null) {
                    pickerSearchBox.setValue("");
                    pickerSearchBox.setFocused(true);
                }
                return true;
            }

            if (hoverSave) {
                save();
                return true;
            }
            if (hoverDelete && !isNewButton) {
                deleteButton();
                return true;
            }
            if (hoverCancel) {
                close();
                return true;
            }
            if (hoverSize && editingButton != null) {
                editingButton.isGigantic = !editingButton.isGigantic;
                return true;
            }
            if (hoverParse && iconBox != null) {
                String val = iconBox.getValue().trim();
                String normalized = ButtonRenderer.normalizeIconInput(val);
                if (!normalized.isEmpty() && !normalized.equalsIgnoreCase(val)) {
                    iconBox.setValue(normalized);
                    setStatus(HelpBoxLang.str("helpbox.ui.buttons.parsed", (normalized.length() > 25 ? normalized.substring(0, 22) + "..." : normalized)), COLOR_SUCCESS);
                } else {
                    setStatus(HelpBoxLang.str("helpbox.ui.buttons.icon_ready", "✓ İkon hazır!"), COLOR_SUCCESS);
                }
                return true;
            }
        }
        
        return true; 
    }

    public static boolean charTyped(char chr, int modifiers) {
        if (!isOpen) return false;
        return false;
    }
    
    public static boolean charTyped(CharacterEvent event) {
        if (!isOpen) return false;
        if (isIconPickerOpen) {
            if (pickerSearchBox != null && pickerSearchBox.isFocused()) {
                boolean typed = pickerSearchBox.charTyped(event);
                if (typed) pickerPage = 0;
                return true;
            }
            return true;
        }
        if (iconBox != null && iconBox.isFocused()) {
            return iconBox.charTyped(event);
        }
        if (commandBox != null && commandBox.isFocused()) {
            return commandBox.charTyped(event);
        }
        return true;
    }

    public static boolean keyPressed(KeyEvent event) {
        if (!isOpen) return false;
        
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (isIconPickerOpen) {
                isIconPickerOpen = false;
                return true;
            }
            close();
            return true;
        }

        if (isIconPickerOpen) {
            if (pickerSearchBox != null && pickerSearchBox.isFocused()) {
                if (pickerSearchBox.keyPressed(event)) {
                    pickerPage = 0;
                    return true;
                }
            }
            return true;
        }

        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            save();
            return true;
        }

        if (event.key() == InputConstants.KEY_TAB) {
            if (iconBox != null && commandBox != null) {
                if (iconBox.isFocused()) {
                    iconBox.setFocused(false);
                    commandBox.setFocused(true);
                } else {
                    iconBox.setFocused(true);
                    commandBox.setFocused(false);
                }
            }
            return true;
        }

        if (iconBox != null && iconBox.isFocused()) {
            if (iconBox.keyPressed(event)) return true;
        }
        if (commandBox != null && commandBox.isFocused()) {
            if (commandBox.keyPressed(event)) return true;
        }
        return true;
    }

    private static boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void setStatus(String msg, int color) {
        statusMsg = msg;
        statusColor = color;
        statusTimer = 60;
    }
}
