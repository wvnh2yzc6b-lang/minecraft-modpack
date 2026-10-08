package com.warfront.combat;

import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Dwarf race power: Hold the Line. Resolve (0-100) builds while a dwarf stands and fights, and gives armor and
 * knockback resistance. At 100 a crouch swears the Oath of Stone (soldiers swear it on their own): 6 seconds of
 * near-immovable footing and 30% less damage, ending in a stone shockwave. Kept in persistent data, like Rage.
 */
public final class Resolve {
    public static final int MAX = 100;
    public static final int PER_SECOND = 4;
    public static final int ON_HURT = 6;
    public static final int SPRINT_LOSS = 10;
    public static final int IDLE_LOSS = 10;
    /** Ticks since the last hit given or taken that still count as "in combat"; past IDLE_TICKS it drains. */
    public static final int COMBAT_TICKS = 60;
    public static final int IDLE_TICKS = 80;
    public static final int OATH_TICKS = 120;
    public static final float OATH_DAMAGE_TAKEN = 0.7F;
    public static final double SHOCK_RADIUS = 4.0;
    public static final float SHOCK_DAMAGE = 4F;

    private static final String KEY = "warfront_resolve";
    private static final String COMBAT_KEY = "warfront_resolve_combat";
    private static final ResourceLocation ARMOR_ID = Warfront.id("resolve_armor");
    private static final ResourceLocation KNOCKBACK_ID = Warfront.id("resolve_knockback");

    private Resolve() {}

    public static int current(LivingEntity e) {
        return e.getPersistentData().getInt(KEY);
    }

    public static boolean inOath(LivingEntity e) {
        return e.hasEffect(WFRegistry.OATH_OF_STONE);
    }

    public static void set(LivingEntity e, int value) {
        e.getPersistentData().putInt(KEY, Math.max(0, Math.min(MAX, value)));
        applyBonus(e);
    }

    /** A hit given or taken: the dwarf is in combat. */
    public static void markCombat(LivingEntity e) {
        e.getPersistentData().putLong(COMBAT_KEY, e.level().getGameTime());
    }

    public static void hurt(LivingEntity e) {
        markCombat(e);
        if (!inOath(e)) set(e, current(e) + ON_HURT);
    }

    /** Once a second for every dwarf: build while standing in a fight, lose it running or idle. */
    public static void tickSecond(LivingEntity e) {
        tickSecond(e, e.level().getGameTime());
    }

    public static void tickSecond(LivingEntity e, long now) {
        if (inOath(e)) return;
        CompoundTag d = e.getPersistentData();
        long since = d.contains(COMBAT_KEY) ? now - d.getLong(COMBAT_KEY) : Long.MAX_VALUE;
        int r = current(e);
        if (e.isSprinting()) r -= SPRINT_LOSS;
        else if (since <= COMBAT_TICKS) r += PER_SECOND;
        else if (since > IDLE_TICKS) r -= IDLE_LOSS;
        set(e, r);
        if (e instanceof SoldierEntity && current(e) >= MAX) swear(e);
    }

    /** +1 armor and +5% knockback resistance per 20 Resolve, as transient attribute modifiers. */
    public static void applyBonus(LivingEntity e) {
        int steps = current(e) / 20;
        modifier(e.getAttribute(Attributes.ARMOR), ARMOR_ID, steps);
        modifier(e.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID, steps * 0.05);
    }

    private static void modifier(AttributeInstance attr, ResourceLocation id, double value) {
        if (attr == null) return;
        AttributeModifier old = attr.getModifier(id);
        if (old != null && old.amount() == value) return;
        attr.removeModifier(id);
        if (value != 0) attr.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
    }

    /** Swears the Oath of Stone if Resolve is full; spends it all. */
    public static boolean swear(LivingEntity e) {
        if (current(e) < MAX || inOath(e)) return false;
        set(e, 0);
        e.addEffect(new MobEffectInstance(WFRegistry.OATH_OF_STONE, OATH_TICKS, 0));
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, OATH_TICKS, 0));
        e.level().playSound(null, e.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8F, 0.6F);
        if (e.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    e.getX(), e.getY() + 0.1, e.getZ(), 30, 0.6, 0.1, 0.6, 0.1);
        }
        if (e instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("OATH OF STONE").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD), true);
        }
        return true;
    }

    /** The Oath ends: a stone shockwave knocks back and hurts enemies within 4 blocks. Returns how many it hit. */
    public static int shockwave(LivingEntity e) {
        if (!(e.level() instanceof ServerLevel level)) return 0;
        int hit = 0;
        for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(SHOCK_RADIUS),
                t -> t != e && t.isAlive() && t.distanceTo(e) <= SHOCK_RADIUS + 0.5 && Factions.relation(e, t) == Relation.ENEMY)) {
            t.hurt(e.damageSources().mobAttack(e), SHOCK_DAMAGE);
            Vec3 away = t.position().subtract(e.position());
            if (away.horizontalDistanceSqr() < 1e-4) away = new Vec3(1, 0, 0);
            t.knockback(1.6, -away.x, -away.z);
            t.hurtMarked = true;
            hit++;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()),
                e.getX(), e.getY() + 0.1, e.getZ(), 80, SHOCK_RADIUS / 2, 0.1, SHOCK_RADIUS / 2, 0.2);
        level.playSound(null, e.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.5F);
        level.playSound(null, e.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        return hit;
    }

    public static Component bar(int resolve) {
        int filled = resolve / 10;
        return Component.literal("Resolve ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("|".repeat(filled)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("|".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
