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
    private final ModelPart wingROuter;
    private final ModelPart wingLOuter;
    private final ModelPart cloak;
    private final float cloakX;
    /** Per player: how far the wings are spread (0 tucked, 1 flight) and the age it was last eased at. */
    private final Map<AbstractClientPlayer, float[]> spread = new WeakHashMap<>();

    // Tucked: the wrist folds up over the shoulder and the membrane hangs flat down the back.
    private static final float TUCK_X = 0.77F, TUCK_Y = 1.26F, TUCK_Z = 0.7F, TUCK_OUTER_Z = -2.95F;

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
        this.cloakX = cloak.getInitialPose().xRot;
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
        boolean flying = player.isFallFlying();
        boolean airborne = !player.onGround() && !player.isInWater() && !player.onClimbable()
                && !player.getAbilities().flying;
        // Wings open fully in flight, half-open to balance in a jump or fall, and fold away on the ground.
        float target = flying ? 1F : airborne ? 0.4F : 0F;
        float[] state = spread.computeIfAbsent(player, p -> new float[] {target, ageInTicks});
        float dt = Mth.clamp(ageInTicks - state[1], 0F, 5F);
        state[1] = ageInTicks;
        state[0] += (target - state[0]) * Math.min(1F, dt * (target > state[0] ? 0.3F : 0.18F));
        float s = state[0];

        // Spread pose: wide and swept back, beating in slow powerful strokes; faster flaps when only
        // half-open, like catching balance. The beat fades in with the spread.
        float beat = Mth.sin(ageInTicks * (flying ? 0.32F : 0.6F)) * s;
        float x = Mth.lerp(s, TUCK_X, 0.1F) + beat * 0.08F;
        float y = Mth.lerp(s, TUCK_Y, 0.2F);
        float z = Mth.lerp(s, TUCK_Z, 0.35F) + beat * 0.35F;
        // Folded, the wings breathe a little; walking jostles them.
        float idle = (1F - s) * (Mth.sin(ageInTicks * 0.08F) * 0.03F + limbSwingAmount * 0.06F);
        wingR.setRotation(x, y, z + idle);
        wingL.setRotation(x, -y, -z - idle);
        // The elbow: folded flat against the inner wing when tucked, straight when spread, and
        // trailing the beat slightly so the tip flexes.
        float outer = Mth.lerp(s, TUCK_OUTER_Z, 0F) - Mth.cos(ageInTicks * 0.32F) * 0.12F * s;
        wingROuter.setRotation(0F, 0F, outer);
        wingLOuter.setRotation(0F, 0F, -outer);

        if (flying) {
            cloak.xRot = 0.18F + Mth.sin(ageInTicks * 0.45F) * 0.08F;   // streams back along the body
        } else {
            cloak.xRot = cloakX + limbSwingAmount * 0.7F + (player.isSprinting() ? 0.3F : 0F)
                    + Mth.sin(ageInTicks * 0.05F) * 0.03F;
        }
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
