package com.warfront.combat;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.registry.WFRegistry;
import com.warfront.world.HiveAdaptation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hive race power: Call the Swarm. The Swarm meter (0-100) fills from kills, twice as fast underground or in the dark.
 * At 50 the power key makes a Brood Call (two Lancer-Drones and two Rippers burrow up); at 100, sneak + the key makes a
 * Deepmaw Call (one Deepmaw). Called units fight this battle only: outside the army size and beast limits, no loot or
 * XP, no duty, never fallen heroes, and they burrow away 20 seconds after the fighting stops.
 */
public final class SwarmCall {
    public static final int MAX = 100;
    public static final int BROOD = 50;
    public static final int PLAYER_KILL = 8;
    public static final int UNIT_KILL = 3;
    public static final int UNIT_RANGE = 32;
    public static final int IDLE_TICKS = 1200;
    public static final int LEAVE_SECONDS = 20;

    private static final String KEY = "warfront_swarm";
    private static final String COMBAT_KEY = "warfront_swarm_combat";
    private static final String IDLE_KEY = "warfront_swarm_idle";
    private static final String CAPTAIN_KEY = "warfront_brood_called";

    private SwarmCall() {}

    public static int current(LivingEntity e) {
        return e.getPersistentData().getInt(KEY);
    }

    public static void set(LivingEntity e, int value) {
        e.getPersistentData().putInt(KEY, Mth.clamp(value, 0, MAX));
    }

    /** Underground or in the dark: the Swarm answers twice as fast. */
    public static boolean deep(LivingEntity e) {
        BlockPos p = e.blockPosition();
        return HiveAdaptation.underground(e) || e.level().getMaxLocalRawBrightness(p) < 7
                && e.level().getBrightness(LightLayer.SKY, p) == 0;
    }

    public static void gain(Player p, int amount, boolean deep) {
        if (Race.of(p) != Race.HIVE) return;
        p.getPersistentData().putLong(COMBAT_KEY, p.level().getGameTime());
        int before = current(p);
        set(p, before + (deep ? amount * 2 : amount));
        if (p instanceof ServerPlayer sp) {
            if (before < BROOD && current(p) >= BROOD) sp.displayClientMessage(Component.literal(
                    "The brood stirs: press R for a Brood Call.").withStyle(ChatFormatting.DARK_AQUA), true);
            if (before < MAX && current(p) >= MAX) sp.displayClientMessage(Component.literal(
                    "The Deepmaw wakes: sneak + R to call it.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        }
    }

    public static void creditKill(LivingEntity killer, LivingEntity victim) {
        if (killer == victim) return;
        if (killer instanceof Player p) {
            if (Factions.relation(p, victim) == Relation.ENEMY) gain(p, PLAYER_KILL, deep(p));
        } else if (killer instanceof SoldierEntity unit && unit.getOwnerUUID() != null && killer.level().getServer() != null) {
            Player owner = killer.level().getServer().getPlayerList().getPlayer(unit.getOwnerUUID());
            if (owner != null && owner.level() == unit.level() && owner.distanceTo(unit) <= UNIT_RANGE) gain(owner, UNIT_KILL, deep(owner));
        }
    }

    /** Once a second for Hive players: drains a point a second after a minute without a fight. */
    public static void secondTick(ServerPlayer p) {
        if (Race.of(p) != Race.HIVE || current(p) == 0) return;
        if (p.level().getGameTime() - p.getPersistentData().getLong(COMBAT_KEY) > IDLE_TICKS) set(p, current(p) - 1);
    }

    /** The power key: a Brood Call at 50+, or (sneaking, at 100) a Deepmaw Call. Returns the units called. */
    public static List<SoldierEntity> call(Player p, boolean sneaking) {
        int meter = current(p);
        if (sneaking && meter >= MAX) {
            set(p, 0);
            return summon(p, List.of(SoldierRole.BEAST));
        }
        if (meter >= BROOD) {
            set(p, meter - BROOD);
            return summon(p, List.of(SoldierRole.SPEARMAN, SoldierRole.SPEARMAN, SoldierRole.SWORDSMAN, SoldierRole.SWORDSMAN));
        }
        if (p instanceof ServerPlayer sp) RacePower.say(sp, "Swarm " + meter + "/" + MAX + ": a Brood Call needs " + BROOD + ".");
        return List.of();
    }

    private static List<SoldierEntity> summon(Player p, List<SoldierRole> roles) {
        List<SoldierEntity> out = new ArrayList<>();
        if (!(p.level() instanceof ServerLevel level)) return out;
        for (int i = 0; i < roles.size(); i++) {
            SoldierEntity s = WFRegistry.SOLDIER.get().create(level);
            if (s == null) continue;
            double a = i * Mth.TWO_PI / roles.size() + level.random.nextFloat() * 0.5;
            double r = roles.get(i) == SoldierRole.BEAST ? 3.0 : 2.0;
            s.moveTo(p.getX() + Math.cos(a) * r, p.getY(), p.getZ() + Math.sin(a) * r, p.getYRot(), 0F);
            s.setupAsRecruit(p, roles.get(i), Race.HIVE);
            s.setSwarmCalled(true);
            level.addFreshEntity(s);
            dig(level, s.position());
            out.add(s);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 0.8F, 1.3F);
        return out;
    }

    /** Dirt and sculk bursting up (or settling back) where a unit burrows. */
    private static void dig(ServerLevel level, Vec3 at) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), at.x, at.y + 0.2, at.z,
                24, 0.4, 0.2, 0.4, 0.1);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), at.x, at.y + 0.2, at.z,
                16, 0.4, 0.2, 0.4, 0.1);
    }

    /**
     * One second of a called unit's life: when neither it nor its commander has fought for 20 seconds, or the
     * commander is gone, it burrows away. Returns true if it left.
     */
    public static boolean idleTick(SoldierEntity s, boolean fighting, boolean ownerHere) {
        int idle = fighting ? 0 : s.getPersistentData().getInt(IDLE_KEY) + 1;
        s.getPersistentData().putInt(IDLE_KEY, idle);
        if (ownerHere && idle < LEAVE_SECONDS) return false;
        if (s.level() instanceof ServerLevel level) {
            dig(level, s.position());
            level.playSound(null, s.blockPosition(), SoundEvents.WARDEN_DIG, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
        s.discard();
        return true;
    }

    /** Every second: called units check whether the battle is over. */
    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (ServerLevel level : server.getAllLevels()) {
            for (SoldierEntity s : level.getEntities(WFRegistry.SOLDIER.get(), SoldierEntity::isSwarmCalled)) {
                UUID ownerId = s.getOwnerUUID();
                ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
                boolean here = owner != null && owner.isAlive() && owner.level() == level;
                boolean fighting = s.getTarget() != null || level.getGameTime() - s.getLastHurtByMobTimestamp() < 100
                        || here && (level.getGameTime() - owner.getLastHurtByMobTimestamp() < 100
                        || owner.getLastHurtMob() != null && level.getGameTime() - owner.getLastHurtMobTimestamp() < 100);
                idleTick(s, fighting, here);
            }
        }
    }

    /** An enemy Swarm raid captain, badly hurt, calls a brood once per raid. */
    public static void captainCall(SoldierEntity captain) {
        if (captain.getHealth() > captain.getMaxHealth() / 2 || captain.getPersistentData().getBoolean(CAPTAIN_KEY)) return;
        NpcFaction faction = NpcFaction.byKey(captain.getFactionKey());
        if (faction != NpcFaction.THE_SWARM || !(captain.level() instanceof ServerLevel level) || captain.getWarbandId() == null) return;
        captain.getPersistentData().putBoolean(CAPTAIN_KEY, true);
        SoldierRole[] roles = {SoldierRole.SPEARMAN, SoldierRole.SPEARMAN, SoldierRole.SWORDSMAN, SoldierRole.SWORDSMAN};
        for (int i = 0; i < roles.length; i++) {
            SoldierEntity s = WFRegistry.SOLDIER.get().create(level);
            if (s == null) continue;
            double a = i * Mth.TWO_PI / roles.length;
            s.moveTo(captain.getX() + Math.cos(a) * 2, captain.getY(), captain.getZ() + Math.sin(a) * 2, captain.getYRot(), 0F);
            s.setupAsRaider(faction, roles[i], captain.getWarbandId(), captain.currentObjective(), null, 1);
            level.addFreshEntity(s);
            dig(level, s.position());
        }
        level.playSound(null, captain.blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.0F, 1.2F);
    }
}
