package com.warfront.client.model;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

/**
 * The Armored Moa, the angels' war beast: a tall running bird. Geometry comes from {@link UnitGeometry#angel_moa()};
 * this class only animates it: long striding legs, a bobbing neck, wings that flare when it fights.
 */
public class MoaModel extends HumanoidModel<SoldierEntity> {
    private final ModelPart[] rig;
    private final PartPose[] rest;

    public MoaModel(ModelPart root) {
        super(root);
        this.rig = new ModelPart[]{head, body, rightArm, leftArm, rightLeg, leftLeg};
        this.rest = new PartPose[rig.length];
        for (int i = 0; i < rig.length; i++) rest[i] = rig[i].getInitialPose();
    }

    @Override
    public void setupAnim(SoldierEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        for (int i = 0; i < rig.length; i++) rig[i].loadPose(rest[i]);
        float stride = Math.min(limbSwingAmount, 1F);
        rightLeg.xRot = Mth.cos(limbSwing * 0.7F) * 1.1F * stride;
        leftLeg.xRot = Mth.cos(limbSwing * 0.7F + Mth.PI) * 1.1F * stride;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = rest[0].xRot + Mth.sin(limbSwing * 1.4F) * 0.12F * stride + headPitch * Mth.DEG_TO_RAD * 0.3F;
        body.y = rest[1].y - Math.abs(Mth.sin(limbSwing * 0.7F)) * 0.8F * stride;
        head.y = rest[0].y + body.y - rest[1].y;
        boolean fighting = entity.isAggressive();
        float flare = fighting ? 0.6F + Mth.sin(ageInTicks * 0.5F) * 0.2F : Mth.sin(ageInTicks * 0.05F) * 0.05F;
        rightArm.zRot = rest[2].zRot + flare;
        leftArm.zRot = rest[3].zRot - flare;
        rightArm.y = rest[2].y + body.y - rest[1].y;
        leftArm.y = rest[3].y + body.y - rest[1].y;
        if (attackTime > 0) head.xRot += Mth.sin(attackTime * Mth.PI) * 0.8F;   // a pecking strike
    }
}
