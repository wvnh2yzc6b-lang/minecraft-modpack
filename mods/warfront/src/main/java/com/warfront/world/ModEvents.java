package com.warfront.world;

import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import com.warfront.registry.WFRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.warfront.network.ManaPayload;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Warfront.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    private ModEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WFRegistry.SOLDIER.get(), SoldierEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(ManaPayload.TYPE, ManaPayload.STREAM_CODEC, ManaPayload::handle);
        registrar.playToClient(com.warfront.network.RacePayload.TYPE, com.warfront.network.RacePayload.STREAM_CODEC,
                com.warfront.network.RacePayload::handle);
    }
}
