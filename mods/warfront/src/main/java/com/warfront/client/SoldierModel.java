package com.warfront.client;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Player-shaped model for standard humanoid soldiers. */
public class SoldierModel extends PlayerModel<SoldierEntity> {
    public SoldierModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void prepareMobModel(SoldierEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        ArmPoses.apply(this, entity);
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }
}
