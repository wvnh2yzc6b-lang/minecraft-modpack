package com.warfront.entity.ai;

import com.warfront.army.Order;
import com.warfront.entity.SoldierEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Keeps a soldier in its formation slot, facing the formation's front when idle. */
public class FormationMoveGoal extends Goal {
    private final SoldierEntity soldier;
    private int repath;

    public FormationMoveGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !soldier.isRouting() && soldier.getSlot() != null
                && !(soldier.getOrder() == Order.CHARGE && soldier.getTarget() != null);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 slot = soldier.getSlot();
        if (slot == null) return;
        double dx = slot.x - soldier.getX();
        double dz = slot.z - soldier.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 1.0) {
            if (--repath <= 0 || soldier.getNavigation().isDone()) {
                repath = 10;
                double speed = soldier.isMarchLeader() ? 0.75 : dist > 10 ? 1.35 : dist > 3 ? 1.1 : 0.85;
                soldier.getNavigation().moveTo(slot.x, slot.y, slot.z, speed);
            }
        } else {
            if (!soldier.getNavigation().isDone()) soldier.getNavigation().stop();
            if (soldier.getTarget() == null) {
                float rad = soldier.getAnchorYaw() * Mth.DEG_TO_RAD;
                soldier.getLookControl().setLookAt(soldier.getX() - Mth.sin(rad) * 8.0, soldier.getEyeY(),
                        soldier.getZ() + Mth.cos(rad) * 8.0);
            }
        }
    }
}
