package com.warfront.client;

import com.warfront.Warfront;
import com.warfront.client.model.UnitGeometry;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import com.warfront.registry.WFRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Warfront.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WFRegistry.SOLDIER.get(), SoldierRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WFModelLayers.IMP, UnitGeometry::imp);
        event.registerLayerDefinition(WFModelLayers.IMP_BULWARK, UnitGeometry::imp_bulwark);
        event.registerLayerDefinition(WFModelLayers.IMP_IMPALER, UnitGeometry::imp_impaler);
        event.registerLayerDefinition(WFModelLayers.IMP_FIRECASTER, UnitGeometry::imp_firecaster);
        event.registerLayerDefinition(WFModelLayers.DEMON_PLAYER, UnitGeometry::demon_player);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new DemonPlayerLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, Warfront.id("mana"), ManaHud::render);
    }
}
