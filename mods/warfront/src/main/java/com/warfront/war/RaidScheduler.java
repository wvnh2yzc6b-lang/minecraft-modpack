package com.warfront.war;

import com.warfront.advisor.Advisor;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.config.WFConfig;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.network.AdvisorLinePayload;
import com.warfront.registry.WFRegistry;
import com.warfront.world.BaseLevel;
import com.warfront.world.GameEvents;
import com.warfront.world.WarbandSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The war's rhythm. Each player has a raid clock: about one raid a day (by preset), a siege on their War Standard
 * every few nights, and a quiet day after every two raids. Nothing comes during the grace period. The clock runs
 * slower while the player is in another dimension than their base. Every attack is announced two minutes ahead with
 * a toast, a horn and a countdown bar, wherever the player is, and they may recall home once per attack.
 */
public final class RaidScheduler {
    public static final int DAY = 24000;
    /** How long after it hits an attack still counts as underway, for the recall. */
    private static final int ACTIVE_TICKS = 6000;
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private RaidScheduler() {}

    /** Whether the grace period is over: enough days passed and the advisor's quest got to the first raid. */
    public static boolean graceOver(long day, int graceDays, int questStep) {
        return day >= graceDays && questStep >= Advisor.Step.RAID.ordinal();
    }

    public static boolean inGrace(ServerPlayer player, WarState war) {
        if (war.graceSkipped()) return false;
        long day = player.server.overworld().getDayTime() / DAY;
        return !graceOver(day, WFConfig.GRACE_DAYS.get(), player.getData(WFRegistry.QUEST_STEP));
    }

    /** Remembers the player's base: the War Standard they planted last. */
    public static void setHome(ServerPlayer player, BlockPos pos) {
        WarState.Clock c = WarState.get(player.server).clock(player.getUUID());
        c.home = pos.immutable();
        c.homeDim = player.level().dimension();
    }

    /** Runs once a second. */
    public static void tick(MinecraftServer server) {
        WarState war = WarState.get(server);
        long now = server.overworld().getGameTime();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || Race.byId(p.getData(WFRegistry.RACE)) == null) continue;
            WarState.Clock c = war.clock(p.getUUID());
            if (c.pending != WarState.Pending.NONE) {
                updateBar(p, c, now);
                if (now >= c.hitsAt) strike(p, c, now);
                continue;
            }
            if (c.activeUntil > now || p.isCreative() || inGrace(p, war)) continue;
            WarState.Preset preset = war.preset();
            boolean away = c.homeDim != null && p.level().dimension() != c.homeDim;
            double rate = 20.0 * (away ? WFConfig.AWAY_RAID_RATE.get() : 1.0);
            RandomSource r = p.getRandom();
            if (WFConfig.WARBANDS_ENABLED.get() && preset.raidsPerDay() > 0) {
                if (c.raidDue < 0) c.raidDue = nextRaidDelay(c, preset, r);
                c.raidProgress += rate;
                if (c.raidProgress >= c.raidDue) {
                    c.raidProgress = 0;
                    c.raidDue = nextRaidDelay(c, preset, r);
                    warn(p, c, WarState.Pending.RAID, null, now);
                    continue;
                }
            }
            if (WFConfig.NATURAL_SIEGES.get() && c.home != null) {
                if (c.siegeDue < 0) c.siegeDue = nextSiegeDelay(preset, r);
                c.siegeProgress += rate;
                if (c.siegeProgress >= c.siegeDue) {
                    c.siegeProgress = 0;
                    c.siegeDue = nextSiegeDelay(preset, r);
                    warn(p, c, WarState.Pending.SIEGE, null, now);
                }
            }
        }
    }

    private static double nextRaidDelay(WarState.Clock c, WarState.Preset preset, RandomSource r) {
        double delay = DAY / preset.raidsPerDay() * (0.6 + 0.8 * r.nextDouble());
        if (++c.raidsInARow >= 2) {
            c.raidsInARow = 0;
            delay += DAY;   // a quiet day after every two raids
        }
        return delay;
    }

    private static double nextSiegeDelay(WarState.Preset preset, RandomSource r) {
        int min = WFConfig.SIEGE_MIN_DAYS.get(), max = Math.max(min, WFConfig.SIEGE_MAX_DAYS.get());
        double days = min + r.nextDouble() * (max - min + 1);
        return DAY * days / Math.max(0.1, preset.raidsPerDay());
    }

    /** Announces an attack, two minutes out. {@code faction} null picks one. */
    public static void warn(ServerPlayer p, WarState.Clock c, WarState.Pending type, @Nullable NpcFaction faction, long now) {
        if (type == WarState.Pending.SIEGE && c.home == null) type = WarState.Pending.RAID;
        ServerLevel at = homeLevel(p, c);
        BlockPos where = at != null && c.home != null ? c.home : p.blockPosition();
        ServerLevel biomeLevel = at != null ? at : p.serverLevel();
        NpcFaction f = faction != null ? faction : NpcFaction.pick(biomeLevel.getBiome(where), biomeLevel, p.getRandom());
        c.pending = type;
        c.faction = f.name();
        c.hitsAt = now + WFConfig.RAID_WARNING.get();
        c.recallUsed = false;
        if (at != null && c.home != null) forceChunks(at, c.home, true);
        String what = type == WarState.Pending.SIEGE ? "A siege" : "A raid";
        String target = c.home != null ? "your base" : "you";
        int secs = WFConfig.RAID_WARNING.get() / 20;
        String line = "The " + f.displayName + " march on " + target + ". " + secs / 60 + ":" + String.format("%02d", secs % 60)
                + (c.home != null ? ". Press H (or /warfront home) to recall." : ".");
        p.sendSystemMessage(Component.literal(what + " is coming! " + line).withStyle(f.color, ChatFormatting.BOLD));
        PacketDistributor.sendToPlayer(p, new AdvisorLinePayload(what + " is coming!", line));
        p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
        updateBar(p, c, now);
    }

    private static void updateBar(ServerPlayer p, WarState.Clock c, long now) {
        ServerBossEvent bar = BARS.computeIfAbsent(p.getUUID(), k ->
                new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS));
        long left = Math.max(0, c.hitsAt - now) / 20;
        NpcFaction f = faction(c);
        bar.setName(Component.literal((c.pending == WarState.Pending.SIEGE ? "Siege" : "Raid") + " in "
                + left / 60 + ":" + String.format("%02d", left % 60) + "  |  " + (f == null ? "" : f.displayName)
                + (c.home != null && !c.recallUsed ? "  |  H: recall home" : "")));
        bar.setProgress(Mth.clamp(left * 20F / WFConfig.RAID_WARNING.get(), 0F, 1F));
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
    }

    private static void clearBar(UUID player) {
        ServerBossEvent bar = BARS.remove(player);
        if (bar != null) bar.removeAllPlayers();
    }

    @Nullable
    private static NpcFaction faction(WarState.Clock c) {
        try {
            return NpcFaction.valueOf(c.faction);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static ServerLevel homeLevel(ServerPlayer p, WarState.Clock c) {
        return c.homeDim == null ? null : p.server.getLevel(c.homeDim);
    }

    private static void forceChunks(ServerLevel level, BlockPos home, boolean on) {
        int cx = home.getX() >> 4, cz = home.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.setChunkForced(cx + dx, cz + dz, on);
        }
    }

    /** The warned attack hits now. */
    private static void strike(ServerPlayer p, WarState.Clock c, long now) {
        clearBar(p.getUUID());
        NpcFaction f = faction(c);
        if (f == null) f = NpcFaction.MARAUDERS;
        WarState.Pending type = c.pending;
        c.pending = WarState.Pending.NONE;
        c.activeUntil = now + ACTIVE_TICKS;
        ServerLevel home = homeLevel(p, c);
        if (home == null || c.home == null) {
            GameEvents.trySpawnWarband(p);
            return;
        }
        if (!(home.getBlockEntity(c.home) instanceof WarStandardBlockEntity standard)) {
            forceChunks(home, c.home, false);
            c.home = null;   // the standard is gone: the raid finds the player instead
            c.homeDim = null;
            GameEvents.trySpawnWarband(p);
            return;
        }
        if (type != WarState.Pending.SIEGE || !standard.startSiege(home, f)) raidBase(home, c.home, f, p);
        // The base stays loaded while the attack plays out, so towers and guards fight even with the player away.
        ACTIVE_HOMES.put(p.getUUID(), new ActiveHome(home, c.home, now + ACTIVE_TICKS));
    }

    private record ActiveHome(ServerLevel level, BlockPos pos, long until) {}

    /** Bases kept loaded during an attack. */
    private static final Map<UUID, ActiveHome> ACTIVE_HOMES = new HashMap<>();

    /** Releases bases whose attack is over. Runs once a second. */
    public static void releaseHomes(long now) {
        ACTIVE_HOMES.entrySet().removeIf(e -> {
            ActiveHome h = e.getValue();
            if (h.until() > now) return false;
            forceChunks(h.level(), h.pos(), false);
            return true;
        });
    }

    /** A warband marching on the base from 40 blocks out. */
    private static void raidBase(ServerLevel level, BlockPos home, NpcFaction f, ServerPlayer p) {
        float angle = level.random.nextFloat() * Mth.TWO_PI;
        int x = home.getX() + Mth.floor(Mth.cos(angle) * 40), z = home.getZ() + Mth.floor(Mth.sin(angle) * 40);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        String key = Factions.keyOf(level.getServer(), p);
        int baseLevel = BaseLevel.of(level, home, key).level();
        WarbandSpawner.spawn(level, f, WarbandSpawner.raidComposition(level.random, f, baseLevel,
                WarState.get(level.getServer()).preset()), ground, Vec3.atBottomCenterOf(home), null, Math.min(4, baseLevel));
        p.sendSystemMessage(Component.literal("The " + f.displayName + " have reached your base!").withStyle(f.color, ChatFormatting.BOLD));
    }

    public static void onLogout(UUID player) {
        clearBar(player);
    }
}
