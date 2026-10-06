package com.warfront.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.warfront.Warfront;
import com.warfront.network.ClientRaceState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Makes players of the demon race look the part: a bone-chitin skull crown, huge blood-glowing
 * wings, a tattered robe and cloak, and brambles, over a demon skin. Player-sized.
 */
public class DemonPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation SKIN = Warfront.id("textures/entity/player/demon_skin.png");
    public static final ResourceLocation SKIN_GLOW = Warfront.id("textures/entity/player/demon_skin_glow.png");
    public static final ResourceLocation EXTRAS = Warfront.id("textures/entity/player/demon_extras.png");
    public static final ResourceLocation EXTRAS_GLOW = Warfront.id("textures/entity/player/demon_extras_glow.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    /** The most recently built layer; the first-person arm renderer borrows its parts. */
    static DemonPlayerLayer instance;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart wingR;
    private final ModelPart wingL;
    private final ModelPart wingROuter;
    private final ModelPart wingLOuter;
    private final ModelPart cloak;
    private final Map<AbstractClientPlayer, WingAnimator.State> states = new WeakHashMap<>();
    private final WingAnimator.Pose pose = new WingAnimator.Pose();

    public DemonPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                            EntityModelSet models) {
        super(parent);
        this.root = models.bakeLayer(WFModelLayers.DEMON_PLAYER);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.wingR = body.getChild("wing_r");
        this.wingL = body.getChild("wing_l");
        this.wingROuter = wingR.getChild("wing_r_outer");
        this.wingLOuter = wingL.getChild("wing_l_outer");
        this.cloak = body.getChild("cloak");
        instance = this;
    }

    public static boolean isDemon(Player player) {
        return "demon".equals(ClientRaceState.get(player.getUUID()));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!isDemon(player) || player.isInvisible()) return;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

        // Demon skin over the player's own, then its glowing eyes.
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(SKIN)), packedLight, overlay);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(SKIN_GLOW)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        head.copyFrom(model.head);
        body.copyFrom(model.body);
        rightArm.copyFrom(model.rightArm);
        leftArm.copyFrom(model.leftArm);
        rightLeg.copyFrom(model.rightLeg);
        leftLeg.copyFrom(model.leftLeg);
        // A helmet hides the skull crown; a chestplate with an elytra folds the wings away.
        head.visible = player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        boolean elytra = player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof net.minecraft.world.item.ElytraItem;
        wingR.visible = !elytra;
        wingL.visible = !elytra;
        animate(player, limbSwingAmount, ageInTicks);

        root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(EXTRAS)), packedLight, overlay);
        root.render(poseStack, buffer.getBuffer(RenderType.eyes(EXTRAS_GLOW)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    private void animate(AbstractClientPlayer player, float limbSwingAmount, float ageInTicks) {
        boolean flying = player.isFallFlying();
        boolean airborne = !player.onGround() && !player.isInWater() && !player.onClimbable()
                && !player.getAbilities().flying;
        double dx = player.getX() - player.xo, dy = player.getY() - player.yo, dz = player.getZ() - player.zo;
        WingAnimator.update(states.computeIfAbsent(player, p -> new WingAnimator.State()),
                new WingAnimator.Input(flying, airborne, limbSwingAmount, player.isSprinting(), (float) dy,
                        (float) Math.sqrt(dx * dx + dy * dy + dz * dz), player.getFallFlyingTicks(), ageInTicks),
                pose);
        wingR.setRotation(pose.x, pose.y, pose.z);
        wingL.setRotation(pose.x, -pose.y, -pose.z);
        wingROuter.setRotation(0F, 0F, pose.outerZ);
        wingLOuter.setRotation(0F, 0F, -pose.outerZ);
        cloak.xRot = pose.cloakX;
    }

    /** Draws the demon arm, and brambles in first person. */
    void renderFirstPersonArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                              PlayerModel<AbstractClientPlayer> model, HumanoidArm side) {
        boolean right = side == HumanoidArm.RIGHT;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        ModelPart extras = right ? rightArm : leftArm;
        VertexConsumer skin = buffer.getBuffer(RenderType.entityCutoutNoCull(SKIN));
        arm.render(poseStack, skin, packedLight, OverlayTexture.NO_OVERLAY);
        sleeve.render(poseStack, skin, packedLight, OverlayTexture.NO_OVERLAY);
        extras.copyFrom(arm);
        extras.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(EXTRAS)), packedLight, OverlayTexture.NO_OVERLAY);
    }
}
