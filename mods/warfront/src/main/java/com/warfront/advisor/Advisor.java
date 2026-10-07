package com.warfront.advisor;

import com.warfront.config.WFConfig;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.network.AdvisorLinePayload;
import com.warfront.registry.WFRegistry;
import com.warfront.world.BaseLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The advisor's first-hour quest line and his counsel afterwards. One step at a time: each finished step gives a
 * small reward, a toast and his next line. Progress lives on the player.
 */
public final class Advisor {
    /** Troops to summon for the SUMMON step. */
    public static final int SUMMONS_NEEDED = 3;

    private Advisor() {}

    public enum Step {
        PICK_RACE("Choose your race",
                "A new banner. Tell me what blood runs in you, and I will tell you what you may become."),
        WELL("Place a Mana Well",
                "Every war begins with power, and power must be kept somewhere. Place a Mana Well. I will watch over it."),
        BLOOM("Harvest a Manabloom",
                "Plant Manabloom seeds on farmland by water and harvest one when it glows. Small things, tended, grow great. I have always been patient."),
        ALTAR("Build the Summoning Altar",
                "Build the Summoning Altar: a 3x3 floor of bricks under it, a Mana Brazier two blocks out on each corner. Those who answer it fight for whoever calls."),
        SUMMON("Summon 3 troops",
                "Fill the well and summon three troops at the altar. An army is only a tool. What matters is the hand that holds it."),
        STANDARD("Plant a War Standard",
                "Plant a War Standard. It tells the world where you stand, and who to come for."),
        RAID("Survive your first raid",
                "They will come now. Hold the standard. Whatever you hear of the one who sends them, hold."),
        DONE("Done", "You know the way now. I will be here. I am never far.");

        public final String goal;
        public final String line;

        Step(String goal, String line) {
            this.goal = goal;
            this.line = line;
        }

        public static Step byOrdinal(int i) {
            Step[] v = values();
            return v[Math.max(0, Math.min(v.length - 1, i))];
        }
    }

    /** The advisor's guise per race (placeholder looks until the owner designs them). */
    public static String title(Race race) {
        return switch (race) {
            case HUMAN -> "Court Wizard";
            case ELF -> "Seer";
            case DWARF -> "Runekeeper";
            case ORC -> "Shaman";
            case DEMON -> "Whisperer";
            case ANGEL -> "Oracle";
            case HIVE -> "Synapse-Elder";
        };
    }

    public static Step step(Player player) {
        return Step.byOrdinal(player.getData(WFRegistry.QUEST_STEP));
    }

    public static boolean questOn() {
        return WFConfig.ADVISOR_QUEST.get();
    }

    // ------------------------------------------------------------------ progress

    /** The player did the thing {@code step} asks for. Advances the quest if that is the current step. */
    public static void complete(Player player, Step step) {
        if (player.level().isClientSide || step(player) != step || step == Step.DONE) return;
        if (step == Step.SUMMON) {
            int n = player.getData(WFRegistry.QUEST_COUNT) + 1;
            player.setData(WFRegistry.QUEST_COUNT, n);
            if (n < SUMMONS_NEEDED) {
                player.displayClientMessage(Component.literal("Troops summoned: " + n + "/" + SUMMONS_NEEDED)
                        .withStyle(ChatFormatting.GOLD), true);
                return;
            }
        }
        reward(player, step);
        setStep(player, Step.byOrdinal(step.ordinal() + 1));
    }

    /** Moves the quest to {@code step} and says its line. */
    public static void setStep(Player player, Step step) {
        player.setData(WFRegistry.QUEST_STEP, step.ordinal());
        player.setData(WFRegistry.QUEST_COUNT, 0);
        say(player, step == Step.DONE ? "Your first hour is done" : "Next: " + step.goal, step.line);
    }

    /** "I know the way": skips the rest of the quest line. */
    public static void skip(Player player) {
        player.setData(WFRegistry.QUEST_STEP, Step.DONE.ordinal());
        say(player, "As you wish", "Then go. You know where to find me.");
    }

    private static void reward(Player player, Step step) {
        ItemStack gift = switch (step) {
            case WELL, ALTAR -> new ItemStack(WFRegistry.MANA_SHARD.get(), 8);
            case BLOOM -> new ItemStack(WFRegistry.MANA_CRYSTAL.get(), 1);
            case SUMMON -> new ItemStack(WFRegistry.ARROW_TOWER_ITEM.get());
            case STANDARD -> new ItemStack(WFRegistry.MANA_CRYSTAL.get(), 2);
            case RAID -> new ItemStack(WFRegistry.HEALING_SHRINE_ITEM.get());
            default -> ItemStack.EMPTY;
        };
        if (step == Step.RAID) give(player, new ItemStack(WFRegistry.MANA_CRYSTAL.get(), 4));
        if (!gift.isEmpty()) give(player, gift);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /** A line from the advisor: in chat, and as a toast. */
    public static void say(Player player, String title, String line) {
        Race race = Race.byId(player.getData(WFRegistry.RACE));
        String who = race == null ? "Advisor" : title(race);
        player.sendSystemMessage(Component.literal(who + ": ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(line).withStyle(ChatFormatting.YELLOW).withStyle(s -> s.withBold(false))));
        if (player instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, new AdvisorLinePayload(title, line));
    }

    // ------------------------------------------------------------------ the advisor himself

    @Nullable
    public static AdvisorEntity find(ServerPlayer player) {
        String id = player.getData(WFRegistry.ADVISOR);
        if (id.isEmpty()) return null;
        try {
            Entity e = player.serverLevel().getEntity(UUID.fromString(id));
            return e instanceof AdvisorEntity a && a.isAlive() ? a : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Makes sure the player has an advisor, spawning one beside {@code near} if he is missing. */
    public static AdvisorEntity ensure(ServerPlayer player, BlockPos near) {
        AdvisorEntity a = find(player);
        if (a != null) return a;
        return spawn(player, near);
    }

    /** Spawns a fresh advisor beside {@code near}, replacing the old one if loaded. */
    public static AdvisorEntity spawn(ServerPlayer player, BlockPos near) {
        AdvisorEntity old = find(player);
        if (old != null) old.discard();
        ServerLevel level = player.serverLevel();
        AdvisorEntity a = WFRegistry.ADVISOR_ENTITY.get().create(level);
        if (a == null) throw new IllegalStateException("advisor entity type failed to create");
        BlockPos spot = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.offset(2, 0, 1));
        a.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot() + 180F, 0F);
        a.setOwner(player.getUUID());
        Race race = Race.byId(player.getData(WFRegistry.RACE));
        a.setDisguise(race == null ? Race.HUMAN : race);
        level.addFreshEntity(a);
        player.setData(WFRegistry.ADVISOR, a.getUUID().toString());
        return a;
    }

    /** The player picked (or switched) race: the advisor takes that race's guise, and the first step is done. */
    public static void onRaceChosen(ServerPlayer player) {
        Race race = Race.byId(player.getData(WFRegistry.RACE));
        AdvisorEntity a = find(player);
        if (a != null && race != null) a.setDisguise(race);
        complete(player, Step.PICK_RACE);
    }

    /** First login: the advisor arrives and, if the quest is on, gives his first line. */
    public static void onLogin(ServerPlayer player) {
        if (!player.getData(WFRegistry.ADVISOR).isEmpty()) return;
        spawn(player, player.blockPosition());
        if (!questOn()) {
            player.setData(WFRegistry.QUEST_STEP, Step.DONE.ordinal());
            return;
        }
        Step start = Race.byId(player.getData(WFRegistry.RACE)) != null ? Step.WELL : Step.PICK_RACE;
        setStep(player, start);
    }

    /** Moves the advisor to the player's base the first time it is founded. */
    public static void moveTo(ServerPlayer player, BlockPos base) {
        AdvisorEntity a = find(player);
        if (a == null) {
            spawn(player, base);
            return;
        }
        BlockPos spot = player.serverLevel().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.offset(2, 0, 1));
        a.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
    }

    /** Right-clicking the advisor. */
    public static void talk(Player player, AdvisorEntity advisor) {
        if (advisor.getOwner() != null && !advisor.getOwner().equals(player.getUUID())) {
            player.displayClientMessage(Component.literal("He looks through you, waiting for someone else.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Step step = step(player);
        if (step != Step.DONE) {
            say(player, step.goal, step.line);
            MutableComponent skip = Component.literal("[I know the way]").withStyle(s -> s.withColor(ChatFormatting.GRAY)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warfront advisor skip")));
            player.sendSystemMessage(skip);
            return;
        }
        if (com.warfront.war.Bounties.deliver(player, player.getMainHandItem())) return;
        say(player, "Counsel", counsel(player, advisor));
        for (String line : com.warfront.war.Bounties.lines(player)) {
            player.sendSystemMessage(Component.literal("  Bounty: " + line).withStyle(ChatFormatting.GRAY));
        }
    }

    /** After the first hour: what to build next, and war news. */
    static String counsel(Player player, AdvisorEntity advisor) {
        String key = Factions.keyOf(player.getServer(), player);
        BaseLevel.Status base = BaseLevel.of(player.level(), advisor.blockPosition(), key);
        String next;
        if (base.buildings() == 0 && !base.isTest()) {
            next = "Your wells are far from here. Build near me, and I can tell you more.";
        } else if (base.level() < BaseLevel.MAX_LEVEL) {
            next = "Your base stands at level " + base.level() + ". To raise it, " + base.missingFor(base.level() + 1)
                    + ". Higher levels call Captains, Champions and more war beasts.";
        } else {
            next = "Your base is as strong as any I have seen. Stronger than I expected.";
        }
        NpcFaction rumour = NpcFaction.pick(player.level().getBiome(player.blockPosition()), player.level(), player.getRandom());
        return next + " Scouts say the " + rumour.displayName + " are gathering. They will not wait long.";
    }
}
