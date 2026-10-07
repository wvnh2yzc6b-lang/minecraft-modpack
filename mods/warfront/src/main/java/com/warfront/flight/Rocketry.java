package com.warfront.flight;

import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server side of the rocket pack: fuel burn, smoke and noise, and the landing shockwave. */
public final class Rocketry {
    private static final Set<UUID> THRUSTING = new HashSet<>();
    private static final Map<UUID, Long> LAST_THRUST = new HashMap<>();
    /** A landing this soon after thrusting is a dive: no fall damage, and a shockwave. */
    public static final int DIVE_TICKS = 20;

    private Rocketry() {}

    public static void setThrusting(ServerPlayer p, boolean on) {
        if (on && RocketPackItem.wearing(p) && RocketPackItem.isOrc(p)) THRUSTING.add(p.getUUID());
        else THRUSTING.remove(p.getUUID());
    }

    /** Burns a tick of fuel; false (and nothing burned) when empty. */
    public static boolean burn(Player p) {
        ItemStack pack = p.getItemBySlot(EquipmentSlot.CHEST);
        int fuel = RocketPackItem.fuel(pack);
        if (fuel <= 0) return false;
        if (!p.getAbilities().instabuild) RocketPackItem.setFuel(pack, fuel - 1);
        LAST_THRUST.put(p.getUUID(), p.level().getGameTime());
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (THRUSTING.isEmpty()) return;
        THRUSTING.removeIf(id -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || !RocketPackItem.wearing(p) || p.onGround() || !burn(p)) return true;
            ServerLevel level = p.serverLevel();
            Vec3 back = p.position().add(p.getLookAngle().multiply(-0.4, 0, -0.4)).add(0, 0.8, 0);
            level.sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z, 3, 0.08, 0.05, 0.08, 0.02);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, back.x, back.y - 0.3, back.z, 2, 0.1, 0.1, 0.1, 0.01);
            if (p.tickCount % 4 == 0) level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.6F);
            return false;
        });
    }

    /** A landing: true if it was a rocket dive (cancel the fall damage); knocks nearby enemies back. */
    public static boolean landed(Player p) {
        Long last = LAST_THRUST.get(p.getUUID());
        if (last == null || p.level().getGameTime() - last > DIVE_TICKS) return false;
        LAST_THRUST.remove(p.getUUID());
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 3, 0.8, 0.1, 0.8, 0);
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.3F);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(3),
                    e -> e != p && Factions.relation(p, e) == Relation.ENEMY)) {
                Vec3 away = e.position().subtract(p.position()).normalize();
                e.knockback(1.2, -away.x, -away.z);
                e.hurtMarked = true;
            }
        }
        return true;
    }

    public static void clearAll() {
        THRUSTING.clear();
        LAST_THRUST.clear();
    }
}
