package com.warfront.item;

import com.warfront.block.ManaWellBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Dense mana mined from Mana Ore: strong fuel, and the material wells, pylons and altars are built from. */
public class ManaCrystalItem extends Item {
    public ManaCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Fuel: +" + (int) ManaWellBlockEntity.CRYSTAL_MANA + " mana in a Mana Well")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Used to build Mana Wells, Pylons and Summoning Altars").withStyle(ChatFormatting.GRAY));
    }
}
