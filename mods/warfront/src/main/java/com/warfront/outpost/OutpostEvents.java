package com.warfront.outpost;

import com.warfront.Warfront;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** A raid chest holding loot can't be broken, not even in creative. */
@EventBusSubscriber(modid = Warfront.MODID)
public final class OutpostEvents {
    private OutpostEvents() {}

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof RaidChestBlockEntity chest && !chest.isEmpty()) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("Empty the raid chest first.").withStyle(ChatFormatting.RED), true);
        }
    }
}
