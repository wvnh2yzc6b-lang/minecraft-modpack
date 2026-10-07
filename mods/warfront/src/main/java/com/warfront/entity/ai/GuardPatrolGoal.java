package com.warfront.entity.ai;

import com.warfront.army.Order;
import com.warfront.army.Duty;
import com.warfront.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Units on watch don't stand frozen at their post: every so often they walk a beat around it, stop to look
 * about, then return to their post (FormationMoveGoal takes them back). Guards walk a short beat; patrols
 * range wider and set out more often.
 */
public class GuardPatrolGoal extends Goal {
    private final SoldierEntity guard;
    private int wait;
    private int linger;
    @Nullable private BlockPos spot;

    public GuardPatrolGoal(SoldierEntity guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.wait = 100;
    }

    @Override
    public boolean canUse() {
        if (!guard.onWatch() || guard.getOrder() != Order.HOLD || guard.getTarget() != null) return false;
        if (--wait > 0) return false;
        boolean patrol = guard.getDuty() == Duty.PATROL;
        int beat = guard.getDuty().beat;
        wait = patrol ? 40 + guard.getRandom().nextInt(60) : 160 + guard.getRandom().nextInt(200);
        BlockPos post = guard.getPost();
        if (post == null) return false;
        double a = guard.getRandom().nextDouble() * Math.PI * 2, r = (patrol ? beat / 2.0 : 3) + guard.getRandom().nextDouble() * (beat - (patrol ? beat / 2.0 : 3));
        BlockPos xz = post.offset(Mth.floor(Math.cos(a) * r), 0, Mth.floor(Math.sin(a) * r));
        BlockPos ground = guard.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz);
        if (Math.abs(ground.getY() - post.getY()) > 3) ground = xz;   // indoors or on a wall: stay on this level
        Path path = guard.getNavigation().createPath(ground, 1);
        if (path == null || !path.canReach()) return false;
        spot = ground;
        guard.getNavigation().moveTo(path, patrol ? 0.7 : 0.55);
        linger = patrol ? 20 + guard.getRandom().nextInt(20) : 40 + guard.getRandom().nextInt(40);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return spot != null && guard.getTarget() == null && guard.getOrder() == Order.HOLD && linger > 0;
    }

    @Override
    public void tick() {
        if (!guard.getNavigation().isDone()) return;
        // At the end of the beat: pause and scan the surroundings.
        linger--;
        if (linger % 20 == 0) {
            float yaw = guard.getYRot() + (guard.getRandom().nextFloat() - 0.5F) * 140F;
            float rad = yaw * Mth.DEG_TO_RAD;
            guard.getLookControl().setLookAt(guard.getX() - Mth.sin(rad) * 8, guard.getEyeY(), guard.getZ() + Mth.cos(rad) * 8);
        }
    }

    @Override
    public void stop() {
        spot = null;
    }
}
