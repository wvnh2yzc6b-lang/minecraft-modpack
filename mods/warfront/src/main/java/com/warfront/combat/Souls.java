package com.warfront.combat;

import com.warfront.Warfront;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Demon Soul Harvest. Every kill takes a soul (up to MAX): the demon heals a little and each soul adds attack
 * damage. With a full harvest the next hit unleashes a Soul Burst on the target and the enemies around it,
 * spending every soul. Souls live in the entity's persistent data, so a death empties them.
 */
public final class Souls {
    public static final int MAX = 10;
    public static final double DAMAGE_PER_SOUL = 0.02;
    public static final float HEAL_PER_KILL = 2.0F;
    public static final float BURST_DAMAGE = 6.0F;
    public static final float BURST_SPLASH = 4.0F;
    public static final double BURST_RADIUS = 3.0;
    private static final String KEY = "warfront_souls";
    private static final ResourceLocation MODIFIER = Warfront.id("soul_harvest");

    private Souls() {}

    public static int count(LivingEntity e) {
        return e.getPersistentData().getInt(KEY);
    }

    /** A demon killed something: take its soul. */
    public static void harvest(LivingEntity demon, LivingEntity victim) {
        if (demon.level().isClientSide || !demon.isAlive() || demon == victim) return;
        int souls = Math.min(MAX, count(demon) + 1);
        set(demon, souls);
        demon.heal(HEAL_PER_KILL);
        if (demon.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY(0.5), victim.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
        }
        demon.playSound(SoundEvents.SCULK_CATALYST_BLOOM, 1.0F, 0.8F);
    }

    /** With a full harvest, spends every soul on a burst around the target. Returns the extra damage. */
    public static float burst(LivingEntity demon, LivingEntity target) {
        if (demon.level().isClientSide || count(demon) < MAX) return 0F;
        set(demon, 0);
        if (demon.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY(0.5), target.getZ(),
                    30, BURST_RADIUS / 2, 0.5, BURST_RADIUS / 2, 0.05);
            for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(BURST_RADIUS))) {
                if (other == target || other == demon || !other.isAlive()) continue;
                if (Factions.relation(demon, other) != Relation.ENEMY) continue;
                other.hurt(server.damageSources().indirectMagic(demon, demon), BURST_SPLASH);
            }
        }
        demon.playSound(SoundEvents.WITHER_SHOOT, 0.8F, 1.3F);
        if (demon instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("SOUL BURST!").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), true);
        }
        return BURST_DAMAGE;
    }

    /** Test mode: a full harvest. */
    public static void fill(LivingEntity e) {
        set(e, MAX);
    }

    private static void set(LivingEntity demon, int souls) {
        demon.getPersistentData().putInt(KEY, souls);
        AttributeInstance attack = demon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(MODIFIER);
            if (souls > 0) attack.addPermanentModifier(new AttributeModifier(MODIFIER, souls * DAMAGE_PER_SOUL,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (demon instanceof ServerPlayer p && souls > 0) {
            p.displayClientMessage(Component.literal("Souls ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.literal("|".repeat(souls)).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("|".repeat(MAX - souls)).withStyle(ChatFormatting.DARK_GRAY)), true);
        }
    }
}
