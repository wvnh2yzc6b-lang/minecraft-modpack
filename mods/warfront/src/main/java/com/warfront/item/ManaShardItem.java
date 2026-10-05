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

/** Right-click to absorb a shard into your mana pool; sneak to absorb as many as fit. */
public class ManaShardItem extends Item {
    public static final float MANA_PER_SHARD = 10F;

    public ManaShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        float room = Mana.max(player) - Mana.get(player);
        if (room < 1F) {
            player.displayClientMessage(Component.literal("Your mana is full.").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }
        int want = player.isShiftKeyDown() ? (int) Math.ceil(room / MANA_PER_SHARD) : 1;
        int used = Math.min(want, stack.getCount());
        Mana.add(player, used * MANA_PER_SHARD);
        if (!player.getAbilities().instabuild) stack.shrink(used);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
        player.displayClientMessage(Component.literal("Mana " + (int) Mana.get(player) + " / " + (int) Mana.max(player))
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: +" + (int) MANA_PER_SHARD + " mana (sneak: as many as fit)")
                .withStyle(ChatFormatting.AQUA));
    }
}
