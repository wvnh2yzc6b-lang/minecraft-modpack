package com.warfront.client;

import com.warfront.network.ClientManaState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** A slim mana bar above the hotbar, left of the experience bar. */
public final class ManaHud {
    private ManaHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        float max = Math.max(1F, ClientManaState.max);
        float mana = Math.min(ClientManaState.mana, max);

        int w = 81;
        int x = g.guiWidth() / 2 - 91;
        int y = g.guiHeight() - (mc.player.isCreative() ? 33 : 49) - 8;
        g.fill(x - 1, y - 1, x + w + 1, y + 4, 0xC0000000);
        g.fill(x, y, x + (int) (w * mana / max), y + 3, 0xFF3FA9F5);
        g.fill(x, y, x + (int) (w * mana / max), y + 1, 0xFF9FDBFF);
        String text = (int) mana + " / " + (int) max;
        g.drawString(mc.font, text, x + w + 4, y - 2, 0xFF7FD0FF, true);
    }
}
