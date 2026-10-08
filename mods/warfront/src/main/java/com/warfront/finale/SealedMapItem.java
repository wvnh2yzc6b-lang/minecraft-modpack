package com.warfront.finale;

import com.warfront.block.WarStandardBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** The Sealed Map: the seven War Maps made one. Used on your War Standard it opens the way to the Frozen Field. */
public class SealedMapItem extends Item {
    public SealedMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock() instanceof WarStandardBlock)) return InteractionResult.PASS;
        if (ctx.getPlayer() instanceof ServerPlayer p && !FrozenField.enter(p)) {
            p.displayClientMessage(Component.literal("The map's ink stays still here.").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    /** In the Frozen Field, using it anywhere leads home. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p && FrozenField.isField(level)) {
            FrozenField.leave(p);
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on your War Standard to march on the Frozen Field.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Your army goes with you. Use it there to come home.").withStyle(ChatFormatting.GRAY));
    }
}
