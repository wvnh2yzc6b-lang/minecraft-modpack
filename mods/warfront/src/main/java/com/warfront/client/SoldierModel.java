package com.warfront.client;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/** Player-shaped model that poses arms for drawn bows, raised shields and held weapons. */
public class SoldierModel extends PlayerModel<SoldierEntity> {
    public SoldierModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void prepareMobModel(SoldierEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        rightArmPose = entity.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        leftArmPose = entity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        if (entity.isUsingItem()) {
            ItemStack using = entity.getUseItem();
            boolean main = entity.getUsedItemHand() == InteractionHand.MAIN_HAND;
            if (using.getItem() instanceof BowItem) {
                if (main) rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
                else leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            } else if (using.getItem() instanceof ShieldItem) {
                if (main) rightArmPose = HumanoidModel.ArmPose.BLOCK;
                else leftArmPose = HumanoidModel.ArmPose.BLOCK;
            }
        }
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }
}
