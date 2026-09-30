package com.knutolof.helpbox.ui;
import com.knutolof.helpbox.storage.StorageInitializer;


import com.knutolof.helpbox.storage.config.EnhancedStorageConfig.BackgroundType;
import com.knutolof.helpbox.storage.config.EnhancedStorageConfig;
import com.knutolof.helpbox.storage.config.SackConfig;
import com.knutolof.helpbox.storage.screen.ModernSackSettingsScreen;
import com.knutolof.helpbox.storage.screen.ModernStorageSettingsScreen;
import com.knutolof.helpbox.config.HelpBoxConfig;
import com.knutolof.helpbox.console.ConsoleHistory;
import com.knutolof.helpbox.console.ConsoleScreen;
import com.knutolof.helpbox.inventory.buttons.ButtonEditorScreen;
import com.knutolof.helpbox.inventory.buttons.ButtonStore;
import com.knutolof.helpbox.navigation.NavigationKeybind;
import com.knutolof.helpbox.navigation.WaypointManager;
import com.knutolof.helpbox.navigation.model.Waypoint;
import com.knutolof.helpbox.navigation.ui.NavigationScreen;
import com.knutolof.helpbox.util.HelpBoxLang;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * Hypixel HelpBox - Modern kontrol ve ayar merkezi.
 * 0: GPS & Radar, 1: Özel Butonlar, 2: Gelişmiş Depo, 3: Gelişmiş Sacks, 4: Oyun Konsolu, 5: Hızlı Kopyalama
 */
public class HelpBoxControlCenterScreen extends Screen {

    private int activeTab = 0;
    private int listeningKeybindType = 0; // 0: None, 1: copyItemName, 2: copyItemSkull

    public static String getTabTitle(int i) {
        return switch (i) {
            case 0 -> HelpBoxLang.str("helpbox.ui.tab.gps");
            case 1 -> HelpBoxLang.str("helpbox.ui.tab.buttons");
            case 2 -> HelpBoxLang.str("helpbox.ui.tab.storage");
            case 3 -> HelpBoxLang.str("helpbox.ui.tab.sacks");
            case 4 -> HelpBoxLang.str("helpbox.ui.tab.console");
            case 5 -> HelpBoxLang.str("helpbox.ui.tab.copier");
            default -> "";
        };
    }

    private static final String[] TAB_ICONS = {
            "\u2295", // 0: GPS (⊕)
            "\u26A1", // 1: Buttons (⚡)
            "\u25A6", // 2: Storage (▦)
            "\u25C8", // 3: Sacks (◈)
            "\u25B6", // 4: Console (▶)
            "\u270E"  // 5: Copier (✎)
    };

    // UI Renk Paleti (Modern Slate & Indigo)
    private static final int COLOR_OVERLAY       = 0x99000000;
    private static final int COLOR_WINDOW_BG     = 0xF0111827; // Tailwind Slate-900 Glass
    private static final int COLOR_WINDOW_BORDER = 0x5538BDF8; // Soft Sky/Cyan Accent
    private static final int COLOR_SIDEBAR_BG    = 0x400F172A; // Deep Sidebar
    private static final int COLOR_CARD_BG       = 0x601E293B; // Slate-800 Card
    private static final int COLOR_CARD_BORDER   = 0x3064748B; // Subtle Card Outline
    private static final int COLOR_ACCENT        = 0xFF38BDF8; // Sky-400
    private static final int COLOR_SUCCESS       = 0xFF10B981; // Emerald-500
    private static final int COLOR_DANGER        = 0xFFEF4444; // Rose-500
    private static final int COLOR_TEXT_PRIMARY  = 0xFFF8FAFC;
    private static final int COLOR_TEXT_MUTED    = 0xFF94A3B8;

    private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("helpbox", "textures/gui/logo.png");

    public HelpBoxControlCenterScreen() {
        super(HelpBoxLang.tr("helpbox.ui.control_center.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void playClickSound() {
        try {
            this.minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            SoundEvents.UI_BUTTON_CLICK, 1.0F
                    )
            );
        } catch (Exception ignored) {}
    }

    private int calculateTabStartY(int startY, int sidebarW) {
        int lines = Math.max(1, this.font.getSplitter().splitLines(HelpBoxLang.str("helpbox.ui.control_center.subtitle"), sidebarW - 42, Style.EMPTY).size());
        return Math.max(startY + 48, startY + 25 + lines * 9 + 6);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Karartma arka planı
        graphics.fill(0, 0, this.width, this.height, COLOR_OVERLAY);

        int dialogW = Math.min(this.width - 30, 580);
        int dialogH = Math.min(this.height - 30, 360);
        int startX = (this.width - dialogW) / 2;
        int startY = (this.height - dialogH) / 2;

        // 1. Ana Pencere
        ModernUiRenderHelper.drawModernCard(graphics, startX, startY, dialogW, dialogH, 14, COLOR_WINDOW_BG, COLOR_WINDOW_BORDER);

        int sidebarW = 145;
        // Sol Kenar Çubuğu Arka Planı
        ModernUiRenderHelper.fillRoundedRect(graphics, startX + 2, startY + 2, sidebarW, dialogH - 4, 12, COLOR_SIDEBAR_BG);
        // Sidebar Dikey Ayırıcı Çizgi
        graphics.fill(startX + sidebarW + 2, startY + 12, startX + sidebarW + 3, startY + dialogH - 12, 0x22FFFFFF);

        // Sidebar Başlığı & Logo
        graphics.blit(LOGO_TEXTURE, startX + 10, startY + 12, startX + 30, startY + 32, 0.0f, 1.0f, 0.0f, 1.0f);
        graphics.text(this.font, "HELPBOX", startX + 34, startY + 13, COLOR_ACCENT, false);
        ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.control_center.subtitle"), startX + 34, startY + 24, sidebarW - 38, COLOR_TEXT_MUTED, 9);

        // Sidebar Sekmeleri (Alt başlık yüksekliğine göre dinamik başlangıç)
        int tabStartY = calculateTabStartY(startY, sidebarW);
        int tabH = 26;
        int tabGap = 4;
        for (int i = 0; i < TAB_ICONS.length; i++) {
            int ty = tabStartY + i * (tabH + tabGap);
            boolean isSelected = (i == activeTab);
            boolean isHovered = mouseX >= startX + 10 && mouseX <= startX + sidebarW - 6 && mouseY >= ty && mouseY <= ty + tabH;

            int tabBg = isSelected ? 0x3338BDF8 : (isHovered ? 0x22334155 : 0x00000000);
            int tabBorder = isSelected ? 0x9938BDF8 : (isHovered ? 0x4464748B : 0x00000000);
            int textColor = isSelected ? COLOR_TEXT_PRIMARY : (isHovered ? 0xFFE2E8F0 : COLOR_TEXT_MUTED);

            ModernUiRenderHelper.fillRoundedRect(graphics, startX + 10, ty, sidebarW - 16, tabH, 6, tabBg);
            if (tabBorder != 0) {
                ModernUiRenderHelper.drawRoundedOutline(graphics, startX + 10, ty, sidebarW - 16, tabH, 6, tabBorder);
            }

            // Sol Seçim İndikatörü
            if (isSelected) {
                ModernUiRenderHelper.fillRoundedRect(graphics, startX + 10, ty + 4, 3, tabH - 8, 2, COLOR_ACCENT);
            }

            // Sekme İkonu & İsmi
            graphics.text(this.font, TAB_ICONS[i], startX + 18, ty + 9, isSelected ? COLOR_ACCENT : textColor, false);
            String title = getTabTitle(i);
            if (this.font.width(title) > sidebarW - 48) {
                title = this.font.plainSubstrByWidth(title, sidebarW - 54) + "..";
            }
            graphics.text(this.font, title, startX + 34, ty + 9, textColor, false);
        }

        // Kapat Butonu (Sidebar Altı)
        int closeBtnY = startY + dialogH - 32;
        boolean closeHov = mouseX >= startX + 10 && mouseX <= startX + sidebarW - 6 && mouseY >= closeBtnY && mouseY <= closeBtnY + 22;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, startX + 10, closeBtnY, sidebarW - 16, 22, HelpBoxLang.str("helpbox.ui.control_center.close"), 0xFFEF4444, closeHov, false);

        // 2. Sağ İçerik Alanı
        int contentX = startX + sidebarW + 16;
        int contentY = startY + 16;
        int contentW = dialogW - sidebarW - 32;
        int contentH = dialogH - 32;

        renderTabContent(graphics, contentX, contentY, contentW, contentH, mouseX, mouseY);
    }

    private void renderTabContent(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        switch (activeTab) {
            case 0 -> renderGpsTab(graphics, x, y, w, h, mouseX, mouseY);
            case 1 -> renderButtonsTab(graphics, x, y, w, h, mouseX, mouseY);
            case 2 -> renderStorageTab(graphics, x, y, w, h, mouseX, mouseY);
            case 3 -> renderSacksTab(graphics, x, y, w, h, mouseX, mouseY);
            case 4 -> renderConsoleTab(graphics, x, y, w, h, mouseX, mouseY);
            case 5 -> renderCopierTab(graphics, x, y, w, h, mouseX, mouseY);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 0: 3D GPS & RADAR
    // ─────────────────────────────────────────────────────────────────────────────
    private void renderGpsTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.gps.title"), x, y + 2, COLOR_TEXT_PRIMARY, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.gps.subtitle"), x, y + 16, COLOR_TEXT_MUTED, false);

        Waypoint activeWp = WaypointManager.getActiveWaypoint();
        if (activeWp != null) {
            String targetBadge = HelpBoxLang.str("helpbox.ui.gps.target") + " " + activeWp.getName();
            ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 140, y, targetBadge, 0x3310B981, COLOR_SUCCESS);
        } else {
            ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 120, y, HelpBoxLang.str("helpbox.ui.gps.no_target"), 0x3364748B, COLOR_TEXT_MUTED);
        }

        int cardY = y + 36;
        int cardH = 76;
        // Kart 1: Navigasyon Davranışları
        ModernUiRenderHelper.drawModernCard(graphics, x, cardY, w, cardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.gps.card_nav"), x + 12, cardY + 10, COLOR_ACCENT, false);

        // Toggle: Auto-Clear Waypoint
        int toggleY = cardY + 28;
        boolean autoClear = WaypointManager.isAutoClearEnabled();
        boolean toggleHover = mouseX >= x + 12 && mouseX <= x + 50 && mouseY >= toggleY && mouseY <= toggleY + 20;
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, x + 12, toggleY, autoClear, toggleHover);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.gps.auto_clear"), x + 58, toggleY + 5, COLOR_TEXT_PRIMARY, false);

        // Opaklık Seçimi (Çizgi Opaklığı)
        int opacityY = cardY + 54;
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.gps.line_opacity"), x + 12, opacityY + 2, COLOR_TEXT_MUTED, false);
        float curOp = WaypointManager.getEspLineOpacity();
        float[] opLevels = {0.25f, 0.50f, 0.75f, 1.00f};
        String[] opLabels = {"25%", "50%", "75%", "100%"};
        int opBtnX = x + 105;
        for (int i = 0; i < opLevels.length; i++) {
            boolean active = Math.abs(curOp - opLevels[i]) < 0.05f;
            boolean hov = mouseX >= opBtnX && mouseX <= opBtnX + 38 && mouseY >= opacityY && mouseY <= opacityY + 16;
            ModernUiRenderHelper.drawPillButton(graphics, this.font, opBtnX, opacityY, 38, 16, opLabels[i], COLOR_ACCENT, hov, active);
            opBtnX += 44;
        }

        // Kart 2: Hızlı Eylemler & Arayüz
        int actionCardY = cardY + cardH + 12;
        int actionCardH = 78;
        ModernUiRenderHelper.drawModernCard(graphics, x, actionCardY, w, actionCardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.gps.card_actions"), x + 12, actionCardY + 10, COLOR_ACCENT, false);

        // Buton 1: Harita ve Waypoint Menüsü
        int btn1Y = actionCardY + 30;
        boolean btn1Hov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= btn1Y && mouseY <= btn1Y + 20;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, btn1Y, w - 24, 20, HelpBoxLang.str("helpbox.ui.gps.open_manager"), COLOR_ACCENT, btn1Hov, false);

        // Buton 2: Rotayı İptal Et
        int btn2Y = actionCardY + 53;
        boolean btn2Hov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= btn2Y && mouseY <= btn2Y + 18;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, btn2Y, w - 24, 18, HelpBoxLang.str("helpbox.ui.gps.clear_route"), 0xFFEF4444, btn2Hov, false);

        // Alt Bilgi Notu
        int noteY = actionCardY + actionCardH + 10;
        String keyName = NavigationKeybind.openGpsScreen != null ? NavigationKeybind.openGpsScreen.getTranslatedKeyMessage().getString() : "M";
        int curNoteY = ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.gps.tip1", keyName), x + 6, noteY, w - 12, COLOR_TEXT_MUTED, 10);
        ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.gps.tip2"), x + 6, curNoteY + 2, w - 12, COLOR_TEXT_MUTED, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 1: ÖZEL ENVANTER BUTONLARI
    // ─────────────────────────────────────────────────────────────────────────────
    private void renderButtonsTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.title"), x, y + 2, COLOR_TEXT_PRIMARY, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.subtitle"), x, y + 16, COLOR_TEXT_MUTED, false);

        boolean enabled = ButtonStore.enabled;
        if (enabled) {
            ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 80, y, HelpBoxLang.str("helpbox.ui.buttons.active"), 0x3310B981, COLOR_SUCCESS);
        } else {
            ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 90, y, HelpBoxLang.str("helpbox.ui.buttons.disabled"), 0x33EF4444, 0xFFEF4444);
        }

        int cardY = y + 36;
        int cardH = 56;
        ModernUiRenderHelper.drawModernCard(graphics, x, cardY, w, cardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.card_general"), x + 12, cardY + 10, COLOR_ACCENT, false);

        // Toggle: Buton Sistemi
        int toggle1Y = cardY + 26;
        boolean t1Hov = mouseX >= x + 12 && mouseX <= x + 50 && mouseY >= toggle1Y && mouseY <= toggle1Y + 20;
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, x + 12, toggle1Y, enabled, t1Hov);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.enable_system"), x + 58, toggle1Y + 5, COLOR_TEXT_PRIMARY, false);

        // Kart 2: Yönetim & Eylemler
        int actionCardY = cardY + cardH + 12;
        int actionCardH = 82;
        ModernUiRenderHelper.drawModernCard(graphics, x, actionCardY, w, actionCardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.card_manage"), x + 12, actionCardY + 10, COLOR_ACCENT, false);

        // Buton 1: Butonları Düzenle (Primary Action)
        int btn1Y = actionCardY + 28;
        boolean btn1Hov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= btn1Y && mouseY <= btn1Y + 22;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, btn1Y, w - 24, 22, HelpBoxLang.str("helpbox.ui.buttons.open_editor"), COLOR_ACCENT, btn1Hov, false);

        // Buton 2: Varsayılanları Yükle
        int btn2Y = actionCardY + 55;
        boolean btn2Hov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= btn2Y && mouseY <= btn2Y + 18;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, btn2Y, w - 24, 18, HelpBoxLang.str("helpbox.ui.buttons.load_defaults"), 0xFFF59E0B, btn2Hov, false);

        // Alt Bilgi Notu
        int noteY = actionCardY + actionCardH + 10;
        int curNoteY = ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.buttons.tip1"), x + 6, noteY, w - 12, COLOR_TEXT_MUTED, 10);
        ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.buttons.tip2"), x + 6, curNoteY + 2, w - 12, COLOR_TEXT_MUTED, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 2: GELİŞMİŞ DEPO (ENHANCED STORAGE)
    // ─────────────────────────────────────────────────────────────────────────────
    private void renderStorageTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.storage.title"), x, y + 2, COLOR_TEXT_PRIMARY, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.storage.subtitle"), x, y + 16, COLOR_TEXT_MUTED, false);

        ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 100, y, HelpBoxLang.str("helpbox.ui.storage.badge"), 0x336366F1, 0xFF818CF8);

        int cardY = y + 36;
        int cardH = 70;
        ModernUiRenderHelper.drawModernCard(graphics, x, cardY, w, cardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.storage.card_toggles"), x + 12, cardY + 10, COLOR_ACCENT, false);

        // Switch 1: Sandık & Sırt Çantaları Deposu
        int sw1Y = cardY + 28;
        boolean s1Hov = mouseX >= x + 12 && mouseX <= x + 50 && mouseY >= sw1Y && mouseY <= sw1Y + 18;
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, x + 12, sw1Y, EnhancedStorageConfig.enableOverlay, s1Hov);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.storage.toggle_storage"), x + 58, sw1Y + 4, COLOR_TEXT_PRIMARY, false);

        // Switch 2: Rift Deposu
        int sw2Y = cardY + 48;
        boolean s2Hov = mouseX >= x + 12 && mouseX <= x + 50 && mouseY >= sw2Y && mouseY <= sw2Y + 18;
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, x + 12, sw2Y, EnhancedStorageConfig.enableRiftOverlay, s2Hov);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.storage.toggle_rift"), x + 58, sw2Y + 4, COLOR_TEXT_PRIMARY, false);

        // Kart 2: Görünüm & Konfigürasyon
        int confCardY = cardY + cardH + 10;
        int confCardH = 68;
        ModernUiRenderHelper.drawModernCard(graphics, x, confCardY, w, confCardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.storage.card_theme"), x + 12, confCardY + 10, COLOR_ACCENT, false);

        // Tema Seçicisi
        int themeY = confCardY + 28;
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.storage.theme"), x + 12, themeY + 3, COLOR_TEXT_MUTED, false);
        BackgroundType curType = EnhancedStorageConfig.backgroundType;
        int thBtnX = x + 105;
        BackgroundType[] types = {BackgroundType.TRANSPARENT, BackgroundType.DARK, BackgroundType.LIGHT};
        String[] typeNames = {
                HelpBoxLang.str("helpbox.ui.storage.theme_transparent"),
                HelpBoxLang.str("helpbox.ui.storage.theme_dark"),
                HelpBoxLang.str("helpbox.ui.storage.theme_light")
        };
        for (int i = 0; i < types.length; i++) {
            boolean active = (curType == types[i]);
            boolean hov = mouseX >= thBtnX && mouseX <= thBtnX + 50 && mouseY >= themeY && mouseY <= themeY + 16;
            ModernUiRenderHelper.drawPillButton(graphics, this.font, thBtnX, themeY, 50, 16, typeNames[i], COLOR_ACCENT, hov, active);
            thBtnX += 56;
        }

        // Kasa Ayarları Butonu
        int cfgBtnY = confCardY + 46;
        boolean cfgHov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= cfgBtnY && mouseY <= cfgBtnY + 16;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, cfgBtnY, w - 24, 16, HelpBoxLang.str("helpbox.ui.vault.settings_btn", "✦ Gelişmiş Kasa Ayarlarını Aç"), COLOR_ACCENT, cfgHov, false);

        // Alt Bilgi Notu
        int noteY = confCardY + confCardH + 10;
        int curNoteY = ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.storage.tip1"), x + 6, noteY, w - 12, COLOR_TEXT_MUTED, 10);
        ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.storage.tip2"), x + 6, curNoteY + 2, w - 12, COLOR_TEXT_MUTED, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 3: GELİŞMİŞ SACKS (ENHANCED SACKS)
    // ─────────────────────────────────────────────────────────────────────────────
    private void renderSacksTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.sacks.title"), x, y + 2, COLOR_TEXT_PRIMARY, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.sacks.subtitle"), x, y + 16, COLOR_TEXT_MUTED, false);

        boolean sackActive = SackConfig.enableSackOverlay;
        if (sackActive) {
            ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 90, y, HelpBoxLang.str("helpbox.ui.sacks.badge_active"), 0x3310B981, COLOR_SUCCESS);
        } else {
            ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 100, y, HelpBoxLang.str("helpbox.ui.sacks.badge_disabled"), 0x33EF4444, 0xFFEF4444);
        }

        int cardY = y + 36;
        int cardH = 88;
        ModernUiRenderHelper.drawModernCard(graphics, x, cardY, w, cardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.sacks.card_toggles"), x + 12, cardY + 10, COLOR_ACCENT, false);

        // Switch 1: Çuval (Sacks) Arayüzü
        int sw1Y = cardY + 28;
        boolean s1Hov = mouseX >= x + 12 && mouseX <= x + 50 && mouseY >= sw1Y && mouseY <= sw1Y + 18;
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, x + 12, sw1Y, SackConfig.enableSackOverlay, s1Hov);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.sacks.toggle_sacks"), x + 58, sw1Y + 4, COLOR_TEXT_PRIMARY, false);

        // Switch 2: Yalnızca Resmi Çuvallar
        int sw2Y = cardY + 48;
        boolean s2Hov = mouseX >= x + 12 && mouseX <= x + 50 && mouseY >= sw2Y && mouseY <= sw2Y + 18;
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, x + 12, sw2Y, SackConfig.onlyOfficialSacks, s2Hov);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.sacks.toggle_official"), x + 58, sw2Y + 4, COLOR_TEXT_PRIMARY, false);

        // Switch 3: Genel Bakış Kartı
        int sw3Y = cardY + 68;
        boolean s3Hov = mouseX >= x + 12 && mouseX <= x + 50 && mouseY >= sw3Y && mouseY <= sw3Y + 18;
        ModernUiRenderHelper.drawModernToggle(graphics, this.font, x + 12, sw3Y, SackConfig.showSackOverviewCard, s3Hov);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.sacks.toggle_overview"), x + 58, sw3Y + 4, COLOR_TEXT_PRIMARY, false);

        // Kart 2: Görünüm & Konfigürasyon
        int confCardY = cardY + cardH + 10;
        int confCardH = 68;
        ModernUiRenderHelper.drawModernCard(graphics, x, confCardY, w, confCardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.sacks.card_theme"), x + 12, confCardY + 10, COLOR_ACCENT, false);

        // Tema Seçicisi
        int themeY = confCardY + 28;
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.sacks.theme"), x + 12, themeY + 3, COLOR_TEXT_MUTED, false);
        BackgroundType curType = SackConfig.sackBackgroundType;
        int thBtnX = x + 105;
        BackgroundType[] types = {BackgroundType.TRANSPARENT, BackgroundType.DARK, BackgroundType.LIGHT};
        String[] typeNames = {
                HelpBoxLang.str("helpbox.ui.storage.theme_transparent"),
                HelpBoxLang.str("helpbox.ui.storage.theme_dark"),
                HelpBoxLang.str("helpbox.ui.storage.theme_light")
        };
        for (int i = 0; i < types.length; i++) {
            boolean active = (curType == types[i]);
            boolean hov = mouseX >= thBtnX && mouseX <= thBtnX + 50 && mouseY >= themeY && mouseY <= themeY + 16;
            ModernUiRenderHelper.drawPillButton(graphics, this.font, thBtnX, themeY, 50, 16, typeNames[i], COLOR_ACCENT, hov, active);
            thBtnX += 56;
        }

        // Midnight Config Butonu
        int cfgBtnY = confCardY + 46;
        boolean cfgHov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= cfgBtnY && mouseY <= cfgBtnY + 16;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, cfgBtnY, w - 24, 16, HelpBoxLang.str("helpbox.ui.sacks.open_midnight"), COLOR_ACCENT, cfgHov, false);

        // Alt Bilgi Notu
        int noteY = confCardY + confCardH + 10;
        int curNoteY = ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.sacks.tip1"), x + 6, noteY, w - 12, COLOR_TEXT_MUTED, 10);
        ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.sacks.tip2"), x + 6, curNoteY + 2, w - 12, COLOR_TEXT_MUTED, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 4: OYUN KONSOLU & SOHBET GEÇMİŞİ
    // ─────────────────────────────────────────────────────────────────────────────
    private void renderConsoleTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.console.title"), x, y + 2, COLOR_TEXT_PRIMARY, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.console.subtitle"), x, y + 16, COLOR_TEXT_MUTED, false);

        int logCount = ConsoleHistory.getEntries().size();
        ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 100, y, HelpBoxLang.str("helpbox.ui.console.messages_badge", logCount), 0x3310B981, COLOR_SUCCESS);

        int cardY = y + 36;
        int cardH = 80;
        ModernUiRenderHelper.drawModernCard(graphics, x, cardY, w, cardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.console.card_status"), x + 12, cardY + 10, COLOR_ACCENT, false);

        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.console.shortcut_info"), x + 12, cardY + 30, COLOR_TEXT_PRIMARY, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.console.total_messages") + " " + logCount, x + 12, cardY + 46, COLOR_TEXT_MUTED, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.console.memory_status"), x + 12, cardY + 60, COLOR_SUCCESS, false);

        // Kart 2: Hızlı Eylemler
        int actionCardY = cardY + cardH + 12;
        int actionCardH = 74;
        ModernUiRenderHelper.drawModernCard(graphics, x, actionCardY, w, actionCardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.console.card_actions"), x + 12, actionCardY + 10, COLOR_ACCENT, false);

        int btn1Y = actionCardY + 26;
        boolean btn1Hov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= btn1Y && mouseY <= btn1Y + 20;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, btn1Y, w - 24, 20, HelpBoxLang.str("helpbox.ui.console.open_now"), COLOR_ACCENT, btn1Hov, false);

        int btn2Y = actionCardY + 50;
        boolean btn2Hov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= btn2Y && mouseY <= btn2Y + 18;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, btn2Y, w - 24, 18, HelpBoxLang.str("helpbox.ui.console.clear_history"), 0xFFEF4444, btn2Hov, false);

        // Alt Bilgi Notu
        int noteY = actionCardY + actionCardH + 10;
        int curNoteY = ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.console.tip1"), x + 6, noteY, w - 12, COLOR_TEXT_MUTED, 10);
        ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.console.tip2"), x + 6, curNoteY + 2, w - 12, COLOR_TEXT_MUTED, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TAB 5: HIZLI KOPYALAMA ARAÇLARI
    // ─────────────────────────────────────────────────────────────────────────────
    private void renderCopierTab(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.copier.title"), x, y + 2, COLOR_TEXT_PRIMARY, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.copier.subtitle"), x, y + 16, COLOR_TEXT_MUTED, false);

        ModernUiRenderHelper.drawBadge(graphics, this.font, x + w - 80, y, HelpBoxLang.str("helpbox.ui.copier.badge_ready"), 0x3310B981, COLOR_SUCCESS);

        int cardY = y + 36;
        int cardH = 88;
        ModernUiRenderHelper.drawModernCard(graphics, x, cardY, w, cardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.copier.card_shortcuts"), x + 12, cardY + 10, COLOR_ACCENT, false);

        int btnW = 96;
        int btnH = 20;
        int btnX = x + w - 16 - btnW - 26;
        int unbindX = x + w - 16 - 22;

        // Row 1: Copy Item Name
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.copier.copy_name_key"), x + 12, cardY + 32, COLOR_TEXT_PRIMARY, false);
        int btn1Y = cardY + 28;
        boolean btn1Hov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btn1Y && mouseY <= btn1Y + btnH;
        String nameLabel = (listeningKeybindType == 1)
                ? HelpBoxLang.str("helpbox.ui.copier.press_key", "> Tuşa Basın <")
                : (HelpBoxConfig.copyItemName != null ? HelpBoxConfig.copyItemName.getTranslatedKeyMessage().getString() : HelpBoxLang.str("helpbox.ui.copier.none"));
        int nameColor = (listeningKeybindType == 1) ? 0xFFF59E0B : COLOR_ACCENT;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, btnX, btn1Y, btnW, btnH, nameLabel, nameColor, btn1Hov, listeningKeybindType == 1);

        boolean un1Hov = mouseX >= unbindX && mouseX <= unbindX + 22 && mouseY >= btn1Y && mouseY <= btn1Y + btnH;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, unbindX, btn1Y, 22, btnH, "✕", COLOR_DANGER, un1Hov, false);

        // Row 2: Copy Skull Key
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.copier.copy_skull_key"), x + 12, cardY + 58, COLOR_TEXT_PRIMARY, false);
        int btn2Y = cardY + 54;
        boolean btn2Hov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btn2Y && mouseY <= btn2Y + btnH;
        String skullLabel = (listeningKeybindType == 2)
                ? HelpBoxLang.str("helpbox.ui.copier.press_key", "> Tuşa Basın <")
                : (HelpBoxConfig.copyItemSkull != null ? HelpBoxConfig.copyItemSkull.getTranslatedKeyMessage().getString() : HelpBoxLang.str("helpbox.ui.copier.none"));
        int skullColor = (listeningKeybindType == 2) ? 0xFFF59E0B : COLOR_SUCCESS;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, btnX, btn2Y, btnW, btnH, skullLabel, skullColor, btn2Hov, listeningKeybindType == 2);

        boolean un2Hov = mouseX >= unbindX && mouseX <= unbindX + 22 && mouseY >= btn2Y && mouseY <= btn2Y + btnH;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, unbindX, btn2Y, 22, btnH, "✕", COLOR_DANGER, un2Hov, false);

        // Kart 2: Tuş Atamaları & Kontrol Paneli
        int actionCardY = cardY + cardH + 12;
        int actionCardH = 74;
        ModernUiRenderHelper.drawModernCard(graphics, x, actionCardY, w, actionCardH, 10, COLOR_CARD_BG, COLOR_CARD_BORDER);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.copier.card_edit"), x + 12, actionCardY + 10, COLOR_ACCENT, false);

        int optBtnY = actionCardY + 32;
        boolean optBtnHov = mouseX >= x + 12 && mouseX <= x + w - 12 && mouseY >= optBtnY && mouseY <= optBtnY + 24;
        ModernUiRenderHelper.drawPillButton(graphics, this.font, x + 12, optBtnY, w - 24, 24, HelpBoxLang.str("helpbox.ui.copier.open_keybinds"), COLOR_ACCENT, optBtnHov, false);

        // Alt Bilgi Notu
        int noteY = actionCardY + actionCardH + 10;
        int curNoteY = ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.copier.tip1"), x + 6, noteY, w - 12, COLOR_TEXT_MUTED, 10);
        ModernUiRenderHelper.drawWrappedText(graphics, this.font, HelpBoxLang.str("helpbox.ui.copier.tip2"), x + 6, curNoteY + 2, w - 12, COLOR_TEXT_MUTED, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TIKLAMA VE ETKİLEŞİM YÖNETİMİ
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (listeningKeybindType != 0 && event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(event.button());
            if (this.minecraft != null) {
                if (listeningKeybindType == 1 && HelpBoxConfig.copyItemName != null) {
                    HelpBoxConfig.copyItemName.setKey(mouseKey);
                } else if (listeningKeybindType == 2 && HelpBoxConfig.copyItemSkull != null) {
                    HelpBoxConfig.copyItemSkull.setKey(mouseKey);
                }
                KeyMapping.resetMapping();
                this.minecraft.options.save();
            }
            listeningKeybindType = 0;
            playClickSound();
            return true;
        }

        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, handled);

        double mx = event.x();
        double my = event.y();

        int dialogW = Math.min(this.width - 30, 580);
        int dialogH = Math.min(this.height - 30, 360);
        int startX = (this.width - dialogW) / 2;
        int startY = (this.height - dialogH) / 2;
        int sidebarW = 145;

        // 1. Sidebar Sekme Tıklamaları
        int tabStartY = calculateTabStartY(startY, sidebarW);
        int tabH = 26;
        int tabGap = 4;
        for (int i = 0; i < TAB_ICONS.length; i++) {
            int ty = tabStartY + i * (tabH + tabGap);
            if (mx >= startX + 10 && mx <= startX + sidebarW - 6 && my >= ty && my <= ty + tabH) {
                if (activeTab != i) {
                    activeTab = i;
                    listeningKeybindType = 0;
                    playClickSound();
                }
                return true;
            }
        }

        // Kapat Butonu Tıklaması
        int closeBtnY = startY + dialogH - 32;
        if (mx >= startX + 10 && mx <= startX + sidebarW - 6 && my >= closeBtnY && my <= closeBtnY + 22) {
            playClickSound();
            this.onClose();
            return true;
        }

        // 2. Sağ İçerik Alanı Tıklamaları
        int contentX = startX + sidebarW + 16;
        int contentY = startY + 16;
        int contentW = dialogW - sidebarW - 32;

        if (activeTab == 0) { // GPS TAB
            int cardY = contentY + 36;
            int toggleY = cardY + 28;
            // Auto Clear Toggle
            if (mx >= contentX + 12 && mx <= contentX + 50 && my >= toggleY && my <= toggleY + 20) {
                WaypointManager.setAutoClearEnabled(!WaypointManager.isAutoClearEnabled());
                playClickSound();
                return true;
            }
            // Opacity Buttons
            int opacityY = cardY + 54;
            float[] opLevels = {0.25f, 0.50f, 0.75f, 1.00f};
            int opBtnX = contentX + 105;
            for (int i = 0; i < opLevels.length; i++) {
                if (mx >= opBtnX && mx <= opBtnX + 38 && my >= opacityY && my <= opacityY + 16) {
                    WaypointManager.setEspLineOpacity(opLevels[i]);
                    playClickSound();
                    return true;
                }
                opBtnX += 44;
            }

            int actionCardY = cardY + 76 + 12;
            int btn1Y = actionCardY + 30;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= btn1Y && my <= btn1Y + 20) {
                playClickSound();
                this.minecraft.setScreenAndShow(new NavigationScreen());
                return true;
            }
            int btn2Y = actionCardY + 53;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= btn2Y && my <= btn2Y + 18) {
                playClickSound();
                WaypointManager.clearActiveWaypoint();
                return true;
            }
        } else if (activeTab == 1) { // BUTTONS TAB
            int cardY = contentY + 36;
            int toggle1Y = cardY + 26;
            // Enable toggle
            if (mx >= contentX + 12 && mx <= contentX + 50 && my >= toggle1Y && my <= toggle1Y + 20) {
                ButtonStore.enabled = !ButtonStore.enabled;
                ButtonStore.save();
                playClickSound();
                return true;
            }

            int actionCardY = cardY + 56 + 12;
            int btn1Y = actionCardY + 28;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= btn1Y && my <= btn1Y + 22) {
                playClickSound();
                ButtonStore.editMode = true;
                ButtonStore.save();
                this.onClose();
                if (this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.translatable("message.helpbox.edit_mode_enabled"));
                    this.minecraft.setScreenAndShow(new InventoryScreen(this.minecraft.player));
                }
                return true;
            }
            int btn2Y = actionCardY + 55;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= btn2Y && my <= btn2Y + 18) {
                ButtonStore.loadDefaults();
                ButtonStore.save();
                playClickSound();
                return true;
            }
        } else if (activeTab == 2) { // STORAGE TAB
            int cardY = contentY + 36;
            int sw1Y = cardY + 28;
            if (mx >= contentX + 12 && mx <= contentX + 50 && my >= sw1Y && my <= sw1Y + 18) {
                EnhancedStorageConfig.enableOverlay = !EnhancedStorageConfig.enableOverlay;
                MidnightConfig.write(StorageInitializer.MOD_ID);
                playClickSound();
                return true;
            }
            int sw2Y = cardY + 48;
            if (mx >= contentX + 12 && mx <= contentX + 50 && my >= sw2Y && my <= sw2Y + 18) {
                EnhancedStorageConfig.enableRiftOverlay = !EnhancedStorageConfig.enableRiftOverlay;
                MidnightConfig.write(StorageInitializer.MOD_ID);
                playClickSound();
                return true;
            }

            int confCardY = cardY + 70 + 10;
            int themeY = confCardY + 28;
            int thBtnX = contentX + 105;
            BackgroundType[] types = {BackgroundType.TRANSPARENT, BackgroundType.DARK, BackgroundType.LIGHT};
            for (int i = 0; i < types.length; i++) {
                if (mx >= thBtnX && mx <= thBtnX + 50 && my >= themeY && my <= themeY + 16) {
                    EnhancedStorageConfig.backgroundType = types[i];
                    MidnightConfig.write(StorageInitializer.MOD_ID);
                    playClickSound();
                    return true;
                }
                thBtnX += 56;
            }

            int cfgBtnY = confCardY + 46;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= cfgBtnY && my <= cfgBtnY + 16) {
                playClickSound();
                this.minecraft.setScreenAndShow(new ModernStorageSettingsScreen(this));
                return true;
            }
        } else if (activeTab == 3) { // SACKS TAB
            int cardY = contentY + 36;
            int sw1Y = cardY + 28;
            if (mx >= contentX + 12 && mx <= contentX + 50 && my >= sw1Y && my <= sw1Y + 18) {
                SackConfig.enableSackOverlay = !SackConfig.enableSackOverlay;
                MidnightConfig.write(StorageInitializer.SACK_MOD_ID);
                playClickSound();
                return true;
            }
            int sw2Y = cardY + 48;
            if (mx >= contentX + 12 && mx <= contentX + 50 && my >= sw2Y && my <= sw2Y + 18) {
                SackConfig.onlyOfficialSacks = !SackConfig.onlyOfficialSacks;
                MidnightConfig.write(StorageInitializer.SACK_MOD_ID);
                playClickSound();
                return true;
            }
            int sw3Y = cardY + 68;
            if (mx >= contentX + 12 && mx <= contentX + 50 && my >= sw3Y && my <= sw3Y + 18) {
                SackConfig.showSackOverviewCard = !SackConfig.showSackOverviewCard;
                MidnightConfig.write(StorageInitializer.SACK_MOD_ID);
                playClickSound();
                return true;
            }

            int confCardY = cardY + 88 + 10;
            int themeY = confCardY + 28;
            int thBtnX = contentX + 105;
            BackgroundType[] types = {BackgroundType.TRANSPARENT, BackgroundType.DARK, BackgroundType.LIGHT};
            for (int i = 0; i < types.length; i++) {
                if (mx >= thBtnX && mx <= thBtnX + 50 && my >= themeY && my <= themeY + 16) {
                    SackConfig.sackBackgroundType = types[i];
                    MidnightConfig.write(StorageInitializer.SACK_MOD_ID);
                    playClickSound();
                    return true;
                }
                thBtnX += 56;
            }

            int cfgBtnY = confCardY + 46;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= cfgBtnY && my <= cfgBtnY + 16) {
                playClickSound();
                this.minecraft.setScreenAndShow(new ModernSackSettingsScreen(this));
                return true;
            }
        } else if (activeTab == 4) { // CONSOLE TAB
            int cardY = contentY + 36;
            int actionCardY = cardY + 80 + 12;
            int btn1Y = actionCardY + 26;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= btn1Y && my <= btn1Y + 20) {
                playClickSound();
                this.minecraft.setScreenAndShow(new ConsoleScreen());
                return true;
            }
            int btn2Y = actionCardY + 50;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= btn2Y && my <= btn2Y + 18) {
                ConsoleHistory.clear();
                playClickSound();
                return true;
            }
        } else if (activeTab == 5) { // COPIER TAB
            int cardY = contentY + 36;
            int btnW = 96;
            int btnH = 20;
            int btnX = contentX + contentW - 16 - btnW - 26;
            int unbindX = contentX + contentW - 16 - 22;

            // Click copy name keybind button
            int btn1Y = cardY + 28;
            if (mx >= btnX && mx <= btnX + btnW && my >= btn1Y && my <= btn1Y + btnH) {
                listeningKeybindType = (listeningKeybindType == 1) ? 0 : 1;
                playClickSound();
                return true;
            }
            // Click unbind name
            if (mx >= unbindX && mx <= unbindX + 22 && my >= btn1Y && my <= btn1Y + btnH) {
                if (HelpBoxConfig.copyItemName != null && this.minecraft != null) {
                    HelpBoxConfig.copyItemName.setKey(InputConstants.UNKNOWN);
                    KeyMapping.resetMapping();
                    this.minecraft.options.save();
                }
                listeningKeybindType = 0;
                playClickSound();
                return true;
            }

            // Click copy skull keybind button
            int btn2Y = cardY + 54;
            if (mx >= btnX && mx <= btnX + btnW && my >= btn2Y && my <= btn2Y + btnH) {
                listeningKeybindType = (listeningKeybindType == 2) ? 0 : 2;
                playClickSound();
                return true;
            }
            // Click unbind skull
            if (mx >= unbindX && mx <= unbindX + 22 && my >= btn2Y && my <= btn2Y + btnH) {
                if (HelpBoxConfig.copyItemSkull != null && this.minecraft != null) {
                    HelpBoxConfig.copyItemSkull.setKey(InputConstants.UNKNOWN);
                    KeyMapping.resetMapping();
                    this.minecraft.options.save();
                }
                listeningKeybindType = 0;
                playClickSound();
                return true;
            }

            int actionCardY = cardY + 88 + 12;
            int optBtnY = actionCardY + 32;
            if (mx >= contentX + 12 && mx <= contentX + contentW - 12 && my >= optBtnY && my <= optBtnY + 24) {
                listeningKeybindType = 0;
                playClickSound();
                this.minecraft.setScreenAndShow(new KeyBindsScreen(this, this.minecraft.options));
                return true;
            }

            if (listeningKeybindType != 0) {
                listeningKeybindType = 0;
                return true;
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningKeybindType != 0) {
            if (event.key() == InputConstants.KEY_ESCAPE || event.key() == InputConstants.KEY_DELETE) {
                if (event.key() == InputConstants.KEY_DELETE && this.minecraft != null) {
                    if (listeningKeybindType == 1 && HelpBoxConfig.copyItemName != null) {
                        HelpBoxConfig.copyItemName.setKey(InputConstants.UNKNOWN);
                    } else if (listeningKeybindType == 2 && HelpBoxConfig.copyItemSkull != null) {
                        HelpBoxConfig.copyItemSkull.setKey(InputConstants.UNKNOWN);
                    }
                    KeyMapping.resetMapping();
                    this.minecraft.options.save();
                }
                listeningKeybindType = 0;
                playClickSound();
                return true;
            }

            InputConstants.Key key = InputConstants.getKey(event);
            if (this.minecraft != null) {
                if (listeningKeybindType == 1 && HelpBoxConfig.copyItemName != null) {
                    HelpBoxConfig.copyItemName.setKey(key);
                } else if (listeningKeybindType == 2 && HelpBoxConfig.copyItemSkull != null) {
                    HelpBoxConfig.copyItemSkull.setKey(key);
                }
                KeyMapping.resetMapping();
                this.minecraft.options.save();
            }
            listeningKeybindType = 0;
            playClickSound();
            return true;
        }

        return super.keyPressed(event);
    }
}
