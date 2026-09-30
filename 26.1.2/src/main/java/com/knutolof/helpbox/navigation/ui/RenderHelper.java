package com.knutolof.helpbox.navigation.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class RenderHelper {

    public static void fillFlatRect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int bgColor, int borderColor) {
        int right = x + width;
        int bottom = y + height;
        
        // Background
        graphics.fill(x, y, right, bottom, bgColor);
        
        // Border
        graphics.fill(x, y, right, y + 1, borderColor); // Top
        graphics.fill(x, bottom - 1, right, bottom, borderColor); // Bottom
        graphics.fill(x, y + 1, x + 1, bottom - 1, borderColor); // Left
        graphics.fill(right - 1, y + 1, right, bottom - 1, borderColor); // Right
    }
}
