package com.knutolof.helpbox.mixin;

import com.daqem.uilib.api.screen.IScreenAccessor;
import com.daqem.uilib.gui.background.BlurredBackground;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = BlurredBackground.class, remap = false)
public abstract class MixinBlurredBackground {

    /**
     * @author HelpBox
     * @reason Fix NoSuchFieldError on Minecraft 26.2
     */
    @Overwrite
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui != null ? mc.gui.screen() : null;
        if (screen instanceof IScreenAccessor screenAccessor) {
            screenAccessor.uilib$extractBlurredBackground(graphics);
        }
    }
}
