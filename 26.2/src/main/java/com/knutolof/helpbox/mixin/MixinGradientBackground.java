package com.knutolof.helpbox.mixin;

import com.daqem.uilib.gui.background.GradientBackground;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GradientBackground.class, remap = false)
public abstract class MixinGradientBackground {

    @Shadow
    public abstract int getColorFrom();

    @Shadow
    public abstract int getColorTo();

    /**
     * @author HelpBox
     * @reason Fix NoSuchFieldError on Minecraft 26.2
     */
    @Overwrite
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui != null ? mc.gui.screen() : null;
        if (screen != null) {
            graphics.fillGradient(0, 0, screen.width, screen.height, getColorFrom(), getColorTo());
        }
    }
}
