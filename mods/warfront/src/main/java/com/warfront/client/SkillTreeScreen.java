package com.warfront.client;

import com.warfront.alert.Alerts;
import com.warfront.client.ui.WFTheme;
import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.network.SkillActionPayload;
import com.warfront.skill.ClientSkills;
import com.warfront.skill.SkillNode;
import com.warfront.skill.SkillPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Skill Tree (K, or from the War Table): paths down the left, the chosen path's tree in the middle, the picked
 * node on the right with Learn and slot buttons, the four skill slots and the respec along the bottom.
 */
public class SkillTreeScreen extends Screen {
    private static final int W = 410, H = 260, NODE = 18, COL = 30, ROW = 25;
    private static int pathIndex;
    @Nullable
    private static String selected;

    private int left, top, seenVersion = -1;

    public SkillTreeScreen() {
        super(Component.literal("Skill Tree"));
    }

    private static void send(String... args) {
        PacketDistributor.sendToServer(new SkillActionPayload(new ArrayList<>(List.of(args))));
    }

    @Nullable
    private Race race() {
        return minecraft == null || minecraft.player == null ? null : Race.byId(ClientRaceState.get(minecraft.player.getUUID()));
    }

    private int accent() {
        return Alerts.accent(race());
    }

    @Nullable
    private SkillPath path() {
        List<SkillPath> paths = ClientSkills.paths();
        if (paths.isEmpty()) return null;
        return paths.get(Math.floorMod(pathIndex, paths.size()));
    }

    private int treeX() {
        return left + 104;
    }

    private int treeY() {
        return top + 40;
    }

    private int nodeX(SkillNode n) {
        return treeX() + n.col() * COL + 4;
    }

    private int nodeY(SkillNode n) {
        return treeY() + n.row() * ROW;
    }

    private static boolean available(SkillNode n) {
        return !ClientSkills.has(n.id()) && (n.isRoot() || n.parents().stream().anyMatch(ClientSkills::has));
    }

    private int cost(SkillNode n) {
        SkillPath p = path();
        if (p != null && p.favors(race())) {
            long in = p.nodes().stream().filter(x -> ClientSkills.has(x.id())).count();
            if (in < 3) return 0;
        }
        return n.cost();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        seenVersion = ClientSkills.version;
        List<SkillPath> paths = ClientSkills.paths();
        Race race = race();
        for (int i = 0; i < paths.size(); i++) {
            int idx = i;
            SkillPath p = paths.get(i);
            Button b = Button.builder(Component.literal(p.name() + (p.favors(race) ? " *" : "")), x -> {
                pathIndex = idx;
                selected = null;
                rebuildWidgets();
            }).bounds(left + 6, top + 40 + i * 22, 90, 20).build();
            b.active = Math.floorMod(pathIndex, paths.size()) != i;
            b.setTooltip(Tooltip.create(Component.literal(p.description() + (p.favors(race)
                    ? "\n* Your race's path: your first 3 nodes here are free." : ""))));
            addRenderableWidget(b);
        }
        Button racePath = Button.builder(Component.literal((race == null ? "Race" : race.displayName()) + " path"), x -> {})
                .bounds(left + 6, top + 40 + paths.size() * 22, 90, 20).build();
        racePath.active = false;
        racePath.setTooltip(Tooltip.create(Component.literal("Coming: each race's own path, deepening its race power.")));
        addRenderableWidget(racePath);

        SkillNode n = selected == null ? null : ClientSkills.node(selected);
        int rx = left + W - 136;
        if (n != null) {
            if (available(n)) {
                int c = cost(n);
                Button learn = Button.builder(Component.literal(c == 0 ? "Learn (free)" : "Learn (" + c + " pt" + (c == 1 ? "" : "s") + ")"),
                        x -> send("learn", n.id())).bounds(rx, top + H - 82, 130, 20).build();
                learn.active = ClientSkills.points >= c;
                addRenderableWidget(learn);
            } else if (n.active() && ClientSkills.has(n.id())) {
                for (int i = 0; i < 4; i++) {
                    int slot = i;
                    Button b = Button.builder(Component.literal(Integer.toString(i + 1)), x -> send("slot", Integer.toString(slot), n.id()))
                            .bounds(rx + i * 33, top + H - 82, 31, 20).build();
                    b.setTooltip(Tooltip.create(Component.literal("Put " + n.name() + " in skill slot " + (i + 1)
                            + " (" + ClientSetup.SKILL_KEYS[i].getTranslatedKeyMessage().getString() + ").")));
                    addRenderableWidget(b);
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            int slot = i;
            String id = ClientSkills.slots().get(i);
            SkillNode s = id.isEmpty() ? null : ClientSkills.node(id);
            String key = ClientSetup.SKILL_KEYS[i].getTranslatedKeyMessage().getString();
            Button b = Button.builder(Component.literal("[" + key + "] " + (s == null ? "empty" : s.name())), x -> {
                if (s != null) {
                    selected = s.id();
                    rebuildWidgets();
                }
            }).bounds(left + 104 + i * 76, top + H - 26, 74, 20).build();
            b.setTooltip(Tooltip.create(Component.literal(s == null ? "Pick a learned active skill, then a slot number."
                    : s.name() + ". Right-click to empty the slot.")));
            addRenderableWidget(b);
        }
        Button respec = Button.builder(Component.literal("Respec"), x -> send("respec"))
                .bounds(left + 6, top + H - 26, 90, 20).build();
        respec.active = ClientSkills.respecCost > 0;
        respec.setTooltip(Tooltip.create(Component.literal(ClientSkills.respecCost > 0
                ? "Refund every point for " + ClientSkills.respecCost + " Mana Crystals." : "Nothing learned yet.")));
        addRenderableWidget(respec);
    }

    @Override
    public void tick() {
        super.tick();
        if (seenVersion != ClientSkills.version) rebuildWidgets();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        WFTheme.panel(g, left, top, W, H, accent());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int accent = accent();
        WFTheme.text(g, font, title, left + 6, top + 7, WFTheme.opaque(accent));
        String head = "Renown " + ClientSkills.level + " · " + ClientSkills.points + " point" + (ClientSkills.points == 1 ? "" : "s");
        WFTheme.text(g, font, head, left + 104, top + 7, WFTheme.TEXT);
        if (ClientSkills.toNext > 0) {
            WFTheme.bar(g, left + 104, top + 19, 200, 4, ClientSkills.into / (float) ClientSkills.toNext, accent);
            WFTheme.text(g, font, ClientSkills.into + " / " + ClientSkills.toNext, left + 310, top + 16, WFTheme.MUTED);
        }
        SkillPath path = path();
        if (path == null) {
            WFTheme.text(g, font, "Loading the skill paths...", treeX(), treeY(), WFTheme.MUTED);
            return;
        }
        WFTheme.text(g, font, path.name(), left + 6, top + 26, WFTheme.opaque(path.color()));
        // links
        for (SkillNode n : path.nodes()) {
            for (String pid : n.parents()) {
                SkillNode par = null;
                for (SkillNode x : path.nodes()) if (x.id().equals(pid)) par = x;
                if (par == null) continue;
                int color = ClientSkills.has(pid) ? WFTheme.opaque(path.color()) : 0xFF3A4048;
                int x1 = nodeX(par) + NODE / 2, y1 = nodeY(par) + NODE, x2 = nodeX(n) + NODE / 2, y2 = nodeY(n);
                int mid = (y1 + y2) / 2;
                g.fill(x1, y1, x1 + 1, mid + 1, color);
                g.fill(Math.min(x1, x2), mid, Math.max(x1, x2) + 1, mid + 1, color);
                g.fill(x2, mid, x2 + 1, y2, color);
            }
        }
        // nodes
        SkillNode hover = null;
        for (SkillNode n : path.nodes()) {
            int x = nodeX(n), y = nodeY(n);
            boolean learned = ClientSkills.has(n.id()), open = available(n);
            int fill = learned ? WFTheme.withAlpha(WFTheme.opaque(path.color()), 0.85F) : WFTheme.TRACK;
            int border = learned ? 0xFFFFFFFF : open ? WFTheme.opaque(path.color()) : 0xFF3A4048;
            g.fill(x, y, x + NODE, y + NODE, fill);
            g.renderOutline(x, y, NODE, NODE, border);
            if (n.active()) g.renderOutline(x - 2, y - 2, NODE + 4, NODE + 4, border);
            if (n.id().equals(selected)) g.renderOutline(x - 3, y - 3, NODE + 6, NODE + 6, 0xFFFFE07A);
            String initials = initials(n.name());
            WFTheme.text(g, font, initials, x + (NODE - font.width(initials)) / 2 + 1, y + 5, learned ? 0xFF101318 : open ? WFTheme.TEXT : 0xFF6A7380);
            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) hover = n;
        }
        // details
        SkillNode n = selected == null ? null : ClientSkills.node(selected);
        int rx = left + W - 136, ry = top + 40;
        if (n == null) {
            for (FormattedCharSequence line : font.split(Component.literal("Pick a node to see it. Nodes with a double border are active skills: put them in a slot and use them with their key.\n\n" + path.description()), 130)) {
                g.drawString(font, line, rx, ry, WFTheme.MUTED, false);
                ry += 10;
            }
        } else {
            WFTheme.text(g, font, n.name(), rx, ry, WFTheme.opaque(path.color()));
            ry += 12;
            String kind = n.active() ? "Active · " + n.cooldown() + "s cooldown" + (n.charges() > 1 ? " · " + n.charges() + " charges" : "") : "Passive";
            WFTheme.text(g, font, kind, rx, ry, WFTheme.MUTED);
            ry += 12;
            for (FormattedCharSequence line : font.split(Component.literal(n.desc()), 130)) {
                g.drawString(font, line, rx, ry, WFTheme.TEXT, false);
                ry += 10;
            }
            ry += 4;
            String status = ClientSkills.has(n.id()) ? "Learned" : available(n) ? "Costs " + cost(n) + " point" + (cost(n) == 1 ? "" : "s")
                    : "Needs " + parentNames(n) + " first";
            for (FormattedCharSequence line : font.split(Component.literal(status), 130)) {
                g.drawString(font, line, rx, ry, ClientSkills.has(n.id()) ? 0xFF8FE07A : WFTheme.MUTED, false);
                ry += 10;
            }
        }
        if (hover != null) g.renderTooltip(font, Component.literal(hover.name() + (hover.active() ? " (active)" : "")), mouseX, mouseY);
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String w : name.replace("'", "").split(" ")) if (!w.isEmpty() && sb.length() < 2) sb.append(Character.toUpperCase(w.charAt(0)));
        if (sb.length() == 1 && name.length() > 1) sb.append(name.charAt(1));
        return sb.toString();
    }

    private static String parentNames(SkillNode n) {
        List<String> names = new ArrayList<>();
        for (String p : n.parents()) {
            SkillNode pn = ClientSkills.node(p);
            names.add(pn == null ? p : pn.name());
        }
        return String.join(" or ", names);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SkillPath path = path();
        if (path != null) {
            for (SkillNode n : path.nodes()) {
                int x = nodeX(n), y = nodeY(n);
                if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                    selected = n.id();
                    rebuildWidgets();
                    return true;
                }
            }
        }
        if (button == 1 && mouseY >= top + H - 26 && mouseY < top + H - 6) {
            for (int i = 0; i < 4; i++) {
                int x = left + 104 + i * 76;
                if (mouseX >= x && mouseX < x + 74 && !ClientSkills.slots().get(i).isEmpty()) {
                    send("slot", Integer.toString(i), "");
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (ClientSetup.SKILL_TREE.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
