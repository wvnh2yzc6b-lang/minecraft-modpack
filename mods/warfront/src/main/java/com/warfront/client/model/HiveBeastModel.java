package com.warfront.client.model;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Deepmaw, the Hive's war beast: a spiked frill, tusk-like mandibles and one huge hooked forelimb.
 * Geometry comes from {@link UnitGeometry#hive_beast()}; this class only animates it.
 */
public class HiveBeastModel extends HumanoidModel<SoldierEntity> {
    private final ModelPart mandibleR;
    private final ModelPart mandibleL;
    private final ModelPart claw;
    private final float mandibleZ;
    private final float clawX;
    private final float bodyPitch;
    private final float legPitch;
    private final float raptorZ;

    public HiveBeastModel(ModelPart root) {
        super(root);
        this.mandibleR = head.getChild("mandible_r");
        this.mandibleL = head.getChild("mandible_l");
        this.claw = rightArm.getChild("forearm_r").getChild("claw_r");
        this.mandibleZ = mandibleR.getInitialPose().zRot;
        this.clawX = claw.getInitialPose().xRot;
        this.bodyPitch = body.getInitialPose().xRot;
        this.legPitch = rightLeg.getInitialPose().xRot;
        this.raptorZ = rightArm.getInitialPose().zRot;
    }

    @Override
    public void setupAnim(SoldierEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        body.xRot += bodyPitch;
        rightLeg.xRot += legPitch;
        leftLeg.xRot += legPitch;
        rightArm.zRot += raptorZ;
        // The raptorial claw rises when it is ready to strike; mandibles work slowly, faster in a fight.
        boolean fighting = entity.isAggressive();
        rightArm.xRot -= fighting ? 0.6F : 0.15F;
        claw.xRot = clawX - (fighting ? 0.4F : 0F) + Mth.sin(ageInTicks * 0.1F) * 0.05F;
        float chew = Mth.sin(ageInTicks * (fighting ? 0.5F : 0.12F)) * (fighting ? 0.18F : 0.06F);
        mandibleR.zRot = mandibleZ + chew;
        mandibleL.zRot = -mandibleZ - chew;
    }
}
