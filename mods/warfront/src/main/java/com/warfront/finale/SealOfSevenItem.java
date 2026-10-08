package com.warfront.finale;

import com.warfront.registry.WFRegistry;
import com.warfront.war.Campaign;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Dropped by the seventh warlord: the clue to the hidden one. Used, it joins the seven War Maps into the Sealed Map. */
public class SealOfSevenItem extends Item {
    public SealOfSevenItem(Properties properties) {
        super(properties);
    }

    /** Turns the seal into the Sealed Map if all seven warlords are beaten; true if it did. */
    public static boolean unseal(Player p, ItemStack seal) {
        if (Campaign.warlordsBeaten(p) < 7) return false;
        seal.shrink(1);
        ItemStack map = new ItemStack(WFRegistry.SEALED_MAP.get());
        if (!p.getInventory().add(map)) p.drop(map, false);
        p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (unseal(player, stack)) {
                player.displayClientMessage(Component.literal("The seven maps fold into one. It points somewhere outside the world.")
                        .withStyle(ChatFormatting.GOLD), false);
            } else {
                player.displayClientMessage(Component.literal("The seal will not open until all seven warlords have fallen.")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Seven wars, one hand behind them all.").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Use it to make the Sealed Map.").withStyle(ChatFormatting.GRAY));
    }
}
