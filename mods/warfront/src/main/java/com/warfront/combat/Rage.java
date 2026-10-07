package com.warfront.combat;

import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Orc rage. Landing and taking hits fills it; when full the orc goes into a Frenzy for a few seconds and the
 * rage empties. Out of combat it drains. Kept in the entity's persistent data, so players and soldiers share it.
 */
public final class Rage {
    public static final int MAX = 100;
    public static final int ON_HIT = 10;
    public static final int ON_HURT = 15;
    public static final int FRENZY_TICKS = 160;
    /** Ticks after the last gain before rage starts to drain, then 1 point per DRAIN_TICKS. */
    private static final int GRACE_TICKS = 60;
    private static final int DRAIN_TICKS = 4;
    private static final String KEY = "warfront_rage";
    private static final String TIME_KEY = "warfront_rage_time";

    private Rage() {}

    public static int current(LivingEntity e) {
        CompoundTag d = e.getPersistentData();
        long idle = e.level().getGameTime() - d.getLong(TIME_KEY) - GRACE_TICKS;
        int rage = d.getInt(KEY);
        if (idle > 0) rage -= (int) Math.min(MAX, idle / DRAIN_TICKS);
        return Math.max(0, rage);
    }

    public static boolean isFrenzied(LivingEntity e) {
        return e.hasEffect(WFRegistry.FRENZY);
    }

    public static void gain(LivingEntity e, int amount) {
        if (e.level().isClientSide || !e.isAlive() || isFrenzied(e)) return;
        int rage = Math.min(MAX, current(e) + amount);
        CompoundTag d = e.getPersistentData();
        d.putLong(TIME_KEY, e.level().getGameTime());
        if (rage < MAX) {
            d.putInt(KEY, rage);
            if (e instanceof ServerPlayer p) p.displayClientMessage(bar(rage), true);
            return;
        }
        d.putInt(KEY, 0);
        e.addEffect(new MobEffectInstance(WFRegistry.FRENZY, FRENZY_TICKS, 0));
        e.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.2F);
        if (e.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, e.getX(), e.getEyeY(), e.getZ(), 6, 0.4, 0.3, 0.4, 0);
        }
        if (e instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("FRENZY!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        }
    }

    /** Test mode: rage one hit short of a Frenzy. */
    public static void fill(LivingEntity e) {
        CompoundTag d = e.getPersistentData();
        d.putInt(KEY, MAX - 1);
        d.putLong(TIME_KEY, e.level().getGameTime());
        if (e instanceof ServerPlayer p) p.displayClientMessage(bar(MAX - 1), true);
    }

    private static Component bar(int rage) {
        int filled = rage / 10;
        return Component.literal("Rage ").withStyle(ChatFormatting.RED)
                .append(Component.literal("|".repeat(filled)).withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal("|".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
