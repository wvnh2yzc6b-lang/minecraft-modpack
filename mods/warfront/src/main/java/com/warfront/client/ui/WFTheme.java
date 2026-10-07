package com.warfront.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The Warfront look: semi-transparent near-black panels with a 1px light border and an optional accent stripe, plain
 * text without shadow, slim bars. One accent per race (see Alerts.accent).
 */
public final class WFTheme {
    public static final int PANEL = 0xD8101318;
    public static final int BORDER = 0x60FFFFFF;
    public static final int TEXT = 0xFFE8E8E8;
    public static final int MUTED = 0xFF9AA3AE;
    public static final int TRACK = 0x80303840;

    private WFTheme() {}

    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** A panel with a border, and an accent stripe on top when {@code accent} isn't 0. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int accent, float alpha) {
        g.fill(x, y, x + w, y + h, withAlpha(PANEL, alpha));
        g.renderOutline(x, y, w, h, withAlpha(BORDER, alpha));
        if (accent != 0) g.fill(x + 1, y + 1, x + w - 1, y + 3, withAlpha(opaque(accent), alpha));
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h, int accent) {
        panel(g, x, y, w, h, accent, 1F);
    }

    /** A horizontal bar: track plus fill for {@code frac} (0..1). */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        g.fill(x, y, x + w, y + h, TRACK);
        int fw = Math.round(w * Math.max(0F, Math.min(1F, frac)));
        if (fw > 0) g.fill(x, y, x + fw, y + h, opaque(color));
    }

    public static void text(GuiGraphics g, Font font, String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color, false);
    }

    public static void text(GuiGraphics g, Font font, Component c, int x, int y, int color) {
        g.drawString(font, c, x, y, color, false);
    }

    /** A small square slot for an icon. */
    public static void slot(GuiGraphics g, int x, int y, int size, int accent) {
        g.fill(x, y, x + size, y + size, TRACK);
        g.renderOutline(x, y, size, size, accent == 0 ? BORDER : opaque(accent));
    }
}
