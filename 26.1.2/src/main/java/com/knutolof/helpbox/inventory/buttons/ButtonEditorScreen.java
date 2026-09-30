package com.knutolof.helpbox.inventory.buttons;

import com.knutolof.helpbox.HelpBoxMod;
import com.knutolof.helpbox.inventory.buttons.model.InventoryButton;
import com.knutolof.helpbox.util.HelpBoxLang;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Envanter butonlarını yönetmek için editör ekranı.
 *
 * <p>{@code /invbuttons} komutuyla veya edit modunda butona sol tıkla açılır.</p>
 * <p>Sürükle-Bırak modu envanterdeyken aktif edilir. Bu ekranda liste ve düzenleme yapılır.</p>
 */
public class ButtonEditorScreen extends Screen {

    // ── UI Sabitleri ───────────────────────────────────────────────────────────
    private static final int PANEL_BG        = 0xEE0A0A1A;
    private static final int PANEL_BORDER     = 0xFF334466;
    private static final int HEADER_BG        = 0xFF0D0D2A;
    private static final int ITEM_BG          = 0xAA111126;
    private static final int ITEM_BG_HOVER    = 0xAA223355;
    private static final int COLOR_TEXT        = 0xFFDDEEFF;
    private static final int COLOR_MUTED       = 0xFF778899;
    private static final int COLOR_ACCENT      = 0xFF6688FF;
    private static final int COLOR_SUCCESS     = 0xFF44FF88;
    private static final int COLOR_DANGER      = 0xFFFF4455;
    private static final int COLOR_WARNING     = 0xFFFFAA33;
    private static final int COLOR_ENABLED     = 0xFF44FF88;
    private static final int COLOR_DISABLED    = 0xFFFF4455;

    private static final int LIST_ITEM_H   = 32;
    private static final int SCROLL_SPEED  = 20;

    // ── Durum ─────────────────────────────────────────────────────────────────
    private double scrollY   = 0;
    private double maxScrollY = 0;

    /** null → liste modu; not-null → bu butonu düzenle */
    private InventoryButton editingButton = null;
    private boolean isNewButton = false;

    private String statusMessage = "";
    private int statusColor = COLOR_SUCCESS;
    private int statusTimer = 0;

    // ── Form Widget'ları (sadece icon + command) ───────────────────────────────
    private EditBox iconBox;
    private EditBox commandBox;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Liste modunda açar */
    public ButtonEditorScreen() {
        super(HelpBoxLang.tr("helpbox.ui.button_editor.title"));
    }

    /** Belirli bir butonu düzenleme modunda açar */
    public ButtonEditorScreen(InventoryButton btn, boolean isNew) {
        super(HelpBoxLang.tr("helpbox.ui.button_editor.title"));
        this.editingButton = btn;
        this.isNewButton = isNew;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        if (editingButton != null) {
            initEditForm();
        } else {
            initListUI();
        }
    }

    @Override
    public void onClose() {
        ButtonStore.save();
        ButtonRenderer.clearCache();
        HelpBoxMod.LOGGER.info("[HelpBox] Buton editörü kapatıldı — kaydedildi.");
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Liste Modu ─────────────────────────────────────────────────────────────

    private void initListUI() {
        int cx = this.width / 2;
        int panelW = Math.min(480, this.width - 40);
        int panelX = cx - panelW / 2;
        int panelY = 30;
        int panelH = this.height - 40;
        int listAreaH = panelH - 135; // Toolbar'a değmemesi için yüksekliği kıstık

        List<InventoryButton> buttons = ButtonStore.buttons;
        int totalH = buttons.size() * LIST_ITEM_H;
        maxScrollY = Math.max(0, totalH - listAreaH);
        scrollY = Math.min(scrollY, maxScrollY);

        int toolbarY = panelY + panelH - 90;
        int btnW3 = (panelW - 20) / 3;

        // ── Üst toolbar ──────────────────────────────────────────────────────

        // Butonları Aç/Kapat
        String enabledLabel = ButtonStore.enabled
            ? HelpBoxLang.str("helpbox.ui.button_editor.system_enabled")
            : HelpBoxLang.str("helpbox.ui.button_editor.system_disabled");
        int enabledColor = ButtonStore.enabled ? COLOR_ENABLED : COLOR_DISABLED;
        this.addRenderableWidget(
            Button.builder(Component.literal(enabledLabel), btn -> {
                ButtonStore.enabled = !ButtonStore.enabled;
                ButtonStore.save();
                this.rebuildWidgets();
            }).bounds(panelX, toolbarY, btnW3, 20).build()
        );

        // Sürükle-Bırak Modu
        String dragLabel = ButtonStore.editMode
            ? HelpBoxLang.str("helpbox.ui.button_editor.drag_on")
            : HelpBoxLang.str("helpbox.ui.button_editor.drag_off");
        this.addRenderableWidget(
            Button.builder(Component.literal(dragLabel), btn -> {
                ButtonStore.editMode = !ButtonStore.editMode;
                if (ButtonStore.editMode) {
                    setStatus(HelpBoxLang.str("helpbox.ui.button_editor.status_drag_hint"), COLOR_WARNING);
                }
                this.rebuildWidgets();
            }).bounds(panelX + btnW3 + 5, toolbarY, btnW3, 20).build()
        );

        // Config Yükle (Windows Dosya Seçici)
        this.addRenderableWidget(
            Button.builder(HelpBoxLang.tr("helpbox.ui.button_editor.load_config"), btn -> {
                new Thread(() -> {
                    try {
                        String result = openNativeFileDialog("HelpBox — Config Seç");
                        if (result != null) {
                            Path selected = Paths.get(result);
                            int imported = ButtonStore.importFromFirmament(selected);
                            Minecraft.getInstance().execute(() -> {
                                if (imported >= 0) {
                                    setStatus(HelpBoxLang.str("helpbox.ui.button_editor.status_imported", imported), COLOR_SUCCESS);
                                } else {
                                    setStatus(HelpBoxLang.str("helpbox.ui.button_editor.status_invalid_file"), COLOR_DANGER);
                                }
                                this.rebuildWidgets();
                            });
                        }
                    } catch (Exception e) {
                        HelpBoxMod.LOGGER.error("Dosya seçici açılamadı", e);
                        Minecraft.getInstance().execute(() -> {
                            setStatus(HelpBoxLang.str("helpbox.ui.button_editor.status_file_error"), COLOR_DANGER);
                        });
                    }
                }, "HelpBox-FilePicker").start();
            }).bounds(panelX + 2 * (btnW3 + 5), toolbarY, btnW3, 20).build()
        );

        // Varsayılanlar + Kapat
        this.addRenderableWidget(
            Button.builder(HelpBoxLang.tr("helpbox.ui.button_editor.load_defaults"), btn -> {
                ButtonStore.loadDefaults();
                ButtonRenderer.clearCache();
                setStatus(HelpBoxLang.str("helpbox.ui.button_editor.status_defaults_loaded", ButtonStore.buttons.size()), COLOR_WARNING);
                this.rebuildWidgets();
            }).bounds(panelX, toolbarY + 25, (panelW - 5) / 2, 20).build()
        );

        this.addRenderableWidget(
            Button.builder(HelpBoxLang.tr("helpbox.ui.button_editor.close_save"), btn -> this.onClose())
                .bounds(panelX + (panelW - 5) / 2 + 5, toolbarY + 25, (panelW - 5) / 2, 20).build()
        );

        // ── Görünüm Modu Toggle ───────────────────────────────────────────────
        String viewLabel = ButtonStore.gridViewEnabled
            ? HelpBoxLang.str("helpbox.ui.button_editor.view_grid")
            : HelpBoxLang.str("helpbox.ui.button_editor.view_list");
        this.addRenderableWidget(
            Button.builder(Component.literal(viewLabel), btn -> {
                ButtonStore.gridViewEnabled = !ButtonStore.gridViewEnabled;
                ButtonStore.save();
                this.rebuildWidgets();
            }).bounds(panelX + panelW - 90, panelY + 4, 80, 20).build()
        );

        // ── Liste öğeleri ──────────────────────────────────────────────────────
        int listStartY = panelY + 40;
        int cols = ButtonStore.gridViewEnabled ? 2 : 1;
        int itemW = (panelW - 20) / cols;
        int itemH = ButtonStore.gridViewEnabled ? 48 : LIST_ITEM_H;
        
        int calculatedTotalH = (int) Math.ceil((double) buttons.size() / cols) * itemH;
        maxScrollY = Math.max(0, calculatedTotalH - listAreaH);
        scrollY = Math.min(scrollY, maxScrollY);

        for (int i = 0; i < buttons.size(); i++) {
            InventoryButton btn = buttons.get(i);
            int row = i / cols;
            int col = i % cols;
            
            int itemX = panelX + 10 + col * itemW;
            int itemY = listStartY + row * itemH - (int) scrollY;

            if (itemY + itemH < listStartY || itemY > listStartY + listAreaH) continue;

            final int fi = i;
            final InventoryButton fbtn = btn;

            // Edit button
            int btnY = ButtonStore.gridViewEnabled ? itemY + 24 : itemY + 6;
            int btnX = itemX + itemW - 60;
            
            // Eğer widget'lar görünür listenin dışına taşıyorsa listeye ekleme
            if (btnY < listStartY || btnY + 20 > listStartY + listAreaH) continue;
            
            this.addRenderableWidget(
                Button.builder(Component.literal("✎"), b -> {
                    editingButton = fbtn;
                    isNewButton = false;
                    this.rebuildWidgets();
                }).bounds(btnX, btnY, 22, 20).build()
            );

            // Delete button
            this.addRenderableWidget(
                Button.builder(Component.literal("✗"), b -> {
                    ButtonStore.buttons.remove(fi);
                    setStatus(HelpBoxLang.str("helpbox.ui.buttons.deleted", "Buton silindi."), COLOR_DANGER);
                    this.rebuildWidgets();
                }).bounds(btnX + 26, btnY, 22, 20).build()
            );
        }
    }

    // ── Düzenleme Formu ────────────────────────────────────────────────────────

    private void initEditForm() {
        int cx = this.width / 2;
        int formW = Math.min(360, this.width - 40);
        int formX = cx - formW / 2;
        int formY = 40;

        // Sol sütun (icon önizleme hariç tüm alan)
        int previewSize = 32;
        int previewX = formX + formW - previewSize - 10;
        int fieldW = previewX - formX - 20; // ikon alanı sağa yer bırak
        int col1X = formX + 10;

        // İkon alanı (icon önizlemenin yanına sığacak kadar dar)
        iconBox = new EditBox(this.font, col1X, formY + 50, fieldW, 20, Component.literal("icon"));
        iconBox.setMaxLength(2048); // setMaxLength MUTLAKA setValue'dan ÖNCE gelmeli!
        iconBox.setHint(Component.literal("skull:hash  /  ITEM_ID  /  /give @s ..."));
        // pendingIconStr varsa (item kopyalandıysa) onu göster, yoksa editingButton.icon kullan
        String iconInitVal = (ButtonStore.pendingIconStr != null) ? ButtonStore.pendingIconStr : (editingButton.icon != null ? editingButton.icon : "");
        iconBox.setValue(iconInitVal);
        iconBox.setCursorPosition(0);
        iconBox.setHighlightPos(0);
        ButtonStore.pendingIconStr = null; // tüketildi, temizle
        iconBox.setTextColor(0xFFDDEEFF); // Normal beyaz — kırmızı değil
        this.addRenderableWidget(iconBox);

        // Parse /give & Komut / URL / Base64 / Hash butonu — ikon alanının hemen altında
        this.addRenderableWidget(
            Button.builder(HelpBoxLang.tr("helpbox.ui.buttons.parse", "Parse / Ayrıştır"), b -> {
                String val = iconBox.getValue().trim();
                String normalized = ButtonRenderer.normalizeIconInput(val);
                if (!normalized.isEmpty() && !normalized.equalsIgnoreCase(val)) {
                    iconBox.setValue(normalized);
                    setStatus(HelpBoxLang.str("helpbox.ui.buttons.parsed", "✓ Ayrıştırıldı: %s", (normalized.length() > 25 ? normalized.substring(0, 22) + "..." : normalized)), COLOR_SUCCESS);
                } else {
                    setStatus(HelpBoxLang.str("helpbox.ui.buttons.icon_ready", "✓ İkon hazır!"), COLOR_SUCCESS);
                }
            }).bounds(col1X, formY + 75, 110, 16).build()
        );

        // Komut alanı — tam genişlik
        commandBox = new EditBox(this.font, col1X, formY + 105, formW - 20, 20, Component.literal("command"));
        commandBox.setHint(Component.literal("warp hub  /  storage  /  pets"));
        commandBox.setValue(editingButton.command != null ? editingButton.command : "");
        commandBox.setMaxLength(128);
        commandBox.setTextColor(0xFFDDEEFF); // Normal beyaz — kırmızı değil
        this.addRenderableWidget(commandBox);

        // İsGigantic toggle
        boolean bigState = editingButton.isGigantic;
        this.addRenderableWidget(
            Button.builder(
                HelpBoxLang.tr("helpbox.ui.buttons.size_toggle", "Boyut: %s", (bigState ? HelpBoxLang.str("helpbox.ui.buttons.size_big", "32×32 (Büyük)") : HelpBoxLang.str("helpbox.ui.buttons.size_normal", "16×16 (Normal)"))),
                b -> {
                    editingButton.isGigantic = !editingButton.isGigantic;
                    this.rebuildWidgets();
                }
            ).bounds(col1X, formY + 140, formW - 20, 18).build()
        );

        // Kaydet / İptal
        int halfW = (formW - 25) / 2;
        this.addRenderableWidget(
            Button.builder(HelpBoxLang.tr("helpbox.ui.buttons.save", "✓ Kaydet"), b -> saveEdit())
                .bounds(col1X, formY + 175, halfW, 20).build()
        );
        this.addRenderableWidget(
            Button.builder(HelpBoxLang.tr("helpbox.ui.buttons.cancel", "✗ İptal"), b -> {
                if (isNewButton) {
                    // Yeni buton eklenmemişti, sadece iptal
                }
                editingButton = null;
                isNewButton = false;
                this.rebuildWidgets();
            }).bounds(col1X + halfW + 5, formY + 175, halfW, 20).build()
        );
    }

    private void saveEdit() {
        if (editingButton == null) return;

        String icon    = iconBox.getValue().trim();
        String command = commandBox.getValue().trim();

        if (icon.isEmpty() || command.isEmpty()) {
            setStatus(HelpBoxLang.str("helpbox.ui.buttons.err_empty", "✗ İkon ve komut boş bırakılamaz."), COLOR_DANGER);
            return;
        }

        // Otomatik ikon normalizasyonu (/give, /setblock, /summon, URL, base64, hash vb.)
        icon = ButtonRenderer.normalizeIconInput(icon);

        editingButton.icon    = icon;
        editingButton.command = command.startsWith("/") ? command.substring(1) : command;

        if (isNewButton) {
            editingButton.id = UUID.randomUUID().toString();
            ButtonStore.buttons.add(editingButton);
        }

        ButtonRenderer.clearCache();
        ButtonStore.save();
        setStatus(HelpBoxLang.str("helpbox.ui.buttons.saved", "✓ Kaydedildi."), COLOR_SUCCESS);
        editingButton = null;
        isNewButton = false;
        this.rebuildWidgets();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        // Koyu arka plan overlay
        graphics.fill(0, 0, this.width, this.height, 0xCC000000);

        if (editingButton != null) {
            renderEditForm(graphics, mouseX, mouseY);
        } else {
            renderList(graphics, mouseX, mouseY);
        }

        // Durum mesajı
        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            statusTimer--;
            int msgW = this.font.width(statusMessage);
            int msgX = this.width / 2 - msgW / 2;
            graphics.fill(msgX - 4, this.height - 18, msgX + msgW + 4, this.height - 4, 0xDD000000);
            graphics.text(this.font, statusMessage, msgX, this.height - 16, statusColor, false);
        }
    }

    private void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int cx = this.width / 2;
        int panelW = Math.min(480, this.width - 40);
        int panelX = cx - panelW / 2;
        int panelY = 30;
        int panelH = this.height - 40;
        int listAreaH = panelH - 135;

        // Panel arka planı
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_BORDER);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);

        // Başlık
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 28, HEADER_BG);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.title", "⚡ Inventory Buttons"), panelX + 10, panelY + 6, COLOR_ACCENT, false);

        // Enabled durumu
        String statusLabel = ButtonStore.enabled ? HelpBoxLang.str("helpbox.ui.buttons.on", "● AÇIK") : HelpBoxLang.str("helpbox.ui.buttons.off", "○ KAPALI");
        int statusCol = ButtonStore.enabled ? COLOR_ENABLED : COLOR_DISABLED;
        graphics.text(this.font, statusLabel,
            panelX + panelW - this.font.width(statusLabel) - 10, panelY + 6, statusCol, false);

        graphics.fill(panelX, panelY + 28, panelX + panelW, panelY + 29, PANEL_BORDER);

        // Alt ayırıcı
        graphics.fill(panelX, panelY + panelH - 95, panelX + panelW, panelY + panelH - 94, PANEL_BORDER);

        // İpucu
        String tipText = ButtonStore.editMode
            ? HelpBoxLang.str("helpbox.ui.buttons.tip_edit_on", "■ Düzenleme AÇIK: Envanterde sağ tık=yeni, sol tık=düzenle, sürükle=taşı")
            : HelpBoxLang.str("helpbox.ui.buttons.tip_edit_off", "☆ Düzenleme modu kapalı. Açmak için \"Düzenleme\" butonuna basın.");
        graphics.text(this.font, tipText, panelX + 10, panelY + panelH - 30, COLOR_MUTED, false);

        // Liste
        int listStartY = panelY + 40;
        int listEndY   = listStartY + listAreaH;

        graphics.enableScissor(panelX, listStartY, panelX + panelW, listEndY);

        int cols = ButtonStore.gridViewEnabled ? 2 : 1;
        int itemW = (panelW - 20) / cols;
        int itemH = ButtonStore.gridViewEnabled ? 48 : LIST_ITEM_H;

        List<InventoryButton> buttons = ButtonStore.buttons;
        for (int i = 0; i < buttons.size(); i++) {
            InventoryButton btn = buttons.get(i);
            int row = i / cols;
            int col = i % cols;
            
            int itemX = panelX + 10 + col * itemW;
            int itemY = listStartY + row * itemH - (int) scrollY;

            if (itemY + itemH < listStartY || itemY > listEndY) continue;

            boolean hovered = mouseX >= itemX && mouseX < itemX + itemW - 4
                           && mouseY >= itemY && mouseY < itemY + itemH - 2;

            // Satır/Hücre arka planı
            graphics.fill(itemX + 2, itemY, itemX + itemW - 2, itemY + itemH - 2,
                hovered ? ITEM_BG_HOVER : ITEM_BG);

            // Sol accent şerit
            graphics.fill(itemX + 2, itemY, itemX + 4, itemY + itemH - 2, COLOR_ACCENT);

            // İtem önizleme
            ItemStack stack = ButtonRenderer.resolveIcon(btn.icon);
            if (stack != null && !stack.isEmpty()) {
                try { graphics.item(stack, itemX + 8, itemY + (itemH / 2) - 8); } catch (Exception ignored) {}
            }

            // Metin
            String displayCmd = "/" + (btn.command != null ? btn.command : "?");
            graphics.text(this.font, displayCmd, itemX + 28, itemY + 4, COLOR_TEXT, false);

            // İkon string (kısaltılmış)
            String iconPreview = btn.icon != null && btn.icon.length() > 22
                ? btn.icon.substring(0, 20) + "…"
                : (btn.icon != null ? btn.icon : "?");
            graphics.text(this.font, iconPreview, itemX + 28, itemY + 16, 0xFF556677, false);

            // Ayırıcı çizgi (sadece liste modunda)
            if (!ButtonStore.gridViewEnabled && i < buttons.size() - 1) {
                graphics.fill(panelX + 4, itemY + LIST_ITEM_H - 2,
                    panelX + panelW - 4, itemY + LIST_ITEM_H - 1, 0xFF1A1A33);
            }
        }

        graphics.disableScissor();

        // Boş liste mesajı
        if (buttons.isEmpty()) {
            String msg = HelpBoxLang.str("helpbox.ui.buttons.empty", "Henüz buton yok — Düzenleme modunu açıp envanterde sağ tıklayın.");
            graphics.centeredText(this.font, msg, cx, listStartY + listAreaH / 2, COLOR_MUTED);
        }

        // Scroll göstergesi
        if (maxScrollY > 0) {
            double scrollRatio = scrollY / maxScrollY;
            int barH = Math.max(20, (int)((double) listAreaH / (buttons.size() * LIST_ITEM_H) * listAreaH));
            int barY = listStartY + (int)((listAreaH - barH) * scrollRatio);
            graphics.fill(panelX + panelW - 4, listStartY, panelX + panelW - 2, listEndY, 0xFF1A1A2A);
            graphics.fill(panelX + panelW - 4, barY, panelX + panelW - 2, barY + barH, COLOR_ACCENT);
        }
    }

    private void renderEditForm(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int cx = this.width / 2;
        int formW = Math.min(360, this.width - 40);
        int formX = cx - formW / 2;
        int formY = 40;
        int formH = 230;

        // Panel
        graphics.fill(formX - 1, formY - 1, formX + formW + 1, formY + formH + 1, PANEL_BORDER);
        graphics.fill(formX, formY, formX + formW, formY + formH, PANEL_BG);

        // Başlık
        graphics.fill(formX, formY, formX + formW, formY + 28, HEADER_BG);
        String title = isNewButton ? HelpBoxLang.str("helpbox.ui.buttons.new_btn", "⊕ Yeni Buton Ekle") : HelpBoxLang.str("helpbox.ui.buttons.edit_btn", "✎ Buton Düzenle");
        graphics.text(this.font, title, formX + 10, formY + 6, COLOR_ACCENT, false);
        graphics.fill(formX, formY + 28, formX + formW, formY + 29, PANEL_BORDER);

        int col1X = formX + 10;
        int previewSize = 32;
        int previewX = formX + formW - previewSize - 10;

        // Alan etiketleri
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.icon_id", "İkon ID:"), col1X, formY + 40, COLOR_MUTED, false);
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.command_label", "Komut (/ prefix opsiyonel):"), col1X, formY + 95, COLOR_MUTED, false);

        // İkon önizleme — sağ üst köşe, ikon alanının tam yanında
        String iconVal = iconBox != null ? iconBox.getValue() : (editingButton.icon != null ? editingButton.icon : "");
        String previewIconStr = iconVal;
        if (iconVal.startsWith("/give") || iconVal.startsWith("give")) {
            String p = ButtonRenderer.extractItemIdFromGive(iconVal);
            if (p != null) previewIconStr = p;
        }

        int prevX = previewX;
        int prevY = formY + 38;
        graphics.fill(prevX - 2, prevY - 2, prevX + previewSize + 2, prevY + previewSize + 2, 0xFF223344);
        graphics.fill(prevX, prevY, prevX + previewSize, prevY + previewSize, 0xFF111122);

        if (!previewIconStr.isBlank()) {
            ItemStack previewStack = ButtonRenderer.resolveIcon(previewIconStr);
            if (previewStack != null && !previewStack.isEmpty()) {
                try { graphics.item(previewStack, prevX + 8, prevY + 8); } catch (Exception ignored) {}
            } else {
                graphics.centeredText(this.font, "?", prevX + previewSize / 2, prevY + previewSize / 2 - 4, COLOR_MUTED);
            }
        } else {
            graphics.centeredText(this.font, "?", prevX + previewSize / 2, prevY + previewSize / 2 - 4, COLOR_MUTED);
        }

        // Boyut etiketi
        graphics.text(this.font, HelpBoxLang.str("helpbox.ui.buttons.size_label", "Boyut:"), col1X, formY + 132, COLOR_MUTED, false);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (editingButton != null) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        this.scrollY -= scrollY * SCROLL_SPEED;
        this.scrollY = Math.max(0, Math.min(this.scrollY, maxScrollY));
        this.rebuildWidgets();
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == 256) { // Esc
            if (editingButton != null) {
                editingButton = null;
                isNewButton = false;
                this.rebuildWidgets();
                return true;
            }
            this.onClose();
            return true;
        }
        if (keyCode == 257 && editingButton != null) { // Enter
            saveEdit();
            return true;
        }
        return super.keyPressed(event);
    }

    // ── Yardımcı ──────────────────────────────────────────────────────────────

    private void setStatus(String msg, int color) {
        statusMessage = msg;
        statusColor   = color;
        statusTimer   = 140;
    }

    private static String openNativeFileDialog(String title) {
        // 1. TinyFileDialogs dene (reflection ile)
        try {
            Class<?> clazz = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
            for (java.lang.reflect.Method m : clazz.getMethods()) {
                if (m.getName().equals("tinyfd_openFileDialog") && m.getParameterCount() == 5) {
                    Object res = m.invoke(null, title, null, null, "JSON Dosyaları (*.json)", false);
                    if (res instanceof String s) return s;
                }
            }
        } catch (Throwable ignored) {}

        // 2. Native AWT FileDialog dene
        try {
            System.setProperty("java.awt.headless", "false");
            java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null, title, java.awt.FileDialog.LOAD);
            dialog.setFile("*.json");
            dialog.setVisible(true);
            if (dialog.getFile() != null) {
                return new java.io.File(dialog.getDirectory(), dialog.getFile()).getAbsolutePath();
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
