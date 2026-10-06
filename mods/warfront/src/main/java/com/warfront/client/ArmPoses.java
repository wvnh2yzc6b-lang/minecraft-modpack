package com.warfront.client;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/** Poses arms for drawn bows, raised shields and held weapons. Shared by every soldier body. */
public final class ArmPoses {
    private ArmPoses() {}

    public static void apply(HumanoidModel<SoldierEntity> model, SoldierEntity entity) {
        model.rightArmPose = entity.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        model.leftArmPose = entity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        if (entity.isUsingItem()) {
            ItemStack using = entity.getUseItem();
            boolean main = entity.getUsedItemHand() == InteractionHand.MAIN_HAND;
            if (using.getItem() instanceof BowItem) {
                if (main) model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
                else model.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            } else if (using.getItem() instanceof ShieldItem) {
                if (main) model.rightArmPose = HumanoidModel.ArmPose.BLOCK;
                else model.leftArmPose = HumanoidModel.ArmPose.BLOCK;
            }
        }
    }
}
