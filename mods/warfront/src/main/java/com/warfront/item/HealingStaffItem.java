package com.warfront.item;

import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Heals you and every ally within 6 blocks. Carried by healers. */
public class HealingStaffItem extends Item {
    public HealingStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6),
                e -> e.isAlive() && (e == player || Factions.relation(player, e) == Relation.ALLY));
        for (LivingEntity e : targets) {
            e.heal(4.0F);
            server.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(),
                    2, 0.3, 0.2, 0.3, 0.0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.2F);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.getCooldowns().addCooldown(this, 80);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: heal yourself and allies within 6 blocks").withStyle(ChatFormatting.GRAY));
    }
}
