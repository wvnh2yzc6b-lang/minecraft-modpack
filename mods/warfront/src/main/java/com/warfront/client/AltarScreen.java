package com.warfront.client;

import com.warfront.network.AltarOpenPayload;
import com.warfront.network.AltarSummonPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** The summoning menu: one button per unit of the player's race, with its mana cost. */
public class AltarScreen extends Screen {
    private static final int BUTTON_W = 170, BUTTON_H = 20, GAP = 4;
    private AltarOpenPayload data;

    public AltarScreen(AltarOpenPayload data) {
        super(Component.literal("Summoning Altar"));
        this.data = data;
    }

    public BlockPos pos() {
        return data.pos();
    }

    public void refresh(AltarOpenPayload data) {
        this.data = data;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        if (!data.missing().isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                    .bounds(width / 2 - 50, height / 2 + 40, 100, BUTTON_H).build());
            return;
        }
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();
        int rows = (visible() + 1) / 2;
        int left = width / 2 - BUTTON_W - GAP / 2;
        int top = height / 2 - (rows * (BUTTON_H + GAP)) / 2 + 10;
        int slot = 0;
        for (int i = 0; i < data.names().size(); i++) {
            int role = i;
            int cost = data.costs().get(i);
            if (cost < 0) continue;
            int n = slot++;
            Button b = Button.builder(Component.literal(data.names().get(i) + "  ·  " + cost),
                            btn -> PacketDistributor.sendToServer(new AltarSummonPayload(data.pos(), role)))
                    .bounds(left + (n % 2) * (BUTTON_W + GAP), top + (n / 2) * (BUTTON_H + GAP), BUTTON_W, BUTTON_H)
                    .tooltip(Tooltip.create(Component.literal(data.descriptions().get(i) + "\nCosts " + cost + " mana.")))
                    .build();
            b.active = creative || cost <= data.mana();
            addRenderableWidget(b);
        }
    }

    private int visible() {
        int n = 0;
        for (int c : data.costs()) if (c >= 0) n++;
        return n;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int rows = (visible() + 1) / 2;
        int top = data.missing().isEmpty() ? height / 2 - (rows * (BUTTON_H + GAP)) / 2 - 22 : height / 2 - 50;
        g.drawCenteredString(font, title, width / 2, top, 0xE2B55A);
        if (data.missing().isEmpty()) {
            g.drawCenteredString(font, Component.literal("Mana in reach: " + (int) data.mana()), width / 2, top + 14, 0x7FD8FF);
        } else {
            g.drawCenteredString(font, Component.literal("This altar is not complete."), width / 2, top + 16, 0xFF8080);
            List<FormattedCharSequence> lines = font.split(Component.literal(data.missing()), Math.min(320, width - 40));
            for (int i = 0; i < lines.size(); i++) {
                g.drawCenteredString(font, lines.get(i), width / 2, top + 32 + i * 11, 0xDDDDDD);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
