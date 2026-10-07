package com.warfront.flight;

import com.warfront.faction.Race;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The elves' Wind Charm: flight as a spell, paid with Mana Shards from your pack. Hold use to Levitate (hover and
 * drift, a shard every few seconds); sneak-use for a Wind Leap (a burst along your look, then a slow fall; a shard a
 * cast). No wings, no gear on the back: just leaves and wind.
 */
public class WindCharmItem extends Item {
    /** Ticks of levitation one shard pays for. */
    public static final int LEVITATE_TICKS_PER_SHARD = 60;
    public static final double LEAP = 1.6;

    public WindCharmItem(Properties properties) {
        super(properties);
    }

    public static boolean isElf(Player p) {
        String race = p.level().isClientSide ? com.warfront.network.ClientRaceState.get(p.getUUID()) : p.getData(WFRegistry.RACE);
        return Race.ELF.id().equals(race);
    }

    /** Takes a Mana Shard from the player's inventory; false if there was none (creative pays nothing). */
    public static boolean pay(Player p) {
        if (p.getAbilities().instabuild) return true;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(WFRegistry.MANA_SHARD.get())) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Wind Leap: one shard, a burst along the look direction, slow fall until landing. */
    public static boolean leap(Player p) {
        if (!isElf(p) || !pay(p)) return false;
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(p.getDeltaMovement().add(look.x * LEAP, Math.max(0.6, look.y * LEAP), look.z * LEAP));
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, true, true));
        p.fallDistance = 0F;
        if (p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY(), p.getZ(), 30, 0.6, 0.2, 0.6, 0.05);
            level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 12, 0.4, 0.1, 0.4, 0.05);
            level.playSound(null, p.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (!isElf(p)) {
            if (!level.isClientSide) p.displayClientMessage(Component.literal("The charm only answers elves.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (p.isShiftKeyDown()) {
            if (!level.isClientSide && !leap(p)) {
                p.displayClientMessage(Component.literal("You need a Mana Shard.").withStyle(ChatFormatting.RED), true);
            }
            p.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        p.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player p)) return;
        int used = getUseDuration(stack, entity) - remaining;
        if (!level.isClientSide && used % LEVITATE_TICKS_PER_SHARD == 0 && !pay(p)) {
            p.releaseUsingItem();
            p.displayClientMessage(Component.literal("Out of Mana Shards.").withStyle(ChatFormatting.RED), true);
            return;
        }
        // Levitate: hover, drifting slowly where you look.
        Vec3 look = p.getLookAngle();
        Vec3 m = p.getDeltaMovement();
        p.setDeltaMovement(m.x * 0.85 + look.x * 0.03, Math.max(m.y, 0.0) * 0.5 + (p.isShiftKeyDown() ? -0.05 : 0.02), m.z * 0.85 + look.z * 0.03);
        p.fallDistance = 0F;
        if (level instanceof ServerLevel server && used % 4 == 0) {
            server.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() - 0.2, p.getZ(), 3, 0.4, 0.1, 0.4, 0.01);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Elf only. Hold use to Levitate; sneak-use to Wind Leap.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Costs Mana Shards from your pack.").withStyle(ChatFormatting.AQUA));
    }
}
