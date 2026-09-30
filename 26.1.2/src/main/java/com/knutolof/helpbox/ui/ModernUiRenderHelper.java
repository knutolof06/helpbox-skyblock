package com.knutolof.helpbox.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * Modern, profesyonel ve yumuşak hatlara (rounded corners) sahip UI çizim yardımcısı.
 */
public final class ModernUiRenderHelper {

    private ModernUiRenderHelper() {}

    /**
     * Yumuşak köşeli, modern dolu dikdörtgen çizer.
     */
    public static void fillRoundedRect(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
        if (radius <= 0) {
            graphics.fill(x, y, x + w, y + h, color);
            return;
        }
        radius = Math.min(radius, Math.min(w / 2, h / 2));

        for (int dy = 0; dy < radius; dy++) {
            double distance = radius - dy;
            int indent = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - distance * distance)));
            // Üst kavis satırı
            graphics.fill(x + indent, y + dy, x + w - indent, y + dy + 1, color);
            // Alt kavis satırı
            graphics.fill(x + indent, y + h - dy - 1, x + w - indent, y + h - dy, color);
        }
        // Orta gövde
        graphics.fill(x, y + radius, x + w, y + h - radius, color);
    }

    /**
     * Yumuşak köşeli modern çerçeve (outline) çizer.
     */
    public static void drawRoundedOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
        if (radius <= 0) {
            graphics.fill(x, y, x + w, y + 1, color);
            graphics.fill(x, y + h - 1, x + w, y + h, color);
            graphics.fill(x, y + 1, x + 1, y + h - 1, color);
            graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
            return;
        }
        radius = Math.min(radius, Math.min(w / 2, h / 2));

        for (int dy = 0; dy < radius; dy++) {
            double distance = radius - dy;
            int indent = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - distance * distance)));
            // Üst kavis sınırları
            graphics.fill(x + indent, y + dy, x + indent + 1, y + dy + 1, color);
            graphics.fill(x + w - indent - 1, y + dy, x + w - indent, y + dy + 1, color);
            // Alt kavis sınırları
            graphics.fill(x + indent, y + h - dy - 1, x + indent + 1, y + h - dy, color);
            graphics.fill(x + w - indent - 1, y + h - dy - 1, x + w - indent, y + h - dy, color);
        }
        // Düz üst ve alt kenarlar
        graphics.fill(x + radius, y, x + w - radius, y + 1, color);
        graphics.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
        // Düz yan kenarlar
        graphics.fill(x, y + radius, x + 1, y + h - radius, color);
        graphics.fill(x + w - 1, y + radius, x + w, y + h - radius, color);
    }

    /**
     * Modern kart (card) arka planı ve çerçevesi çizer.
     */
    public static void drawModernCard(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int bgColor, int borderColor) {
        fillRoundedRect(graphics, x, y, w, h, radius, bgColor);
        if ((borderColor & 0xFF000000) != 0) {
            drawRoundedOutline(graphics, x, y, w, h, radius, borderColor);
        }
    }

    /**
     * Modern animasyonlu/yuvarlak buton (pill button) çizer.
     */
    public static void drawPillButton(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, String text, int accentColor, boolean hovered, boolean active) {
        int radius = h / 2;
        int bg = active ? accentColor : (hovered ? 0xFF2B3346 : 0xFF1C2230);
        int border = active ? 0xFFFFFFFF : (hovered ? accentColor : 0x554A5568);
        int textColor = active ? 0xFFFFFFFF : (hovered ? 0xFFFFFFFF : 0xFFCBD5E1);

        fillRoundedRect(graphics, x, y, w, h, radius, bg);
        drawRoundedOutline(graphics, x, y, w, h, radius, border);

        int textW = font.width(text);
        graphics.text(font, text, x + (w - textW) / 2, y + (h - 8) / 2, textColor, false);
    }

    /**
     * Modern iOS/Material tarzı Açık/Kapalı Toggle Switch çizer.
     */
    public static void drawModernToggle(GuiGraphicsExtractor graphics, Font font, int x, int y, boolean active, boolean hovered) {
        int switchW = 38;
        int switchH = 20;
        int radius = switchH / 2;

        int trackBg = active ? 0xFF10B981 : (hovered ? 0xFF374151 : 0xFF1F2937);
        int trackBorder = active ? 0xFF34D399 : (hovered ? 0xFF4B5563 : 0xFF374151);

        fillRoundedRect(graphics, x, y, switchW, switchH, radius, trackBg);
        drawRoundedOutline(graphics, x, y, switchW, switchH, radius, trackBorder);

        // Knob (Kaydırıcı yuvarlak)
        int knobSize = switchH - 4;
        int knobX = active ? (x + switchW - knobSize - 2) : (x + 2);
        int knobY = y + 2;
        int knobColor = 0xFFFFFFFF;

        fillRoundedRect(graphics, knobX, knobY, knobSize, knobSize, knobSize / 2, knobColor);
    }

    /**
     * Durum veya kategori etiketi (Badge / Chip) çizer.
     */
    public static void drawBadge(GuiGraphicsExtractor graphics, Font font, int x, int y, String text, int bgColor, int textColor) {
        int textW = font.width(text);
        int padH = 6;
        int w = textW + padH * 2;
        int h = 14;
        fillRoundedRect(graphics, x, y, w, h, 6, bgColor);
        graphics.text(font, text, x + padH, y + 3, textColor, false);
    }

    /**
     * Metni belirtilen maksimum genişliğe göre satırlara bölerek çizer ve bir sonraki Y koordinatını döner.
     */
    public static int drawWrappedText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int maxW, int color, int lineSpacing) {
        if (text == null || text.isEmpty()) return y;
        List<FormattedText> lines = font.getSplitter().splitLines(text, Math.max(20, maxW), Style.EMPTY);
        int curY = y;
        for (FormattedText line : lines) {
            graphics.text(font, line.getString(), x, curY, color, false);
            curY += lineSpacing;
        }
        return curY;
    }
}
