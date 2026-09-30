package com.knutolof.helpbox.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.knutolof.helpbox.inventory.buttons.ButtonRenderer;
import com.knutolof.helpbox.inventory.buttons.ButtonStore;
import com.knutolof.helpbox.inventory.buttons.PopupEditor;
import com.knutolof.helpbox.inventory.buttons.model.InventoryButton;
import com.knutolof.helpbox.ui.ModernUiRenderHelper;
import com.knutolof.helpbox.util.HelpBoxLang;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinContainerScreen {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Shadow @org.jetbrains.annotations.Nullable protected net.minecraft.world.inventory.Slot hoveredSlot;

    @Unique private InventoryButton helpbox_draggedButton = null;
    @Unique private InventoryButton helpbox_mouseDownButton = null;
    @Unique private double helpbox_mouseDownX = 0;
    @Unique private double helpbox_mouseDownY = 0;
    @Unique private int helpbox_dragStartOffsetX = 0;
    @Unique private int helpbox_dragStartOffsetY = 0;
    @Unique private boolean helpbox_wasDragged = false;

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void helpbox_renderButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ButtonStore.editMode && !PopupEditor.isOpen) {
            graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xAA000000);
        }

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (!ButtonStore.buttons.isEmpty() && !helpbox_hasEnhancedStorageOverlay()) {
            ButtonRenderer.render(graphics, sw, sh,
                this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                mouseX, mouseY);
        }
        if (PopupEditor.isOpen) {
            PopupEditor.render(graphics, mouseX, mouseY, partialTick);
        }

        if (ButtonStore.editMode && !PopupEditor.isOpen) {
            // Sol Üst Bilgi Kartı (Yumuşak yuvarlak hatlar)
            int hudX = 10;
            int hudY = 10;
            int hudW = 185;
            int hudH = 74;
            ModernUiRenderHelper.drawModernCard(graphics, hudX, hudY, hudW, hudH, 8, 0xDD0F172A, 0x6638BDF8);
            graphics.text(mc.font, HelpBoxLang.str("helpbox.ui.buttons.hud_title", "\u2726 Buton D\u00fczenleme Modu"), hudX + 10, hudY + 8, 0xFF38BDF8, false);
            graphics.text(mc.font, HelpBoxLang.str("helpbox.ui.buttons.hud_drag", "\u2022 Sol T\u0131k & S\u00fcr\u00fckle: Butonu ta\u015f\u0131"), hudX + 10, hudY + 22, 0xFFE2E8F0, false);
            graphics.text(mc.font, HelpBoxLang.str("helpbox.ui.buttons.hud_edit", "\u2022 Sol T\u0131k: Buton ayarlar\u0131 / Sil"), hudX + 10, hudY + 34, 0xFFCBD5E1, false);
            graphics.text(mc.font, HelpBoxLang.str("helpbox.ui.buttons.hud_new", "\u2022 Sa\u011f T\u0131k (Bo\u015f Alan): Yeni buton"), hudX + 10, hudY + 46, 0xFFCBD5E1, false);
            graphics.text(mc.font, HelpBoxLang.str("helpbox.ui.buttons.hud_exit", "\u2022 ESC veya 'Bitti': Kaydet & \u00e7\u0131k"), hudX + 10, hudY + 58, 0xFF94A3B8, false);

            if (ButtonStore.pendingMacroName != null) {
                String macroMsg = HelpBoxLang.str("helpbox.ui.buttons.macro_hint", "Makro butonu yerle\u015ftirmek i\u00e7in bo\u015f bir yere t\u0131klay\u0131n. (\u0130ptal: ESC)");
                int mw = mc.font.width(macroMsg) + 16;
                int mx = sw / 2 - mw / 2;
                ModernUiRenderHelper.drawModernCard(graphics, mx, 8, mw, 20, 6, 0xEE0F172A, 0x88F59E0B);
                graphics.text(mc.font, macroMsg, mx + 8, 14, 0xFFF59E0B, false);
            } else {
                int btnW = 100;
                int btnH = 22;
                int btnX = sw / 2 - btnW / 2;
                int btnY = sh - 30;
                boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
                ModernUiRenderHelper.drawPillButton(graphics, mc.font, btnX, btnY, btnW, btnH, HelpBoxLang.str("helpbox.ui.buttons.btn_done", "\u2713 Bitti"), 0xFF10B981, hovered, false);
            }
        }
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void helpbox_onExtractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (ButtonStore.editMode) {
            ci.cancel();
        }
    }

    @Unique
    private boolean helpbox_hasEnhancedStorageOverlay() {
        Object self = (Object) this;
        try {
            Class<?> overlayHolder = Class.forName("com.knutolof.helpbox.storage.OverlayHolder");
            if (overlayHolder.isInstance(self)) {
                java.lang.reflect.Method m = overlayHolder.getMethod("es$hasOverlay");
                return Boolean.TRUE.equals(m.invoke(self));
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void helpbox_onMouseClicked(MouseButtonEvent event, boolean handled, CallbackInfoReturnable<Boolean> cir) {
        if (!ButtonStore.enabled) return;
        if (helpbox_hasEnhancedStorageOverlay()) return;

        if (PopupEditor.isOpen) {
            PopupEditor.mouseClicked(event, handled);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (ButtonStore.editMode) {
            double mx = event.x();
            double my = event.y();
            int button = event.button();

            if (ButtonStore.pendingMacroName != null && button == InputConstants.MOUSE_BUTTON_LEFT) {
                InventoryButton newBtn = new InventoryButton();
                helpbox_setAnchorFromScreenPos(newBtn, (int) mx, (int) my);
                newBtn.command = "@macro:" + ButtonStore.pendingMacroName;
                newBtn.icon = "COMMAND_BLOCK";
                ButtonStore.buttons.add(newBtn);
                ButtonStore.save();
                ButtonStore.pendingMacroName = null;
                // Keep editMode = true so they can drag it
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }

            if (ButtonStore.pendingMacroName == null && button == InputConstants.MOUSE_BUTTON_LEFT) {
                int btnW = 100;
                int btnH = 22;
                int btnX = sw / 2 - btnW / 2;
                int btnY = sh - 30;
                if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                    ButtonStore.editMode = false;
                    ButtonStore.save();
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }
            }

            InventoryButton hitBtn = null;
            for (InventoryButton btn : ButtonStore.buttons) {
                if (!btn.isValid()) continue;
                int[] bounds = btn.getBounds(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
                int bx = bounds[0], by = bounds[1], size = btn.getSize();
                if (mx >= bx && mx < bx + size && my >= by && my < by + size) {
                    hitBtn = btn;
                    break;
                }
            }

            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                helpbox_mouseDownX = mx;
                helpbox_mouseDownY = my;
                helpbox_mouseDownButton = hitBtn;
                helpbox_draggedButton = hitBtn;
                if (hitBtn != null) {
                    int[] bounds = hitBtn.getBounds(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
                    helpbox_dragStartOffsetX = (int) mx - bounds[0];
                    helpbox_dragStartOffsetY = (int) my - bounds[1];
                }
                helpbox_wasDragged = false;
            } else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                if (hitBtn == null) {
                    InventoryButton newBtn = new InventoryButton();
                    helpbox_setAnchorFromScreenPos(newBtn, (int) mx, (int) my);
                    PopupEditor.open(newBtn, true, sw, sh);
                }
            }
            
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double mx = event.x();
            double my = event.y();
            for (InventoryButton btn : ButtonStore.buttons) {
                if (!btn.isValid()) continue;
                int[] bounds = btn.getBounds(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
                int bx = bounds[0], by = bounds[1], size = btn.getSize();
                if (mx >= bx && mx < bx + size && my >= by && my < by + size) {
                    String rawCmd = btn.command.trim();
                    if (rawCmd.toLowerCase().startsWith("@macro:")) {
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.helpbox.macro_disabled"));
                        }
                    } else {
                        String cmd = rawCmd.startsWith("/") ? rawCmd.substring(1) : rawCmd;
                        if (mc.player != null && mc.player.connection != null) mc.player.connection.sendCommand(cmd);
                    }
                    cir.setReturnValue(true);
                    cir.cancel();
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void helpbox_onMouseDragged(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (helpbox_hasEnhancedStorageOverlay()) return;

        if (PopupEditor.isOpen) {
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }
        if (!ButtonStore.editMode) return;

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double dx = event.x() - helpbox_mouseDownX;
            double dy = event.y() - helpbox_mouseDownY;
            if (dx * dx + dy * dy > 9) {
                helpbox_wasDragged = true;
            }

            if (helpbox_wasDragged && helpbox_draggedButton != null) {
                int buttonScreenX = (int) event.x() - helpbox_dragStartOffsetX;
                int buttonScreenY = (int) event.y() - helpbox_dragStartOffsetY;
                int size = helpbox_draggedButton.getSize();

                // Duvar: buton envanter içine giremez
                int[] clamped = helpbox_clampOutside(buttonScreenX, buttonScreenY, size);
                buttonScreenX = clamped[0];
                buttonScreenY = clamped[1];

                helpbox_setAnchorFromScreenPos(helpbox_draggedButton, buttonScreenX, buttonScreenY);
            }
        }
        
        cir.setReturnValue(true);
        cir.cancel();
    }

    /**
     * Verilen ekran piksel konumuna göre butonun anchor bayraklarını ve inv-relative
     * koordinatlarını hesaplayıp buton nesnesine yazar.
     */
    @Unique
    private void helpbox_setAnchorFromScreenPos(InventoryButton btn, int screenX, int screenY) {
        int invRight  = this.leftPos + this.imageWidth;
        int invBottom = this.topPos  + this.imageHeight;

        // Yatay anchor: buton sağ yarıdaysa sağdan, sol yarıdaysa soldan ölç
        if (screenX >= this.leftPos + this.imageWidth / 2) {
            btn.anchorRight = true;
            btn.x = screenX - invRight;
        } else {
            btn.anchorRight = false;
            btn.x = screenX - this.leftPos;
        }

        // Dikey anchor: buton alt yarıdaysa alttan, üst yarıdaysa üstten ölç
        if (screenY >= this.topPos + this.imageHeight / 2) {
            btn.anchorBottom = true;
            btn.y = screenY - invBottom;
        } else {
            btn.anchorBottom = false;
            btn.y = screenY - this.topPos;
        }
    }

    /**
     * Buton envanter dikdörtgeni içine girmesin diye en yakın dış kenara iter.
     * Buton merkezi hangi taraftaysa o tarafa iter (duvar davranışı).
     */
    @Unique
    private int[] helpbox_clampOutside(int bx, int by, int size) {
        int invRight  = this.leftPos + this.imageWidth;
        int invBottom = this.topPos  + this.imageHeight;

        // Çakışma yoksa dokunma
        if (bx + size <= this.leftPos || bx >= invRight
         || by + size <= this.topPos  || by >= invBottom) {
            return new int[]{bx, by};
        }

        // Hangi kenara daha yakın: fare konumuna göre karar ver
        int btnCX = bx + size / 2;
        int btnCY = by + size / 2;
        int invCX = this.leftPos + this.imageWidth  / 2;
        int invCY = this.topPos  + this.imageHeight / 2;

        int dx = btnCX - invCX;
        int dy = btnCY - invCY;

        if (Math.abs(dx) >= Math.abs(dy)) {
            bx = (dx >= 0) ? invRight : this.leftPos - size;
        } else {
            by = (dy >= 0) ? invBottom : this.topPos - size;
        }
        return new int[]{bx, by};
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void helpbox_onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (helpbox_hasEnhancedStorageOverlay()) return;

        if (PopupEditor.isOpen) {
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }
        if (ButtonStore.editMode) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                if (!helpbox_wasDragged && helpbox_mouseDownButton != null) {
                    Minecraft mc = Minecraft.getInstance();
                    PopupEditor.open(helpbox_mouseDownButton, false, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                } else if (helpbox_draggedButton != null) {
                    ButtonStore.save();
                }
                helpbox_draggedButton = null;
                helpbox_mouseDownButton = null;
                helpbox_wasDragged = false;
            }
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }
    }



    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void helpbox_onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (PopupEditor.isOpen) {
            PopupEditor.keyPressed(event);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        if (ButtonStore.pendingMacroName != null && event.key() == InputConstants.KEY_ESCAPE) {
            ButtonStore.pendingMacroName = null;
            ButtonStore.editMode = false;
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        if (ButtonStore.editMode && event.key() == InputConstants.KEY_ESCAPE) {
            ButtonStore.editMode = false;
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }
    }
}
