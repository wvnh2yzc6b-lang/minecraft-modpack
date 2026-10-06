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

/**
 * Makes players of the demon race look the part: a bone-chitin skull crown, huge blood-glowing
 * wings, a tattered robe and cloak, brambles and talons, over a demon skin. Player-sized.
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
    private final ModelPart cloak;
    private final float wingZ;
    private final float wingY;
    private final float cloakX;
    private final float wingX;

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
        this.cloak = body.getChild("cloak");
        this.wingZ = wingR.getInitialPose().zRot;
        this.wingY = wingR.getInitialPose().yRot;
        this.cloakX = cloak.getInitialPose().xRot;
        this.wingX = wingR.getInitialPose().xRot;
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
        // Claws tuck away on a hand that is holding something.
        boolean rightMain = player.getMainArm() == HumanoidArm.RIGHT;
        setClaws(rightArm, "r", (rightMain ? player.getMainHandItem() : player.getOffhandItem()).isEmpty());
        setClaws(leftArm, "l", (rightMain ? player.getOffhandItem() : player.getMainHandItem()).isEmpty());
        animate(player, limbSwingAmount, ageInTicks);

        root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(EXTRAS)), packedLight, overlay);
        root.render(poseStack, buffer.getBuffer(RenderType.eyes(EXTRAS_GLOW)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    private static void setClaws(ModelPart arm, String side, boolean visible) {
        for (int i = 0; i < 3; i++) {
            String name = "talon_" + side + i;
            if (arm.hasChild(name)) arm.getChild(name).visible = visible;
        }
    }

    private void animate(AbstractClientPlayer player, float limbSwingAmount, float ageInTicks) {
        if (player.isFallFlying()) {
            // In flight: wings spread wide and swept back, beating in slow powerful strokes;
            // the cloak streams out behind.
            float beat = Mth.sin(ageInTicks * 0.32F);
            wingR.zRot = 0.35F + beat * 0.35F;
            wingL.zRot = -0.35F - beat * 0.35F;
            wingR.yRot = 0.2F;
            wingL.yRot = -0.2F;
            wingR.xRot = 0.1F + beat * 0.08F;
            wingL.xRot = 0.1F + beat * 0.08F;
            cloak.xRot = 1.25F + Mth.sin(ageInTicks * 0.4F) * 0.06F;
            return;
        }
        wingR.xRot = wingX;
        wingL.xRot = wingX;
        boolean airborne = !player.onGround() && !player.isInWater();
        float effort = Math.min(1F, limbSwingAmount * 1.5F + (airborne ? 1F : 0F));
        float flap = Mth.sin(ageInTicks * (0.12F + effort * 0.35F)) * (0.05F + effort * 0.3F);
        wingR.zRot = wingZ + flap;
        wingL.zRot = -wingZ - flap;
        wingR.yRot = wingY - effort * 0.25F;
        wingL.yRot = -wingY + effort * 0.25F;
        cloak.xRot = cloakX + limbSwingAmount * 0.7F + (player.isSprinting() ? 0.3F : 0F)
                + Mth.sin(ageInTicks * 0.05F) * 0.03F;
    }

    /** Draws the demon arm, brambles and talons in first person. */
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
        setClaws(extras, right ? "r" : "l", false);   // first-person hands are usually holding something
        extras.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(EXTRAS)), packedLight, OverlayTexture.NO_OVERLAY);
    }
}
