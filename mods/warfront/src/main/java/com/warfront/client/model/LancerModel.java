package com.warfront.client.model;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Hive Lancer-Drone: four arms, antennae and bent-back legs, with a spear held in both upper hands. Geometry
 * comes from
 * {@link UnitGeometry#hive_lancer()}; this class only animates it.
 */
public class LancerModel extends HumanoidModel<SoldierEntity> {
    private final ModelPart lowerArmR;
    private final ModelPart lowerArmL;
    private final ModelPart antennaR;
    private final ModelPart antennaL;
    private final float lowerArmX;
    private final float antennaX;
    private final float bodyPitch;
    private final float legPitch;

    public LancerModel(ModelPart root) {
        super(root);
        this.lowerArmR = body.getChild("lower_arm_r");
        this.lowerArmL = body.getChild("lower_arm_l");
        this.antennaR = head.getChild("antenna_r");
        this.antennaL = head.getChild("antenna_l");
        this.lowerArmX = lowerArmR.getInitialPose().xRot;
        this.antennaX = antennaR.getInitialPose().xRot;
        this.bodyPitch = body.getInitialPose().xRot;
        this.legPitch = rightLeg.getInitialPose().xRot;
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
        body.xRot += bodyPitch;
        rightLeg.xRot += legPitch;
        leftLeg.xRot += legPitch;
        // Upper arms hold the spear in both hands at the ready (same pose as SPEAR_ARM_R/L in hive_units.py). Both
        // arms move together so the front hand stays on the haft; an attack thrusts the spear forward.
        float carry = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount * 0.12F - attackTime * 0.9F;
        rightArm.xRot = -0.2F + carry;
        rightArm.yRot = 0F;
        rightArm.zRot = 0F;
        leftArm.xRot = -0.9F + carry;
        leftArm.yRot = 0F;
        leftArm.zRot = 0.6F;
        // The lower arms swing against the stride; antennae twitch.
        float swing = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount * 0.6F;
        lowerArmR.xRot = lowerArmX + swing;
        lowerArmL.xRot = lowerArmX - swing;
        float twitch = Mth.sin(ageInTicks * 0.21F) * 0.08F + Mth.sin(ageInTicks * 0.53F) * 0.03F;
        antennaR.xRot = antennaX + twitch;
        antennaL.xRot = antennaX - twitch * 0.8F;
    }
}
