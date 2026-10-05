package com.warfront.item;

import com.warfront.mana.Mana;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Permanently expands the user's mana pool. */
public class ManaCrystalItem extends Item {
    public static final float CAPACITY = 25F;

    public ManaCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (Mana.max(player) >= Mana.CAP) {
            player.displayClientMessage(Component.literal("Your mana pool cannot grow any further.")
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }
        Mana.raiseMax(player, CAPACITY);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.6F);
        player.displayClientMessage(Component.literal("Maximum mana is now " + (int) Mana.max(player))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: +" + (int) CAPACITY + " maximum mana (up to "
                + (int) Mana.CAP + ")").withStyle(ChatFormatting.AQUA));
    }
}
