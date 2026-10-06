package com.warfront.item;

import com.warfront.block.ManaWellBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Everyday mana fuel, grown on Manabloom. Feed it to a Mana Well. */
public class ManaShardItem extends Item {
    public ManaShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Fuel: +" + (int) ManaWellBlockEntity.SHARD_MANA + " mana in a Mana Well")
                .withStyle(ChatFormatting.AQUA));
    }
}
