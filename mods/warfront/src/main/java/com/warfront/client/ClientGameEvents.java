package com.warfront.client;

import com.warfront.Warfront;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import com.warfront.flight.WingFlight;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

/** Client-only game events. */
@EventBusSubscriber(modid = Warfront.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    private static boolean jumpWasDown;

    /**
     * Wing takeoff. Vanilla only offers takeoff to players wearing an elytra, so winged players get the
     * same gesture here: press jump while falling. The server double-checks via the same rule.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        boolean jumpDown = mc.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown && player.getDeltaMovement().y < 0.0
                && !WingFlight.wearsWorkingElytra(player) && player.tryToStartFallFlying()) {
            player.connection.send(new ServerboundPlayerCommandPacket(player,
                    ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
        jumpWasDown = jumpDown;

        com.warfront.client.ui.ManaOverlay.tick();
        while (ClientSetup.WAR_TABLE.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.warfront.network.WarTableActionPayload(-1));
        }
        while (ClientSetup.RECALL.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(com.warfront.network.RecallPayload.INSTANCE);
        }
        while (ClientSetup.TEST_PANEL.consumeClick()) {
            if (com.warfront.network.TestModePayload.clientOn) mc.setScreen(new TestPanelScreen());
            else player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Test mode is off. An operator turns it on with /wftest on."), true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        com.warfront.client.ui.ManaOverlay.renderWorld(event);
    }

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
