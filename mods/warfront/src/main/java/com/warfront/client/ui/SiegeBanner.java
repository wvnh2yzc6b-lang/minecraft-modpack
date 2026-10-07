package com.warfront.client.ui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * The siege banner: wide, centered near the top, for wave start, wave won and a fallen War Standard. Slides in,
 * holds three seconds, fades. Tinted to the enemy faction.
 */
public final class SiegeBanner {
    private static final long IN_MS = 300, HOLD_MS = 3000, OUT_MS = 700;
    private static String title = "";
    private static String text = "";
    private static int accent;
    private static long shownAt = -1;

    private SiegeBanner() {}

    public static void show(String t, String sub, int color) {
        title = t;
        text = sub;
        accent = color;
        shownAt = System.currentTimeMillis();
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        if (shownAt < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        long age = System.currentTimeMillis() - shownAt;
        if (age > IN_MS + HOLD_MS + OUT_MS) {
            shownAt = -1;
            return;
        }
        float slide = age < IN_MS ? age / (float) IN_MS : 1F;
        float alpha = age > IN_MS + HOLD_MS ? 1F - (age - IN_MS - HOLD_MS) / (float) OUT_MS : 1F;
        Font font = mc.font;
        int w = Math.max(220, Math.max(font.width(title) * 2, font.width(text)) + 40);
        int h = text.isEmpty() ? 30 : 42;
        int x = (g.guiWidth() - w) / 2;
        int y = Mth.floor(-h + (h + 18) * slide);
        WFTheme.panel(g, x, y, w, h, accent, alpha);
        g.pose().pushPose();
        g.pose().translate(g.guiWidth() / 2F, y + 9, 0);
        g.pose().scale(2F, 2F, 1F);
        int tw = font.width(title);
        g.drawString(font, title, -tw / 2, 0, WFTheme.withAlpha(WFTheme.opaque(WFToast.brighten(accent)), Math.max(0.05F, alpha)), false);
        g.pose().popPose();
        if (!text.isEmpty()) {
            g.drawString(font, text, (g.guiWidth() - font.width(text)) / 2, y + h - 13,
                    WFTheme.withAlpha(WFTheme.TEXT, Math.max(0.05F, alpha)), false);
        }
    }
}
