package com.warfront.client;

import com.warfront.Warfront;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/** Client-only game events. */
@EventBusSubscriber(modid = Warfront.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    /** Demon players see their own clawed demon arm in first person. */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        DemonPlayerLayer layer = DemonPlayerLayer.instance;
        if (layer == null || !DemonPlayerLayer.isDemon(player)) return;
        EntityRenderer<? super AbstractClientPlayer> r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(r instanceof PlayerRenderer renderer)) return;
        var model = renderer.getModel();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        model.rightArm.xRot = 0.0F;
        model.leftArm.xRot = 0.0F;
        model.rightSleeve.xRot = 0.0F;
        model.leftSleeve.xRot = 0.0F;
        layer.renderFirstPersonArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), model,
                event.getArm());
        event.setCanceled(true);
    }
}
