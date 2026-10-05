package com.warfront.entity.ai;

import com.warfront.entity.SoldierEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Melee that respects formation discipline: only engage enemies within the order's leash. */
public class SoldierMeleeGoal extends MeleeAttackGoal {
    private final SoldierEntity soldier;

    public SoldierMeleeGoal(SoldierEntity soldier) {
        super(soldier, 1.15, true);
        this.soldier = soldier;
    }

    private boolean allowed() {
        return soldier.getRole().melee && !soldier.isRouting() && soldier.withinLeash(soldier.getTarget());
    }

    @Override
    public boolean canUse() {
        return allowed() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return allowed() && super.canContinueToUse();
    }
}
