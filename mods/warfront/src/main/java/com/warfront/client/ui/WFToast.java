package com.warfront.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** A Warfront toast: themed panel, accent stripe, a title and up to two lines; about four seconds. */
public class WFToast implements Toast {
    private static final long SHOW_MS = 4000L;
    private static final int W = 180;
    private final String title;
    private final String text;
    private final int accent;

    public WFToast(String title, String text, int accent) {
        this.title = title;
        this.text = text;
        this.accent = accent;
    }

    @Override
    public int width() {
        return W;
    }

    @Override
    public int height() {
        return text.isEmpty() ? 22 : 38;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
        Font font = toasts.getMinecraft().font;
        WFTheme.panel(g, 0, 0, width(), height(), accent);
        WFTheme.text(g, font, title, 7, 7, WFTheme.opaque(accent == 0 ? 0xE2B55A : brighten(accent)));
        if (!text.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(Component.literal(text), W - 14);
            for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), 7, 18 + i * 9, WFTheme.TEXT, false);
        }
        return time >= SHOW_MS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }

    static int brighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 60), gr = Math.min(255, ((rgb >> 8) & 0xFF) + 60), b = Math.min(255, (rgb & 0xFF) + 60);
        return (r << 16) | (gr << 8) | b;
    }
}
