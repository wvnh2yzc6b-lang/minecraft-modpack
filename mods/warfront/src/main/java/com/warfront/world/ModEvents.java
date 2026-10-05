package com.warfront.world;

import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import com.warfront.registry.WFRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = Warfront.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    private ModEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WFRegistry.SOLDIER.get(), SoldierEntity.createAttributes().build());
    }
}
