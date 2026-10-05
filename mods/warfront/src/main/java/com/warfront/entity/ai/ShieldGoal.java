package com.warfront.entity.ai;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ShieldItem;

/** Shieldbearers raise their shields while enemies close in, and lower them to strike. */
public class ShieldGoal extends Goal {
    private final SoldierEntity soldier;

    public ShieldGoal(SoldierEntity soldier) {
        this.soldier = soldier;
    }

    private boolean shouldBlock() {
        if (soldier.getRole() != SoldierRole.SHIELDBEARER || soldier.isRouting()) return false;
        if (!(soldier.getOffhandItem().getItem() instanceof ShieldItem)) return false;
        LivingEntity target = soldier.getTarget();
        if (target == null || !target.isAlive()) return false;
        double d2 = soldier.distanceToSqr(target);
        return d2 > 3.0 * 3.0 && d2 < 20.0 * 20.0;
    }

    @Override
    public boolean canUse() {
        return shouldBlock();
    }

    @Override
    public boolean canContinueToUse() {
        return shouldBlock();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!soldier.isUsingItem()) soldier.startUsingItem(InteractionHand.OFF_HAND);
    }

    @Override
    public void stop() {
        if (soldier.isUsingItem()) soldier.stopUsingItem();
    }
}
