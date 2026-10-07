package com.warfront.client.model;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

/**
 * The Deepmaw, the Hive's war beast: a low lobster-like crawler on eight legs, with two big pincers held out
 * in front. Geometry comes from {@link UnitGeometry#hive_beast()}; this class only animates it.
 */
public class HiveBeastModel extends HumanoidModel<SoldierEntity> {
    private final ModelPart mandibleR;
    private final ModelPart mandibleL;
    private final ModelPart clawR;
    private final ModelPart clawL;
    private final ModelPart tail;
    /** Walking legs, right side then left, front to back; their hips and upper segments. */
    private final ModelPart[] hips = new ModelPart[8];
    private final ModelPart[] uppers = new ModelPart[8];
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
        for (int i = 0; i < 8; i++) {
            String name = "leg_" + (i < 4 ? "r" : "l") + (i % 4);
            hips[i] = body.getChild(name);
            uppers[i] = hips[i].getChild(name + "_upper");
        }
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
            p.yRot += r.yRot;
            p.zRot += r.zRot;
        }
        // Eight-legged crawl: alternate legs move together (front right with second left, and so on). A leg
        // sweeps forward while its knee lifts, then plants and pushes back.
        float stride = Math.min(limbSwingAmount, 1F);
        for (int i = 0; i < 8; i++) {
            float phase = limbSwing * 1.2F + ((i + i / 4) % 2 == 0 ? 0F : Mth.PI);
            float side = i < 4 ? 1F : -1F;
            hips[i].yRot = hips[i].getInitialPose().yRot + Mth.cos(phase) * 0.35F * stride * side;
            uppers[i].zRot = uppers[i].getInitialPose().zRot + Math.max(0F, Mth.sin(phase)) * 0.35F * stride * side;
        }
        body.y = rest[1].y - Math.abs(Mth.sin(limbSwing * 1.2F)) * 0.4F * stride;
        // The pincers barely swing; in a fight they lift and snap, and the right one strikes.
        boolean fighting = entity.isAggressive();
        float sway = Mth.cos(limbSwing * 0.6F) * stride * 0.08F;
        rightArm.xRot = rest[2].xRot + sway - (fighting ? 0.2F : 0F) - attackTime * 0.6F;
        leftArm.xRot = rest[3].xRot - sway - (fighting ? 0.2F : 0F);
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
