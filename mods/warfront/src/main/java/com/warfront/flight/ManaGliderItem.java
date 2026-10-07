package com.warfront.flight;

import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The human Mana Glider. Worn in the chest slot and launched like an elytra (jump while falling), but only
 * humans can fly it, and it glides rather than dives: speed bleeds off above a cruising pace and the sink
 * rate is gentle unless you point steeply down. Using a Mana Shard mid-flight burns it for a burst of speed.
 * Built on the elytra so other mods see an ordinary chest-slot flier.
 */
public class ManaGliderItem extends ElytraItem {
    /** Above this speed (blocks per tick) the glider bleeds speed off. Elytra dives go well past 2. */
    public static final double CRUISE_SPEED = 0.9;
    /** Fraction of the excess speed kept each tick, so a boost fades over a few seconds. */
    public static final double DRAG = 0.96;
    /** Steepest sink (blocks per tick) unless the pilot points well down. */
    public static final double MAX_SINK = 0.3;
    /** Pitch (degrees below level) past which the pilot is diving on purpose and the sink cap lifts. */
    public static final float DIVE_PITCH = 50F;
    /** Speed added along the look direction by burning a Mana Shard. */
    public static final double BOOST = 1.0;
    public static final double BOOST_SPEED = 1.6;

    public ManaGliderItem(Properties properties) {
        super(properties);
    }

    /** Humans only. Works on both sides. */
    public static boolean isHuman(Player player) {
        String race = player.level().isClientSide ? ClientRaceState.get(player.getUUID())
                : player.getData(WFRegistry.RACE);
        return Race.HUMAN.id().equals(race);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return isFlyEnabled(stack) && entity instanceof Player player && isHuman(player);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(WFRegistry.MANA_CRYSTAL.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Human only. Jump while falling to glide.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Use a Mana Shard in flight for a burst of speed.").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Repair with Mana Crystals.").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** True while a player is flying on a Mana Glider. */
    public static boolean gliding(Player player) {
        return player.isFallFlying() && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .getItem() instanceof ManaGliderItem;
    }

    /** Glider handling, run each tick on the side that moves the player. */
    public static void glide(Player player) {
        Vec3 motion = player.getDeltaMovement();
        double speed = motion.length();
        if (speed > CRUISE_SPEED) {
            motion = motion.scale((CRUISE_SPEED + (speed - CRUISE_SPEED) * DRAG) / speed);
        }
        if (motion.y < -MAX_SINK && player.getXRot() < DIVE_PITCH) {
            motion = new Vec3(motion.x, -MAX_SINK, motion.z);
        }
        player.setDeltaMovement(motion);
    }

    /** Burns a Mana Shard for a burst of speed. Server side; the motion is sent to the client. */
    public static void boost(Player player, ItemStack shard) {
        if (!player.getAbilities().instabuild) shard.shrink(1);
        Vec3 motion = player.getDeltaMovement().add(player.getLookAngle().scale(BOOST));
        if (motion.length() > BOOST_SPEED) motion = motion.normalize().scale(BOOST_SPEED);
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 1.0F, 1.4F);
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(), player.getZ(), 16, 0.3, 0.3, 0.3, 0.05);
        }
    }
}
