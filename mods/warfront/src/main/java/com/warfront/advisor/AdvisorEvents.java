package com.warfront.advisor;

import com.warfront.Warfront;
import com.warfront.registry.WFRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Watches for the things the advisor's quest asks for. */
@EventBusSubscriber(modid = Warfront.MODID)
public final class AdvisorEvents {
    private AdvisorEvents() {}

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState state = event.getPlacedBlock();
        if (state.is(WFRegistry.MANA_WELL.get())) {
            if (Advisor.step(player) == Advisor.Step.WELL) Advisor.moveTo(player, event.getPos());
            Advisor.complete(player, Advisor.Step.WELL);
        } else if (state.is(WFRegistry.SUMMONING_ALTAR.get())) {
            Advisor.complete(player, Advisor.Step.ALTAR);
        } else if (state.is(WFRegistry.WAR_STANDARD.get())) {
            com.warfront.war.RaidScheduler.setHome(player, event.getPos());
            Advisor.complete(player, Advisor.Step.STANDARD);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (event.getPlayer() instanceof ServerPlayer player && state.is(WFRegistry.MANABLOOM.get())
                && state.getValue(CropBlock.AGE) >= CropBlock.MAX_AGE) {
            Advisor.complete(player, Advisor.Step.BLOOM);
        }
    }
}
