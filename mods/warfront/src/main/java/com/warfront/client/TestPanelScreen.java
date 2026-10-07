package com.warfront.client;

import com.warfront.army.SoldierRole;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.network.TestActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The Test Panel (F8 with test mode on). Four tabs: Spawn, Base, Siege and Player. Every button sends the same words
 * {@code /wftest} takes; the server checks test mode and operator rights before acting. Features not built yet show
 * as greyed "coming" buttons.
 */
public class TestPanelScreen extends Screen {
    private static final int W = 360, H = 214, BW = 110, BH = 20, GAP = 6;
    private static final String[] TABS = {"Spawn", "Base", "Siege", "Player", "Advisor"};
    private static final List<SoldierRole> ROLES = Arrays.stream(SoldierRole.values())
            .filter(r -> !r.retired() && r != SoldierRole.BEAST).toList();

    // Remembered between openings.
    private static int tab;
    private static int race;
    private static int role;
    private static int count = 1;
    /** -1 = friendly, else an NpcFaction ordinal. */
    private static int side = -1;
    private static int baseLevel = 1;
    private static int wave = 5;
    /** -1 = any faction (picked by the biome). */
    private static int siegeFaction = -1;
    private static boolean infinite;
    private static int questStep;
    private static int preset = 1;

    private int left, top;

    public TestPanelScreen() {
        super(Component.literal("Test Panel"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int tw = (W - 2 * GAP) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            Button b = Button.builder(Component.literal(TABS[i]), x -> {
                tab = t;
                rebuildWidgets();
            }).bounds(left + GAP + i * tw, top + 22, tw - 2, BH).build();
            b.active = i != tab;
            addRenderableWidget(b);
        }
        switch (tab) {
            case 0 -> spawnTab();
            case 1 -> baseTab();
            case 2 -> siegeTab();
            case 3 -> playerTab();
            default -> advisorTab();
        }
    }

    // ------------------------------------------------------------------ layout helpers

    private int col(int c) {
        return left + GAP + 6 + c * (BW + GAP);
    }

    private int row(int r) {
        return top + 52 + r * (BH + 4);
    }

    private Button add(int c, int r, String label, Runnable onPress) {
        Button b = Button.builder(Component.literal(label), x -> onPress.run()).bounds(col(c), row(r), BW, BH).build();
        addRenderableWidget(b);
        return b;
    }

    private Button send(int c, int r, String label, String... args) {
        return add(c, r, label, () -> PacketDistributor.sendToServer(new TestActionPayload(new ArrayList<>(List.of(args)))));
    }

    private void coming(int c, int r, String label, String why) {
        Button b = add(c, r, label + " (coming)", () -> {});
        b.active = false;
        b.setTooltip(Tooltip.create(Component.literal(why)));
    }

    private Button cycle(int c, int r, String label, Runnable next) {
        return add(c, r, label, () -> {
            next.run();
            rebuildWidgets();
        });
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ tabs

    private void spawnTab() {
        Race r = Race.values()[race];
        SoldierRole ro = ROLES.get(role % ROLES.size());
        String sideName = side < 0 ? "Friendly" : NpcFaction.values()[side].displayName;
        cycle(0, 0, "Race: " + r.displayName(), () -> race = (race + 1) % Race.values().length);
        cycle(1, 0, "Role: " + ro.displayName(), () -> role = (role + 1) % ROLES.size());
        cycle(2, 0, "Count: " + count, () -> count = count % 10 + 1);
        cycle(0, 1, sideName, () -> side = side + 1 >= NpcFaction.values().length ? -1 : side + 1)
                .setTooltip(Tooltip.create(Component.literal("Friendly units join your army. A faction spawns its raiders (they use the faction's race).")));
        String sideArg = side < 0 ? "friendly" : lower(NpcFaction.values()[side]);
        send(1, 1, "Spawn", "spawn", r.id(), ro.id(), Integer.toString(count), sideArg);
        send(2, 1, "War beast", "beast", r.id(), "1", sideArg)
                .setTooltip(Tooltip.create(Component.literal("Ignores the beast limit in test mode. Only races with a beast have a model for it.")));
        send(0, 3, "Heal all", "army", "heal");
        send(1, 3, "Kill all", "army", "kill");
        send(2, 3, "Dismiss all", "army", "dismiss");
        coming(0, 5, "Rank", "Pick a unit's rank once veterancy is built.");
        coming(1, 5, "Revive heroes", "Revive fallen heroes once mortal units are built.");
    }

    private void baseTab() {
        cycle(0, 0, "Base level: " + baseLevel, () -> baseLevel = baseLevel % 5 + 1);
        send(1, 0, "Set level (TEST)", "base", "level", Integer.toString(baseLevel));
        send(2, 0, "Clear TEST level", "base", "level", "0");
        send(0, 1, "Fill mana", "base", "fill")
                .setTooltip(Tooltip.create(Component.literal("Fills every Mana Well within 32 blocks.")));
        add(1, 1, "Infinite mana: " + (infinite ? "ON" : "OFF"), () -> {
            infinite = !infinite;
            PacketDistributor.sendToServer(new TestActionPayload(new ArrayList<>(List.of("base", "infinite", infinite ? "on" : "off"))));
            rebuildWidgets();
        });
        send(0, 2, "Fill Mess Hall", "base", "food", "fill");
        send(1, 2, "Empty Mess Hall", "base", "food", "empty");
        coming(0, 4, "New bounties", "Advisor bounties are not built yet.");
        coming(1, 4, "Raider camp", "Raider camps are not built yet.");
        coming(2, 4, "Merchant", "The traveling merchant is not built yet.");
        send(0, 3, "Starter base", "base", "starter")
                .setTooltip(Tooltip.create(Component.literal("Clears a flat spot in front of you and builds a Mana Well, two Pylons, "
                        + "a Summoning Altar, a War Standard and an Arrow Tower.")));
    }

    private void siegeTab() {
        String f = siegeFaction < 0 ? "Any faction" : NpcFaction.values()[siegeFaction].displayName;
        cycle(0, 0, f, () -> siegeFaction = siegeFaction + 1 >= NpcFaction.values().length ? -1 : siegeFaction + 1);
        if (siegeFaction < 0) send(1, 0, "Start wave now", "siege", "start");
        else send(1, 0, "Start wave now", "siege", "start", lower(NpcFaction.values()[siegeFaction]));
        cycle(0, 1, "Wave: " + wave, () -> wave = wave >= 30 ? 1 : wave < 5 ? wave + 1 : wave + 5);
        send(1, 1, "Jump to wave", "siege", "wave", Integer.toString(wave));
        send(0, 2, "Pause", "siege", "pause");
        send(1, 2, "Resume", "siege", "resume");
        send(2, 2, "End siege", "siege", "end");
        String[] presets = {"easy", "normal", "hard", "warlord"};
        cycle(0, 3, "Preset: " + presets[preset], () -> preset = (preset + 1) % presets.length);
        send(1, 3, "Set difficulty", "difficulty", presets[preset]);
        send(2, 3, "Skip grace", "raid", "grace");
        send(0, 4, "Raid warning now", "raid", "warn");
        send(1, 4, "Siege warning now", "raid", "siege");
        send(2, 4, "Recall now", "raid", "recall")
                .setTooltip(Tooltip.create(Component.literal("Teleports you and your followers to your War Standard at once.")));
        coming(0, 5, "Raise outpost", "Siege outposts are not built yet.");
        coming(1, 5, "Destroy outpost", "Siege outposts are not built yet.");
    }

    private void playerTab() {
        Race r = Race.values()[race];
        cycle(0, 0, "Race: " + r.displayName(), () -> race = (race + 1) % Race.values().length);
        send(1, 0, "Become " + r.displayName(), "player", "race", r.id());
        send(0, 1, "Fill souls/rage/glider", "player", "fill");
        send(0, 2, "Day", "player", "day");
        send(1, 2, "Night", "player", "night");
        send(2, 2, "Clear weather", "player", "clear");
        send(0, 3, "God mode", "player", "god")
                .setTooltip(Tooltip.create(Component.literal("Toggles: invulnerable, with creative-style flight.")));
        send(1, 3, "Give kit", "player", "kit");
        coming(0, 5, "Rocket fuel", "Comes with the orc rocket pack.");
        coming(1, 5, "Spell mana", "Comes with the Iron's Spells link.");
    }

    private void advisorTab() {
        var step = com.warfront.advisor.Advisor.Step.byOrdinal(questStep);
        cycle(0, 0, "Step " + questStep + ": " + (step.goal.length() > 11 ? step.goal.substring(0, 10) + "." : step.goal), () -> questStep = (questStep + 1) % com.warfront.advisor.Advisor.Step.values().length);
        send(1, 0, "Jump to step", "advisor", "step", Integer.toString(questStep))
                .setTooltip(Tooltip.create(Component.literal(step.goal)));
        send(0, 1, "Respawn advisor", "advisor", "respawn");
        Race r = Race.values()[race];
        cycle(0, 2, "Guise: " + r.displayName(), () -> race = (race + 1) % Race.values().length);
        send(1, 2, "Set guise", "advisor", "disguise", r.id());
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, 0xE0141820);
        g.renderOutline(left, top, W, H, 0xFF3A4A60);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, left + 8, top + 8, 0x7FD8FF);
        g.drawString(font, Component.literal("TEST MODE"), left + W - 8 - font.width("TEST MODE"), top + 8, 0xFF8060);
        String hint = switch (tab) {
            case 0 -> "Army tools act on all your units within 128 blocks.";
            case 1 -> "Acts on the mana networks within 32 blocks.";
            case 2 -> "Acts on the nearest War Standard within 128 blocks.";
            case 4 -> "Your advisor's quest line and his look.";
            default -> "Same as /wftest player ...";
        };
        g.drawString(font, Component.literal(hint), left + 8, top + H - 14, 0x8090A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
