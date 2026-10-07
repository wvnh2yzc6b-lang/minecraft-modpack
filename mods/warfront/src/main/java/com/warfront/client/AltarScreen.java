package com.warfront.client;

import com.warfront.alert.Alerts;
import com.warfront.client.ui.WFTheme;
import com.warfront.faction.Race;
import com.warfront.network.AltarOpenPayload;
import com.warfront.network.AltarSummonPayload;
import com.warfront.network.ClientRaceState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The summoning menu: a grid of unit cards for the player's race (locked ones greyed with the reason), a detail pane
 * for the selected card with a Summon button, the mana in reach, beasts used out of allowed, and the returning heroes.
 * The server still checks everything.
 */
public class AltarScreen extends Screen {
    private static final int W = 360, H = 236, COLS = 4, CARD_W = 84, CARD_H = 26, GAP = 3;
    private AltarOpenPayload data;
    private int selected = -1;
    private int left, top;

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

    private int accent() {
        return minecraft == null || minecraft.player == null ? 0xC9A45C
                : Alerts.accent(Race.byId(ClientRaceState.get(minecraft.player.getUUID())));
    }

    private List<Integer> roles() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < data.costs().size(); i++) if (data.costs().get(i) >= 0) out.add(i);
        return out;
    }

    private boolean creative() {
        return minecraft != null && minecraft.player != null && minecraft.player.isCreative();
    }

    private String lock(int role) {
        return role < data.locks().size() ? data.locks().get(role) : "";
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (!data.missing().isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                    .bounds(width / 2 - 50, top + H - 28, 100, 20).build());
            return;
        }
        List<Integer> roles = roles();
        if (selected < 0 && !roles.isEmpty()) selected = roles.get(0);
        for (int n = 0; n < roles.size(); n++) {
            int role = roles.get(n);
            int x = left + 6 + (n % COLS) * (CARD_W + GAP), y = top + 26 + (n / COLS) * (CARD_H + GAP);
            Button card = Button.builder(Component.literal(data.names().get(role)), b -> {
                selected = role;
                rebuildWidgets();
            }).bounds(x, y, CARD_W, CARD_H).build();
            String lock = lock(role);
            if (!lock.isEmpty()) card.setTooltip(Tooltip.create(Component.literal(lock)));
            addRenderableWidget(card);
        }
        if (selected >= 0) {
            int cost = data.costs().get(selected);
            boolean locked = !lock(selected).isEmpty();
            Button summon = Button.builder(Component.literal("Summon · " + cost),
                            b -> PacketDistributor.sendToServer(new AltarSummonPayload(data.pos(), selected)))
                    .bounds(left + W - 96, top + 26 + 3 * (CARD_H + GAP) + 32, 88, 18).build();
            summon.active = !locked && (creative() || cost <= data.mana());
            addRenderableWidget(summon);
        }
        int heroY = top + H - 26;
        for (int i = 0; i < Math.min(3, data.heroNames().size()); i++) {
            int index = i;
            int cost = data.heroCosts().get(i), wait = data.heroWait().get(i);
            String label = data.heroNames().get(i) + " · " + cost + (wait > 0 ? " (" + wait / 60 + ":" + String.format("%02d", wait % 60) + ")" : "");
            Button b = Button.builder(Component.literal(label),
                            btn -> PacketDistributor.sendToServer(new com.warfront.network.AltarReturnPayload(data.pos(), index)))
                    .bounds(left + 6 + i * 117, heroY, 114, 18)
                    .tooltip(Tooltip.create(Component.literal(wait > 0 ? "Still recovering." : "A returning hero, rank kept. Half cost.")))
                    .build();
            b.active = wait <= 0 && (creative() || cost <= data.mana());
            addRenderableWidget(b);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        WFTheme.panel(g, left, top, W, H, accent());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        WFTheme.text(g, font, title, left + 6, top + 8, WFTheme.opaque(accent()));
        if (!data.missing().isEmpty()) {
            WFTheme.text(g, font, "This altar is not complete.", left + 6, top + 30, 0xFFFF8080);
            List<FormattedCharSequence> lines = font.split(Component.literal(data.missing()), W - 12);
            for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), left + 6, top + 46 + i * 10, WFTheme.TEXT, false);
            return;
        }
        String head = "Mana in reach: " + (int) data.mana() + (data.beastCap() > 0 ? "   ·   beasts " + data.beasts() + "/" + data.beastCap() : "");
        WFTheme.text(g, font, head, left + W - 6 - font.width(head), top + 8, 0xFF7FD8FF);
        // Grey out locked and unaffordable cards.
        List<Integer> roles = roles();
        for (int n = 0; n < roles.size(); n++) {
            int role = roles.get(n);
            int x = left + 6 + (n % COLS) * (CARD_W + GAP), y = top + 26 + (n / COLS) * (CARD_H + GAP);
            boolean locked = !lock(role).isEmpty();
            boolean poor = !creative() && data.costs().get(role) > data.mana();
            if (role == selected) g.renderOutline(x - 1, y - 1, CARD_W + 2, CARD_H + 2, WFTheme.opaque(accent()));
            if (locked || poor) g.fill(x, y, x + CARD_W, y + CARD_H, 0x90101318);
            String sub = locked ? "locked" : data.costs().get(role) + " mana";
            WFTheme.text(g, font, sub, x + (CARD_W - font.width(sub)) / 2, y + CARD_H - 9, locked ? 0xFFFF8080 : 0xFF7FD8FF);
        }
        // Detail pane.
        if (selected >= 0) {
            int y = top + 26 + 3 * (CARD_H + GAP) + 4;
            g.fill(left + 6, y - 2, left + W - 6, y - 1, 0x40FFFFFF);
            WFTheme.text(g, font, data.names().get(selected), left + 6, y + 2, WFTheme.TEXT);
            String stats = selected < data.stats().size() ? data.stats().get(selected) : "";
            WFTheme.text(g, font, stats, left + 6, y + 13, WFTheme.MUTED);
            List<FormattedCharSequence> lines = font.split(Component.literal(data.descriptions().get(selected)), W - 112);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(font, lines.get(i), left + 6, y + 25 + i * 10, WFTheme.TEXT, false);
            String lock = lock(selected);
            if (!lock.isEmpty()) WFTheme.text(g, font, lock, left + 6, y + 57, 0xFFFF8080);
        }
        if (!data.heroNames().isEmpty()) WFTheme.text(g, font, "Returning heroes", left + 6, top + H - 37, 0xFFE2B55A);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
