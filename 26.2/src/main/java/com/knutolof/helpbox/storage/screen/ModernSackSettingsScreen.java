package com.knutolof.helpbox.storage.screen;
import com.knutolof.helpbox.storage.StorageInitializer;


import com.knutolof.helpbox.storage.config.EnhancedStorageConfig;
import com.knutolof.helpbox.storage.config.EnhancedStorageConfig.BackgroundType;
import com.knutolof.helpbox.storage.config.SackConfig;
import com.knutolof.helpbox.ui.ModernUiRenderHelper;
import com.knutolof.helpbox.util.HelpBoxLang;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ModernSackSettingsScreen extends Screen {

    private final Screen parent;
    private static final int COLOR_ACCENT = 0xFF38BDF8;
    private static final int COLOR_CARD_BG = 0xF00F172A;
    private static final int COLOR_CARD_BORDER = 0x6638BDF8;
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT_MUTED = 0xFF94A3B8;

    public ModernSackSettingsScreen(Screen parent) {
        super(Component.translatable("helpbox.ui.sacks.settings_title"));
        this.parent = parent;
    }

    private static boolean inRect(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);

        int cardW = 340;
        int cardH = 340;
        int cardX = (this.width - cardW) / 2;
        int cardY = (this.height - cardH) / 2;

        ModernUiRenderHelper.drawModernCard(graphics, cardX, cardY, cardW, cardH, 12, COLOR_CARD_BG, COLOR_CARD_BORDER);

        // Header
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.settings_title", "✦ HelpBox Sacks Ayarları"), cardX + 16, cardY + 14, COLOR_ACCENT, false);
        ModernUiRenderHelper.drawBadge(graphics, this.font, cardX + cardW - 65, cardY + 12, "SACKS", 0x3338BDF8, COLOR_ACCENT);

        // Option 1: Tema (Transparent / Dark / Light)
        int row1Y = cardY + 40;
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.theme", "Görünüm Teması:"), cardX + 16, row1Y + 3, COLOR_TEXT_PRIMARY, false);
        BackgroundType curType = SackConfig.sackBackgroundType;
        BackgroundType[] types = {BackgroundType.TRANSPARENT, BackgroundType.DARK, BackgroundType.LIGHT};
        String[] typeNames = {
                HelpBoxLang.get("helpbox.ui.storage.theme_transparent", "Şeffaf"),
                HelpBoxLang.get("helpbox.ui.storage.theme_dark", "Karanlık"),
                HelpBoxLang.get("helpbox.ui.storage.theme_light", "Açık")
        };
        int btnX = cardX + cardW - 16 - (3 * 54);
        for (int i = 0; i < types.length; i++) {
            boolean active = (curType == types[i]);
            boolean hov = inRect(mouseX, mouseY, btnX, row1Y, 50, 18);
            ModernUiRenderHelper.drawPillButton(graphics, this.font, btnX, row1Y, 50, 18, typeNames[i], COLOR_ACCENT, hov, active);
            btnX += 54;
        }

        // Option 2: Satır Başına Sacks (1 - 6)
        int row2Y = cardY + 68;
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.sacks_per_row", "Satır Başına Sacks:"), cardX + 16, row2Y + 3, COLOR_TEXT_PRIMARY, false);
        int curPages = SackConfig.sackMaxPagePerRow;
        int pageBtnX = cardX + cardW - 16 - (6 * 24);
        for (int p = 1; p <= 6; p++) {
            boolean active = (curPages == p);
            boolean hov = inRect(mouseX, mouseY, pageBtnX, row2Y, 20, 18);
            ModernUiRenderHelper.drawPillButton(graphics, this.font, pageBtnX, row2Y, 20, 18, String.valueOf(p), COLOR_ACCENT, hov, active);
            pageBtnX += 24;
        }

        // Option 3: Kart Boşluğu (Spacing: 0 - 6)
        int row3Y = cardY + 96;
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.card_spacing", "Kart Aralığı (px):"), cardX + 16, row3Y + 3, COLOR_TEXT_PRIMARY, false);
        int curSpacing = SackConfig.sackCardSpacing;
        int spBtnX = cardX + cardW - 16 - (7 * 21);
        for (int s = 0; s <= 6; s++) {
            boolean active = (curSpacing == s);
            boolean hov = inRect(mouseX, mouseY, spBtnX, row3Y, 18, 18);
            ModernUiRenderHelper.drawPillButton(graphics, this.font, spBtnX, row3Y, 18, 18, String.valueOf(s), COLOR_ACCENT, hov, active);
            spBtnX += 21;
        }

        // Option 4: Otomatik Kaydırma (Auto Scroll)
        int row4Y = cardY + 124;
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.auto_scroll", "Oto Kaydırma:"), cardX + 16, row4Y + 3, COLOR_TEXT_PRIMARY, false);
        EnhancedStorageConfig.AutoScrollMode curScroll = SackConfig.autoScrollToOpenSack;
        EnhancedStorageConfig.AutoScrollMode[] scrollModes = {
                EnhancedStorageConfig.AutoScrollMode.OFF,
                EnhancedStorageConfig.AutoScrollMode.IF_PARTLY_HIDDEN,
                EnhancedStorageConfig.AutoScrollMode.IF_FULLY_HIDDEN
        };
        String[] scrollNames = {
                HelpBoxLang.get("helpbox.ui.vault.scroll_off", "Kapalı"),
                HelpBoxLang.get("helpbox.ui.vault.scroll_partly", "Kısmi"),
                HelpBoxLang.get("helpbox.ui.vault.scroll_fully", "Tam")
        };
        int scBtnX = cardX + cardW - 16 - (3 * 54);
        for (int i = 0; i < scrollModes.length; i++) {
            boolean active = (curScroll == scrollModes[i]);
            boolean hov = inRect(mouseX, mouseY, scBtnX, row4Y, 50, 18);
            ModernUiRenderHelper.drawPillButton(graphics, this.font, scBtnX, row4Y, 50, 18, scrollNames[i], COLOR_ACCENT, hov, active);
            scBtnX += 54;
        }

        // Option 5: Toggles
        int row5Y = cardY + 154;
        boolean tog1Hov = inRect(mouseX, mouseY, cardX + 16, row5Y, 40, 18);
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, cardX + 16, row5Y, SackConfig.enableSackOverlay, tog1Hov);
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.enable_sacks", "Sacks Arayüzünü Etkinleştir"), cardX + 62, row5Y + 4, COLOR_TEXT_PRIMARY, false);

        int row6Y = cardY + 178;
        boolean tog2Hov = inRect(mouseX, mouseY, cardX + 16, row6Y, 40, 18);
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, cardX + 16, row6Y, SackConfig.onlyOfficialSacks, tog2Hov);
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.only_official", "Yalnızca Resmi Sacks"), cardX + 62, row6Y + 4, COLOR_TEXT_PRIMARY, false);

        int row7Y = cardY + 202;
        boolean tog3Hov = inRect(mouseX, mouseY, cardX + 16, row7Y, 40, 18);
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, cardX + 16, row7Y, SackConfig.showSackOverviewCard, tog3Hov);
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.show_overview", "Genel Bakış Kartını Göster"), cardX + 62, row7Y + 4, COLOR_TEXT_PRIMARY, false);

        int row8Y = cardY + 226;
        boolean tog4Hov = inRect(mouseX, mouseY, cardX + 16, row8Y, 40, 18);
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, cardX + 16, row8Y, SackConfig.showSackIndexPanel, tog4Hov);
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.show_index_panel", "Sack of Sacks Panelini Göster"), cardX + 62, row8Y + 4, COLOR_TEXT_PRIMARY, false);

        // Option 9: SkyBlock Profili
        int row9Y = cardY + 252;
        String curProf = com.knutolof.helpbox.storage.storage.StorageProfile.getInstance().current().orElse("default");
        graphics.text(this.font, HelpBoxLang.get("helpbox.ui.sacks.active_profile", "SkyBlock Profili:"), cardX + 16, row9Y + 4, COLOR_TEXT_PRIMARY, false);

        java.util.List<String> knownProfiles = com.knutolof.helpbox.storage.storage.StorageProfile.getInstance().getKnownProfiles();
        int pBtnX = cardX + cardW - 16;
        int maxProfiles = Math.min(knownProfiles.size(), 4);
        for (int i = maxProfiles - 1; i >= 0; i--) {
            String pName = knownProfiles.get(i);
            int bw = Math.max(38, this.font.width(pName) + 10);
            pBtnX -= bw;
            boolean active = curProf.equalsIgnoreCase(pName);
            boolean hov = inRect(mouseX, mouseY, pBtnX, row9Y, bw - 4, 18);
            ModernUiRenderHelper.drawPillButton(graphics, this.font, pBtnX, row9Y, bw - 4, 18, pName, COLOR_ACCENT, hov, active);
            pBtnX -= 4;
        }

        // Save & Close Button
        int saveBtnY = cardY + cardH - 34;
        boolean saveHov = inRect(mouseX, mouseY, cardX + 16, saveBtnY, cardW - 32, 22);
        ModernUiRenderHelper.drawPillButton(graphics, this.font, cardX + 16, saveBtnY, cardW - 32, 22, HelpBoxLang.get("helpbox.ui.sacks.save_close", "Kaydet & Kapat"), COLOR_ACCENT, saveHov, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);

        int cardW = 340;
        int cardH = 340;
        int cardX = (this.width - cardW) / 2;
        int cardY = (this.height - cardH) / 2;

        // Theme buttons
        int row1Y = cardY + 40;
        BackgroundType[] types = {BackgroundType.TRANSPARENT, BackgroundType.DARK, BackgroundType.LIGHT};
        int btnX = cardX + cardW - 16 - (3 * 54);
        for (BackgroundType type : types) {
            if (inRect(event.x(), event.y(), btnX, row1Y, 50, 18)) {
                SackConfig.sackBackgroundType = type;
                return true;
            }
            btnX += 54;
        }

        // Pages per row
        int row2Y = cardY + 68;
        int pageBtnX = cardX + cardW - 16 - (6 * 24);
        for (int p = 1; p <= 6; p++) {
            if (inRect(event.x(), event.y(), pageBtnX, row2Y, 20, 18)) {
                SackConfig.sackMaxPagePerRow = p;
                return true;
            }
            pageBtnX += 24;
        }

        // Spacing
        int row3Y = cardY + 96;
        int spBtnX = cardX + cardW - 16 - (7 * 21);
        for (int s = 0; s <= 6; s++) {
            if (inRect(event.x(), event.y(), spBtnX, row3Y, 18, 18)) {
                SackConfig.sackCardSpacing = s;
                return true;
            }
            spBtnX += 21;
        }

        // Auto Scroll
        int row4Y = cardY + 124;
        EnhancedStorageConfig.AutoScrollMode[] scrollModes = {
                EnhancedStorageConfig.AutoScrollMode.OFF,
                EnhancedStorageConfig.AutoScrollMode.IF_PARTLY_HIDDEN,
                EnhancedStorageConfig.AutoScrollMode.IF_FULLY_HIDDEN
        };
        int scBtnX = cardX + cardW - 16 - (3 * 54);
        for (EnhancedStorageConfig.AutoScrollMode mode : scrollModes) {
            if (inRect(event.x(), event.y(), scBtnX, row4Y, 50, 18)) {
                SackConfig.autoScrollToOpenSack = mode;
                return true;
            }
            scBtnX += 54;
        }

        // Toggle 1: enableSackOverlay
        int row5Y = cardY + 154;
        if (inRect(event.x(), event.y(), cardX + 16, row5Y, 40, 18)) {
            SackConfig.enableSackOverlay = !SackConfig.enableSackOverlay;
            return true;
        }

        // Toggle 2: onlyOfficialSacks
        int row6Y = cardY + 178;
        if (inRect(event.x(), event.y(), cardX + 16, row6Y, 40, 18)) {
            SackConfig.onlyOfficialSacks = !SackConfig.onlyOfficialSacks;
            return true;
        }

        // Toggle 3: showSackOverviewCard
        int row7Y = cardY + 202;
        if (inRect(event.x(), event.y(), cardX + 16, row7Y, 40, 18)) {
            SackConfig.showSackOverviewCard = !SackConfig.showSackOverviewCard;
            return true;
        }

        // Toggle 4: showSackIndexPanel
        int row8Y = cardY + 226;
        if (inRect(event.x(), event.y(), cardX + 16, row8Y, 40, 18)) {
            SackConfig.showSackIndexPanel = !SackConfig.showSackIndexPanel;
            return true;
        }

        // Profile buttons click
        int row9Y = cardY + 252;
        java.util.List<String> knownProfiles = com.knutolof.helpbox.storage.storage.StorageProfile.getInstance().getKnownProfiles();
        int maxProfiles = Math.min(knownProfiles.size(), 4);
        int pBtnX = cardX + cardW - 16;
        for (int i = maxProfiles - 1; i >= 0; i--) {
            String pName = knownProfiles.get(i);
            int bw = Math.max(38, this.font.width(pName) + 10);
            pBtnX -= bw;
            if (inRect(event.x(), event.y(), pBtnX, row9Y, bw - 4, 18)) {
                com.knutolof.helpbox.storage.storage.StorageProfile.getInstance().onProfileIdSeen(pName);
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(
                            Component.translatable("helpbox.ui.sacks.profile_switched", pName));
                }
                return true;
            }
            pBtnX -= 4;
        }

        // Save & Close button
        int saveBtnY = cardY + cardH - 34;
        if (inRect(event.x(), event.y(), cardX + 16, saveBtnY, cardW - 32, 22)) {
            saveAndClose();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void saveAndClose() {
        SackConfig.write(StorageInitializer.SACK_MOD_ID);
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
