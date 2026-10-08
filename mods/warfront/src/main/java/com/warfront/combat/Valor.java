package com.warfront.combat;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Human race power: Rally the Banner. Valor (0-100) comes from the player's kills (+10), their units' kills nearby (+4)
 * and siege waves survived (+2); it drains only after 30 s out of combat. At 100 the power key Rallies: allies within
 * 16 blocks heal 30% of their health, hit 20% harder, take less damage and stop routing for 10 seconds.
 */
public final class Valor {
    public static final int MAX = 100;
    public static final int PLAYER_KILL = 10;
    public static final int UNIT_KILL = 4;
    public static final int WAVE = 2;
    public static final int UNIT_RANGE = 32;
    public static final int IDLE_TICKS = 600;
    public static final int DRAIN = 2;
    public static final double RADIUS = 16;
    public static final int RALLY_TICKS = 200;
    public static final float HEAL = 0.3F;

    private static final String KEY = "warfront_valor";
    private static final String COMBAT_KEY = "warfront_valor_combat";

    private Valor() {}

    public static int current(LivingEntity e) {
        return e.getPersistentData().getInt(KEY);
    }

    public static void set(LivingEntity e, int value) {
        e.getPersistentData().putInt(KEY, Mth.clamp(value, 0, MAX));
    }

    public static void gain(Player p, int amount) {
        if (Race.of(p) != Race.HUMAN) return;
        CompoundTag d = p.getPersistentData();
        d.putLong(COMBAT_KEY, p.level().getGameTime());
        int before = current(p);
        set(p, before + amount);
        if (p instanceof ServerPlayer sp && current(p) != before) {
            sp.displayClientMessage(current(p) >= MAX ? Component.literal("Valor full: press the race power key to Rally!")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) : bar(current(p)), true);
        }
    }

    /** A kill: credit the human who made it, or whose unit made it within 32 blocks. */
    public static void creditKill(LivingEntity killer, LivingEntity victim) {
        if (killer == victim) return;
        if (killer instanceof Player p) {
            if (Factions.relation(p, victim) == Relation.ENEMY || victim instanceof net.minecraft.world.entity.monster.Enemy) gain(p, PLAYER_KILL);
        } else if (killer instanceof SoldierEntity unit && unit.getOwnerUUID() != null && killer.level().getServer() != null) {
            creditUnitKill(killer.level().getServer().getPlayerList().getPlayer(unit.getOwnerUUID()), unit);
        }
    }

    /** One of the player's units made a kill: Valor if the player is within 32 blocks. */
    public static void creditUnitKill(@org.jetbrains.annotations.Nullable Player owner, SoldierEntity unit) {
        if (owner != null && owner.level() == unit.level() && owner.distanceTo(unit) <= UNIT_RANGE) gain(owner, UNIT_KILL);
    }

    /** Once a second for human players: drains after 30 s without a fight. */
    public static void secondTick(ServerPlayer p) {
        if (Race.of(p) != Race.HUMAN || current(p) == 0) return;
        long since = p.level().getGameTime() - p.getPersistentData().getLong(COMBAT_KEY);
        if (since > IDLE_TICKS) set(p, current(p) - DRAIN);
    }

    /** The Rally's reach: 16 blocks, a quarter more with a human Captain or Champion of the player's army close by. */
    public static double radius(Player p) {
        boolean officer = !p.level().getEntitiesOfClass(SoldierEntity.class, p.getBoundingBox().inflate(RADIUS),
                s -> p.getUUID().equals(s.getOwnerUUID()) && s.getRace() == Race.HUMAN
                        && (s.getRole() == SoldierRole.CAPTAIN || s.getRole() == SoldierRole.CHAMPION)).isEmpty();
        return officer ? RADIUS * 1.25 : RADIUS;
    }

    /** Rallies if Valor is full; returns how many allies it reached (-1 if it wasn't ready). */
    public static int rally(Player p) {
        if (current(p) < MAX || !(p.level() instanceof ServerLevel level)) return -1;
        set(p, 0);
        double r = radius(p);
        List<LivingEntity> allies = new java.util.ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && e.distanceTo(p) <= r && Factions.relation(p, e) == Relation.ALLY));
        allies.add(p);
        for (LivingEntity e : allies) {
            e.heal(e.getMaxHealth() * HEAL);
            e.addEffect(new MobEffectInstance(WFRegistry.RALLIED, RALLY_TICKS, 0));
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RALLY_TICKS, 0));
            if (e instanceof SoldierEntity s) s.rallied();
            level.sendParticles(ParticleTypes.WAX_ON, e.getX(), e.getY() + 1, e.getZ(), 6, 0.3, 0.5, 0.3, 0.1);
        }
        for (int i = 0; i < 48; i++) {
            double a = i * Mth.TWO_PI / 48;
            level.sendParticles(ParticleTypes.END_ROD, p.getX() + Math.cos(a) * r * 0.5, p.getY() + 0.2,
                    p.getZ() + Math.sin(a) * r * 0.5, 1, 0, 0.05, 0, 0.01);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 1.5F, 1.2F);
        if (p instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("RALLY THE BANNER!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        }
        return allies.size();
    }

    public static Component bar(int valor) {
        int filled = valor / 10;
        return Component.literal("Valor ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("|".repeat(filled)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("|".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
