package com.warfront.client.model;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

/**
 * The Deepmaw, the Hive's war beast: a hunched crab-beast under a domed shell, with eye stalks,
 * tusk-mandibles and two great pincers. Geometry comes from {@link UnitGeometry#hive_beast()}; this class
 * only animates it.
 */
public class HiveBeastModel extends HumanoidModel<SoldierEntity> {
    private final ModelPart mandibleR;
    private final ModelPart mandibleL;
    private final ModelPart clawR;
    private final ModelPart clawL;
    private final ModelPart tail;
    /** The main parts, whose pivots and base angles vanilla humanoid animation would otherwise reset. */
    private final ModelPart[] rig;
    private final PartPose[] rest;

    public HiveBeastModel(ModelPart root) {
        super(root);
        this.mandibleR = head.getChild("mandible_r");
        this.mandibleL = head.getChild("mandible_l");
        this.clawR = rightArm.getChild("forearm_r").getChild("pincer_r").getChild("claw_r");
        this.clawL = leftArm.getChild("forearm_l").getChild("pincer_l").getChild("claw_l");
        this.tail = body.getChild("tail");
        this.rig = new ModelPart[]{head, body, rightArm, leftArm, rightLeg, leftLeg};
        this.rest = new PartPose[rig.length];
        for (int i = 0; i < rig.length; i++) rest[i] = rig[i].getInitialPose();
    }

    @Override
    public void setupAnim(SoldierEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // Keep vanilla's swing and look angles, but on this body's own pivots and resting pose.
        for (int i = 0; i < rig.length; i++) {
            ModelPart p = rig[i];
            PartPose r = rest[i];
            p.x = r.x;
            p.y = r.y;
            p.z = r.z;
            p.xRot += r.xRot;
            p.zRot += r.zRot;
        }
        // Arm swing is damped: the pincers are heavy. In a fight they rise and snap.
        boolean fighting = entity.isAggressive();
        float swing = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount * 0.3F;
        rightArm.xRot = rest[2].xRot + swing - (fighting ? 0.5F : 0F) - attackTime * 1.2F;
        leftArm.xRot = rest[3].xRot - swing - (fighting ? 0.5F : 0F);
        float snap = fighting ? Math.max(0F, Mth.sin(ageInTicks * 0.45F)) * 0.6F : 0F;
        float idle = Mth.sin(ageInTicks * 0.08F) * 0.08F;
        clawR.xRot = clawR.getInitialPose().xRot - snap + idle;
        clawL.xRot = clawL.getInitialPose().xRot - snap - idle;
        float chew = Mth.sin(ageInTicks * (fighting ? 0.5F : 0.12F)) * (fighting ? 0.18F : 0.06F);
        mandibleR.zRot = mandibleR.getInitialPose().zRot + chew;
        mandibleL.zRot = mandibleL.getInitialPose().zRot - chew;
        tail.xRot = tail.getInitialPose().xRot + Mth.sin(ageInTicks * 0.1F) * 0.06F;
    }
}
