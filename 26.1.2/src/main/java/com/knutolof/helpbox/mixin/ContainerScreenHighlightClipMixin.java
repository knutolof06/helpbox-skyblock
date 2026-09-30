package com.knutolof.helpbox.mixin;

import com.knutolof.helpbox.storage.screen.IHighlightClipProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenHighlightClipMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "extractSlotHighlightBack", at = @At("HEAD"))
    private void helpbox$clipBackStart(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        helpbox$apply(graphics);
    }

    @Inject(method = "extractSlotHighlightBack", at = @At("TAIL"))
    private void helpbox$clipBackEnd(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        helpbox$remove(graphics);
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"))
    private void helpbox$clipFrontStart(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        helpbox$apply(graphics);
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("TAIL"))
    private void helpbox$clipFrontEnd(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        helpbox$remove(graphics);
    }

    @Unique
    private void helpbox$apply(GuiGraphicsExtractor graphics) {
        if (hoveredSlot == null || !(((Object) this) instanceof IHighlightClipProvider provider)) return;
        int[] clip = provider.helpbox$getHighlightClip(hoveredSlot);
        if (clip != null) {
            graphics.enableScissor(clip[0] - leftPos, clip[1] - topPos, clip[2] - leftPos, clip[3] - topPos);
        }
    }

    @Unique
    private void helpbox$remove(GuiGraphicsExtractor graphics) {
        if (hoveredSlot == null || !(((Object) this) instanceof IHighlightClipProvider provider)) return;
        if (provider.helpbox$getHighlightClip(hoveredSlot) != null) {
            graphics.disableScissor();
        }
    }
}
