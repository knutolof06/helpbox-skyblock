package com.knutolof.helpbox.mixin;

import com.daqem.uilib.gui.background.ColorBackground;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ColorBackground.class, remap = false)
public abstract class MixinColorBackground {

    @Shadow
    public abstract int getColor();

    /**
     * @author HelpBox
     * @reason Fix NoSuchFieldError on Minecraft 26.3
     */
    @Overwrite
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui != null ? mc.gui.screen() : null;
        if (screen != null) {
            graphics.fill(0, 0, screen.width, screen.height, getColor());
        }
    }
}
