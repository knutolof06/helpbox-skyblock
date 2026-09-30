package com.knutolof.helpbox.mixin;

import com.knutolof.helpbox.inventory.buttons.PopupEditor;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void helpbox_onCharTyped(long window, CharacterEvent event, CallbackInfo ci) {
        if (PopupEditor.isOpen) {
            PopupEditor.charTyped(event);
            ci.cancel();
        }
    }
}
