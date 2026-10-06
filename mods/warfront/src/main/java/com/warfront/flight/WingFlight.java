package com.warfront.flight;

import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.registry.WFRegistry;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Demon wing flight. Works like an elytra (press jump while falling to spread your wings and glide,
 * with vanilla elytra physics and pose) but needs no elytra, and the wings add a gentle constant
 * thrust toward where you look. Thrust costs a little hunger and stops when you're too hungry to
 * sprint, leaving a plain glide.
 */
public final class WingFlight {
    /** Thrust added along the look direction each tick. */
    public static final double THRUST = 0.045;
    /** Thrust stops adding speed above this (blocks per tick); rockets reach about 1.7. */
    public static final double CRUISE_SPEED = 0.85;
    /** Food exhaustion per tick of powered flight (4 exhaustion = 1 hunger point). */
    public static final float EXHAUSTION = 0.025F;

    private WingFlight() {}

    /** True for players of a winged race. Works on both sides. */
    public static boolean hasWings(Player player) {
        String race = player.level().isClientSide ? ClientRaceState.get(player.getUUID())
                : player.getData(WFRegistry.RACE);
        return Race.DEMON.id().equals(race);
    }

    /** True if the player wears an elytra that would work anyway; vanilla handles that case. */
    public static boolean wearsWorkingElytra(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(player);
    }

    public static boolean canTakeOff(Player player) {
        return hasWings(player) && !player.onGround() && !player.isFallFlying() && !player.isInWater()
                && !player.isPassenger() && !player.getAbilities().flying && !player.hasEffect(MobEffects.LEVITATION);
    }

    public static boolean canStayAloft(Player player) {
        return !player.onGround() && !player.isPassenger() && !player.isInWater()
                && !player.getAbilities().flying && !player.hasEffect(MobEffects.LEVITATION);
    }

    /** Powered flight: a gentle push along the look direction, capped at a cruising speed. */
    public static void applyThrust(Player player) {
        if (player.getFoodData().getFoodLevel() <= 6 && !player.getAbilities().instabuild) return;
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        if (motion.dot(look) < CRUISE_SPEED) {
            player.setDeltaMovement(motion.add(look.scale(THRUST)));
        }
    }
}
