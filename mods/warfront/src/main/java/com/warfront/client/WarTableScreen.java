package com.warfront.client;

import com.warfront.alert.Alerts;
import com.warfront.client.ui.WFTheme;
import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.network.WarTableActionPayload;
import com.warfront.network.WarTablePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The War Table: Army (every unit, sortable and filterable; click one to make it glow), Heroes (fallen heroes with
 * their countdown and place, returning heroes with their ready time) and Base (level, progress, mana, unlocks).
 */
public class WarTableScreen extends Screen {
    private static final int W = 380, H = 230, ROW = 13;
    private static final String[] TABS = {"Army", "Heroes", "Base"};
    private static final String[] SORTS = {"distance", "role", "rank", "health"};
    private static final String[] FILTERS = {"all", "following", "on guard", "patrolling", "working", "holding"};
    private static int tab, sort, filter;

    private WarTablePayload data;
    private int scroll;
    private int left, top;

    public WarTableScreen(WarTablePayload data) {
        super(Component.literal("War Table"));
        this.data = data;
    }

    public void refresh(WarTablePayload data) {
        this.data = data;
        rebuildWidgets();
    }

    private int accent() {
        return minecraft == null || minecraft.player == null ? 0xC9A45C
                : Alerts.accent(Race.byId(ClientRaceState.get(minecraft.player.getUUID())));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            Button b = Button.builder(Component.literal(TABS[i]), x -> {
                tab = t;
                scroll = 0;
                rebuildWidgets();
            }).bounds(left + 6 + i * 64, top + 18, 62, 16).build();
            b.active = tab != i;
            addRenderableWidget(b);
        }
        if (tab == 0) {
            addRenderableWidget(Button.builder(Component.literal("Sort: " + SORTS[sort]), x -> {
                sort = (sort + 1) % SORTS.length;
                rebuildWidgets();
            }).bounds(left + W - 186, top + 18, 88, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Show: " + FILTERS[filter]), x -> {
                filter = (filter + 1) % FILTERS.length;
                scroll = 0;
                rebuildWidgets();
            }).bounds(left + W - 94, top + 18, 88, 16).build());
        }
    }

    private List<String[]> units() {
        List<String[]> out = new ArrayList<>();
        for (String u : data.units()) {
            String[] f = u.split("\\|");
            if (f.length < 7) continue;
            if (filter > 0) {
                String want = switch (filter) {
                    case 1 -> "following";
                    case 2 -> "guard";
                    case 3 -> "patrol";
                    case 4 -> "working";
                    default -> "holding";
                };
                if (!f[5].contains(want)) continue;
            }
            out.add(f);
        }
        Comparator<String[]> c = switch (sort) {
            case 1 -> Comparator.comparing(f -> f[2]);
            case 2 -> Comparator.comparing((String[] f) -> -Integer.parseInt(f[3]));
            case 3 -> Comparator.comparing(f -> Integer.parseInt(f[4]));
            default -> Comparator.comparing(f -> Integer.parseInt(f[6]));
        };
        out.sort(c);
        return out;
    }

    private int visibleRows() {
        return (H - 60) / ROW;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        WFTheme.panel(g, left, top, W, H, accent());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        WFTheme.text(g, font, title, left + 6, top + 6, WFTheme.opaque(accent()));
        int y = top + 42;
        int x = left + 8;
        int accent = accent();
        switch (tab) {
            case 0 -> {
                List<String[]> list = units();
                if (list.isEmpty()) WFTheme.text(g, font, "No units here.", x, y, WFTheme.MUTED);
                for (int i = scroll; i < Math.min(list.size(), scroll + visibleRows()); i++) {
                    String[] f = list.get(i);
                    boolean hover = mouseX >= left && mouseX < left + W && mouseY >= y - 1 && mouseY < y + ROW - 1;
                    if (hover) g.fill(left + 2, y - 2, left + W - 2, y + ROW - 2, 0x20FFFFFF);
                    WFTheme.text(g, font, f[1], x, y, WFTheme.TEXT);
                    int rank = Integer.parseInt(f[3]);
                    for (int r = 0; r < rank; r++) g.fill(x + font.width(f[1]) + 4 + r * 4, y + 1, x + font.width(f[1]) + 6 + r * 4, y + 7, WFTheme.opaque(accent));
                    WFTheme.text(g, font, f[2], x + 140, y, WFTheme.MUTED);
                    int hp = Integer.parseInt(f[4]);
                    WFTheme.bar(g, x + 210, y + 2, 50, 4, hp / 100F, hp > 60 ? 0x5FA83A : hp > 30 ? 0xE8C547 : 0xC0392B);
                    WFTheme.text(g, font, f[5], x + 268, y, WFTheme.MUTED);
                    WFTheme.text(g, font, f[6] + "m", x + 338, y, WFTheme.MUTED);
                    y += ROW;
                }
                WFTheme.text(g, font, list.size() + " units · click one to make it glow · scroll for more", x, top + H - 12, WFTheme.MUTED);
            }
            case 1 -> {
                if (data.heroes().isEmpty()) WFTheme.text(g, font, "No Captains, Champions or war beasts yet.", x, y, WFTheme.MUTED);
                for (String h : data.heroes()) {
                    String[] f = h.split("\\|");
                    if (f.length < 4) continue;
                    int secs = Integer.parseInt(f[2]);
                    String status = switch (f[1]) {
                        case "fallen" -> "FALLEN: revive within " + secs + "s at " + f[3];
                        case "returning" -> secs > 0 ? "returning: ready in " + secs / 60 + ":" + String.format("%02d", secs % 60) : "ready at any altar (half cost)";
                        default -> "in the field at " + f[3];
                    };
                    WFTheme.text(g, font, f[0], x, y, WFTheme.TEXT);
                    WFTheme.text(g, font, status, x + 150, y, f[1].equals("fallen") ? 0xFFFF8080 : WFTheme.MUTED);
                    y += ROW;
                }
            }
            default -> {
                for (String line : data.base()) {
                    if (line.startsWith("bar|")) {
                        String[] f = line.split("\\|");
                        int have = Integer.parseInt(f[2]), need = Math.max(1, Integer.parseInt(f[3]));
                        WFTheme.text(g, font, f[1], x, y, WFTheme.TEXT);
                        WFTheme.bar(g, x + 80, y + 2, 200, 5, have / (float) need, f[1].equals("Mana") ? 0x7FD8FF : accent);
                        WFTheme.text(g, font, have + " / " + need, x + 290, y, WFTheme.MUTED);
                    } else {
                        WFTheme.text(g, font, line, x, y, line.startsWith("Base level") ? WFTheme.opaque(accent) : WFTheme.TEXT);
                    }
                    y += ROW + 3;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == 0 && mouseY >= top + 41 && mouseX >= left && mouseX < left + W) {
            int row = (int) ((mouseY - (top + 41)) / ROW) + scroll;
            List<String[]> list = units();
            if (row >= scroll && row < Math.min(list.size(), scroll + visibleRows())) {
                PacketDistributor.sendToServer(new WarTableActionPayload(Integer.parseInt(list.get(row)[0])));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        int max = Math.max(0, units().size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
