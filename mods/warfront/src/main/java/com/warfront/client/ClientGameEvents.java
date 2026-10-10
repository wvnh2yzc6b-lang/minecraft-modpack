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
    private static boolean rocketOn;

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
        // Orc rocket pack: hold jump in the air to thrust; the server burns the fuel.
        boolean thrusting = jumpDown && com.warfront.flight.RocketPackItem.canThrust(player);
        if (thrusting) com.warfront.flight.RocketPackItem.thrust(player);
        if (thrusting != rocketOn) {
            rocketOn = thrusting;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.warfront.network.RocketPayload(thrusting));
        }

        com.warfront.client.ui.ManaOverlay.tick();
        while (ClientSetup.WAR_TABLE.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.warfront.network.WarTableActionPayload(-1));
        }
        while (ClientSetup.RACE_POWER.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.warfront.network.RacePowerPayload(player.isShiftKeyDown()));
        }
        while (ClientSetup.SKILL_TREE.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.warfront.network.SkillActionPayload(new java.util.ArrayList<>(java.util.List.of("open"))));
        }
        for (int i = 0; i < ClientSetup.SKILL_KEYS.length; i++) {
            while (ClientSetup.SKILL_KEYS[i].consumeClick()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.warfront.network.SkillActionPayload(
                        new java.util.ArrayList<>(java.util.List.of("use", Integer.toString(i),
                                Float.toString(player.input.forwardImpulse), Float.toString(player.input.leftImpulse)))));
            }
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

    /** Run and Gun (Marksman): no slowdown while drawing a bow or aiming. Vanilla cuts the input to a fifth right after this. */
    @SubscribeEvent
    public static void onMovementInput(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (!player.isUsingItem() || player.isPassenger() || !com.warfront.skill.ClientSkills.has("run_and_gun")
                || !com.warfront.skill.SkillHooks.isRanged(player.getUseItem())) return;
        event.getInput().leftImpulse *= 5.0F;
        event.getInput().forwardImpulse *= 5.0F;
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
