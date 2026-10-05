package com.warfront.entity.ai;

import com.warfront.entity.SoldierEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

/** A soldier whose morale broke flees away from the nearest threat until it recovers. */
public class RoutGoal extends Goal {
    private final SoldierEntity soldier;
    private int repath;

    public RoutGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return soldier.isRouting();
    }

    @Override
    public void start() {
        repath = 0;
        if (soldier.isUsingItem()) soldier.stopUsingItem();
    }

    @Override
    public void tick() {
        if (--repath > 0) return;
        repath = 20;
        Vec3 threat = threat();
        Vec3 pos = threat != null
                ? DefaultRandomPos.getPosAway(soldier, 16, 7, threat)
                : DefaultRandomPos.getPos(soldier, 10, 7);
        if (pos != null) soldier.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.35);
    }

    @Nullable
    private Vec3 threat() {
        LivingEntity attacker = soldier.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) return attacker.position();
        return soldier.level().getEntitiesOfClass(LivingEntity.class, soldier.getBoundingBox().inflate(16),
                        soldier::isEnemy).stream()
                .min(Comparator.comparingDouble(soldier::distanceToSqr))
                .map(LivingEntity::position)
                .orElse(null);
    }
}
