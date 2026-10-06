package com.warfront.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The imp: digitigrade legs, long clawed arms, huge bat wings and a curling scorpion tail.
 * Geometry comes from {@link UnitGeometry#imp()}; this class only animates it.
 */
public class ImpModel extends HumanoidModel<SoldierEntity> {
    private final ModelPart wingR;
    private final ModelPart wingL;
    private final ModelPart[] tail = new ModelPart[6];
    private final float wingZ;
    private final float wingY;
    private final float bodyPitch;
    private final float legPitch;
    /** The Firecaster's floating ember orb, if this variant has one. */
    private final ModelPart orb;
    private final float orbY;

    public ImpModel(ModelPart root) {
        super(root);
        this.wingR = body.getChild("wing_r");
        this.wingL = body.getChild("wing_l");
        ModelPart seg = body.getChild("tail_1");
        for (int i = 0; i < tail.length; i++) {
            tail[i] = seg;
            if (i + 1 < tail.length) seg = seg.getChild("tail_" + (i + 2));
        }
        this.wingZ = wingR.getInitialPose().zRot;
        this.wingY = wingR.getInitialPose().yRot;
        this.bodyPitch = body.getInitialPose().xRot;
        this.legPitch = rightLeg.getInitialPose().xRot;
        ModelPart hand = leftArm.getChild("forearm_l").getChild("hand_l");
        this.orb = hand.hasChild("ember_orb") ? hand.getChild("ember_orb") : null;
        this.orbY = orb != null ? orb.getInitialPose().y : 0F;
    }

    @Override
    public void prepareMobModel(SoldierEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        com.warfront.client.ArmPoses.apply(this, entity);
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(SoldierEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Hunched, crouching stance on bent-back legs.
        body.xRot += bodyPitch;
        rightLeg.xRot += legPitch;
        leftLeg.xRot += legPitch;

        // Wings beat faster and wider while moving or fighting.
        float effort = Math.min(1F, limbSwingAmount * 2F + (entity.isAggressive() ? 0.5F : 0F));
        float flap = Mth.sin(ageInTicks * (0.18F + effort * 0.25F)) * (0.08F + effort * 0.3F);
        wingR.zRot = wingZ + flap;
        wingL.zRot = -wingZ - flap;
        wingR.yRot = wingY - effort * 0.2F;
        wingL.yRot = -wingY + effort * 0.2F;

        if (orb != null) {
            orb.y = orbY + Mth.sin(ageInTicks * 0.15F) * 0.6F;
            orb.yRot = ageInTicks * 0.12F;
            orb.xRot = ageInTicks * 0.07F;
        }

        // Lazy tail sway, travelling down the segments.
        for (int i = 0; i < tail.length; i++) {
            tail[i].yRot = Mth.sin(ageInTicks * 0.07F - i * 0.6F) * 0.12F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        super.translateToHand(side, poseStack);
        // Imp arms are longer than a player's: move held items down to the clawed hand.
        poseStack.translate(0.0F, 0.2F, 0.0F);
    }
}
