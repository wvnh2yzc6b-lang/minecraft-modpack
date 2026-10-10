package com.warfront.client.model;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

/**
 * The Bone Stalker, the demons' war beast: a skeletal predator prowling on all fours, with insect wings and a
 * scorpion tail. Geometry comes from {@link UnitGeometry#demon_beast()}; this class only animates it: a four-legged
 * prowl (diagonal pairs), wings that buzz in a fight, a gaping jaw, and a tail that whips the stinger forward to strike.
 */
public class BoneStalkerModel extends HumanoidModel<SoldierEntity> {
    private final ModelPart jaw;
    private final ModelPart[] wings;
    private final ModelPart[] tail = new ModelPart[6];
    private final ModelPart[] rig;
    private final PartPose[] rest;

    public BoneStalkerModel(ModelPart root) {
        super(root);
        this.jaw = head.getChild("jaw");
        this.wings = new ModelPart[]{
                body.getChild("wing_root_r").getChild("wing_r"), body.getChild("wing_root_l").getChild("wing_l"),
                body.getChild("hindwing_root_r").getChild("hindwing_r"), body.getChild("hindwing_root_l").getChild("hindwing_l")};
        ModelPart seg = body.getChild("tail_0");
        for (int i = 0; i < tail.length; i++) {
            tail[i] = seg;
            if (i + 1 < tail.length) seg = seg.getChild("tail_" + (i + 1));
        }
        this.rig = new ModelPart[]{head, body, rightArm, leftArm, rightLeg, leftLeg};
        this.rest = new PartPose[rig.length];
        for (int i = 0; i < rig.length; i++) rest[i] = rig[i].getInitialPose();
    }

    @Override
    public void setupAnim(SoldierEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        for (int i = 0; i < rig.length; i++) rig[i].loadPose(rest[i]);
        float stride = Math.min(limbSwingAmount, 1F);
        float walk = limbSwing * 0.8F;
        // Diagonal pairs move together, like a hound: front right with hind left.
        rightArm.xRot = rest[2].xRot + Mth.cos(walk) * 0.55F * stride;
        leftLeg.xRot = rest[5].xRot + Mth.cos(walk) * 0.55F * stride;
        leftArm.xRot = rest[3].xRot + Mth.cos(walk + Mth.PI) * 0.55F * stride;
        rightLeg.xRot = rest[4].xRot + Mth.cos(walk + Mth.PI) * 0.55F * stride;
        body.y = rest[1].y - Math.abs(Mth.sin(walk)) * 0.5F * stride;
        head.y = rest[0].y + body.y - rest[1].y;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot = rest[0].xRot + headPitch * Mth.DEG_TO_RAD * 0.3F;

        boolean fighting = entity.isAggressive();
        jaw.xRot = jaw.getInitialPose().xRot + (fighting ? 0.2F + Mth.sin(ageInTicks * 0.4F) * 0.15F : Mth.sin(ageInTicks * 0.1F) * 0.05F);
        // Wings: still when prowling, a fast buzz in a fight.
        float buzz = fighting ? Mth.sin(ageInTicks * 2.6F) * 0.25F : Mth.sin(ageInTicks * 0.12F) * 0.04F;
        for (int i = 0; i < wings.length; i++) {
            float side = i % 2 == 0 ? -1F : 1F;
            wings[i].zRot = wings[i].getInitialPose().zRot + side * buzz * (i < 2 ? 1F : 0.8F);
        }
        // The tail sways; a strike whips the stinger forward over the back.
        float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0F;
        for (int i = 0; i < tail.length; i++) {
            ModelPart t = tail[i];
            t.yRot = t.getInitialPose().yRot + (i == 0 ? Mth.sin(ageInTicks * 0.07F) * 0.15F : 0F);
            t.xRot = t.getInitialPose().xRot + strike * (i < 2 ? -0.1F : 0.28F) + Mth.sin(ageInTicks * 0.1F + i) * 0.02F;
        }
        if (strike > 0) head.xRot += strike * 0.25F;
    }
}
