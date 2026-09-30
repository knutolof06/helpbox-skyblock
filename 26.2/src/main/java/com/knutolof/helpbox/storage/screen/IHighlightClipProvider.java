package com.knutolof.helpbox.storage.screen;

import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

public interface IHighlightClipProvider {
    /**
     * [left, top, right, bottom] scissor rect for this slot's highlight, or null for no clip.
     */
    @Nullable
    int[] helpbox$getHighlightClip(Slot slot);
}