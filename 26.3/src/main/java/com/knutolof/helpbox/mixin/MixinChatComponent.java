package com.knutolof.helpbox.mixin;

import com.knutolof.helpbox.console.ConsoleHistory;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MixinChatComponent {

    @Inject(method = "addClientSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void helpbox$onAddClientSystemMessage(Component component, CallbackInfo ci) {
        ConsoleHistory.addEntry(component);
    }

    @Inject(method = "addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void helpbox$onAddServerSystemMessage(Component component, CallbackInfo ci) {
        ConsoleHistory.addEntry(component);
    }

    @Inject(method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"))
    private void helpbox$onAddPlayerMessage(Component component, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        ConsoleHistory.addEntry(component);
    }
}
