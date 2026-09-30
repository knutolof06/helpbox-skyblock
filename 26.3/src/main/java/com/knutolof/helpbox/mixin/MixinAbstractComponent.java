package com.knutolof.helpbox.mixin;

import com.daqem.uilib.gui.component.AbstractComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractComponent.class, remap = false)
public abstract class MixinAbstractComponent {

    @Shadow
    public abstract void extractRenderStateBase(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, int width, int height);

    /**
     * @author HelpBox
     * @reason Fix NoSuchFieldError on Minecraft 26.3 where Minecraft.screen was refactored to Minecraft.gui.screen()
     */
    @Overwrite
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui != null ? mc.gui.screen() : null;
        if (screen != null) {
            extractRenderStateBase(graphics, mouseX, mouseY, partialTick, screen.width, screen.height);
        }
    }
}
