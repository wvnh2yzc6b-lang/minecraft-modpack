package com.warfront.entity.ai;

import com.warfront.block.WarStandardBlock;
import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Raiders that cannot reach their objective hack through whatever is in the way: walls, doors,
 * fences. Harder blocks take longer; anything above the configured hardness (obsidian by
 * default) stops them. Respects the mobGriefing game rule.
 */
public class BreachGoal extends Goal {
    private final SoldierEntity soldier;
    @Nullable private BlockPos target;
    @Nullable private BlockState targetState;
    private int progress;
    private int needed;
    private int lastStage = -1;

    public BreachGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (soldier.getOwnerUUID() != null || soldier.isRouting() || !soldier.isStuck()) return false;
        if (!WFConfig.RAIDERS_BREAK_BLOCKS.get()) return false;
        if (!(soldier.level() instanceof ServerLevel level)
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        Vec3 objective = soldier.currentObjective();
        if (objective == null) return false;
        target = findObstacle(objective);
        return target != null;
    }

    @Nullable
    private BlockPos findObstacle(Vec3 objective) {
        Vec3 to = objective.subtract(soldier.position());
        Direction dir = Direction.getNearest(to.x, 0, to.z);
        BlockPos feet = soldier.blockPosition();
        BlockPos ahead = feet.relative(dir);
        BlockPos[] candidates = to.y > 1.5
                ? new BlockPos[]{feet.above(2), ahead.above(), ahead, ahead.above(2)}
                : new BlockPos[]{ahead.above(), ahead, feet.above(2)};
        for (BlockPos p : candidates) {
            if (breakable(p)) return p;
        }
        return null;
    }

    private boolean breakable(BlockPos pos) {
        BlockState state = soldier.level().getBlockState(pos);
        if (state.isAir() || state.getCollisionShape(soldier.level(), pos).isEmpty()) return false;
        if (state.getBlock() instanceof WarStandardBlock) return false;
        float hardness = state.getDestroySpeed(soldier.level(), pos);
        if (hardness < 0 || hardness > WFConfig.MAX_BREAK_HARDNESS.get()) return false;
        return EventHooks.onEntityDestroyBlock(soldier, pos, state);
    }

    @Override
    public void start() {
        targetState = soldier.level().getBlockState(target);
        float hardness = targetState.getDestroySpeed(soldier.level(), target);
        needed = Math.max(15, (int) (hardness * 30) - soldier.getRole().rank);
        progress = 0;
        lastStage = -1;
        soldier.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && targetState != null && progress < needed && !soldier.isRouting()
                && soldier.level().getBlockState(target) == targetState
                && soldier.position().distanceToSqr(Vec3.atCenterOf(target)) < 3.5 * 3.5;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) return;
        soldier.getLookControl().setLookAt(Vec3.atCenterOf(target));
        progress++;
        if (progress % 10 == 0) {
            soldier.swing(InteractionHand.MAIN_HAND);
            SoundType sound = targetState.getSoundType(soldier.level(), target, soldier);
            soldier.level().playSound(null, target, sound.getHitSound(), soldier.getSoundSource(), 1.0F, 0.8F);
        }
        int stage = (int) (progress * 10.0F / needed);
        if (stage != lastStage) {
            soldier.level().destroyBlockProgress(soldier.getId(), target, stage);
            lastStage = stage;
        }
        if (progress >= needed) {
            com.warfront.upkeep.RaidDamage.log(soldier.level(), target);
            soldier.level().destroyBlock(target, true, soldier);
            soldier.resetStuck();
        }
    }

    @Override
    public void stop() {
        if (target != null) soldier.level().destroyBlockProgress(soldier.getId(), target, -1);
        target = null;
        targetState = null;
    }
}
