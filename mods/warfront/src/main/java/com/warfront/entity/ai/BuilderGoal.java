package com.warfront.entity.ai;

import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.entity.work.Blueprint;
import com.warfront.entity.work.WorkSites;
import com.warfront.config.WFConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builders keep their base standing. They compare the world with their blueprint and put back
 * whatever is missing, lowest blocks first, using blocks they carry or fetch from a nearby chest.
 */
public class BuilderGoal extends Goal {
    private static final double REACH = 3.6;

    private enum Job { BUILD, FETCH }

    private final SoldierEntity builder;
    @Nullable private Job job;
    @Nullable private BlockPos target;
    @Nullable private BlockState wanted;
    private final Map<BlockPos, Long> skipUntil = new HashMap<>();
    private int cooldown;
    private int ticks;
    private int idleNotice;

    public BuilderGoal(SoldierEntity builder) {
        this.builder = builder;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (builder.getRole() != SoldierRole.BUILDER || builder.getOrder() != Order.HOLD || builder.isRouting()) return false;
        if (builder.getBlueprint() == null || --cooldown > 0) return false;
        cooldown = 30;
        return findJob();
    }

    @Override
    public boolean canContinueToUse() {
        return job != null && target != null && ticks < 300 && builder.getOrder() == Order.HOLD;
    }

    @Override
    public void start() {
        ticks = 0;
        moveTo();
    }

    @Override
    public void stop() {
        if (job == Job.BUILD && target != null && ticks >= 300) {
            skipUntil.put(target, builder.level().getGameTime() + 1200);   // unreachable for now
        }
        job = null;
        target = null;
        wanted = null;
        builder.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (job == null || target == null) return;
        ticks++;
        builder.getLookControl().setLookAt(Vec3.atCenterOf(target));
        double reach = job == Job.BUILD ? REACH : 2.4;
        if (builder.getEyePosition().distanceToSqr(Vec3.atCenterOf(target)) > reach * reach) {
            if (ticks % 20 == 0 || builder.getNavigation().isDone()) moveTo();
            return;
        }
        builder.getNavigation().stop();
        if (ticks % 10 != 0) return;   // the swing lands on the beat
        if (job == Job.FETCH) {
            fetch();
            stop();
            cooldown = 5;
        } else if (place()) {
            // Keep working along the wall without pausing to re-plan.
            ticks = 0;
            if (!findJob()) stop();
        } else {
            stop();
        }
    }

    private void moveTo() {
        if (target != null) builder.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.9);
    }

    private boolean findJob() {
        if (!(builder.level() instanceof ServerLevel level)) return false;
        Blueprint bp = builder.getBlueprint();
        if (bp == null) return false;
        long now = level.getGameTime();
        skipUntil.values().removeIf(t -> t < now);
        List<Map.Entry<BlockPos, BlockState>> damage = bp.damage(level, builder.blockPosition(), 96);
        if (damage.isEmpty()) return false;
        SimpleContainer bag = builder.getWorkItems();
        boolean free = !WFConfig.BUILDERS_NEED_MATERIALS.get();
        for (Map.Entry<BlockPos, BlockState> e : damage) {
            if (skipUntil.containsKey(e.getKey())) continue;
            if (free || WorkSites.count(bag, Blueprint.itemFor(e.getValue())) > 0) {
                job = Job.BUILD;
                target = e.getKey();
                wanted = e.getValue();
                return true;
            }
        }
        // Nothing to hand: look for a chest holding any of the missing blocks.
        BlockPos chest = WorkSites.findStorage(level, bp.origin(), c -> {
            for (Map.Entry<BlockPos, BlockState> e : damage) if (WorkSites.count(c, Blueprint.itemFor(e.getValue())) > 0) return true;
            return false;
        });
        if (chest != null && WorkSites.hasSpace(bag)) {
            job = Job.FETCH;
            target = chest;
            return true;
        }
        if (++idleNotice % 6 == 0) {   // out of materials: grumble now and then
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, builder.getX(), builder.getEyeY() + 0.4, builder.getZ(), 1, 0.2, 0.1, 0.2, 0);
        }
        return false;
    }

    private void fetch() {
        if (!(builder.level() instanceof ServerLevel level) || target == null) return;
        Container chest = WorkSites.containerAt(level, target);
        Blueprint bp = builder.getBlueprint();
        if (chest == null || bp == null) return;
        Map<Item, Integer> need = new HashMap<>();
        for (Map.Entry<BlockPos, BlockState> e : bp.damage(level, builder.blockPosition(), 256)) {
            need.merge(Blueprint.itemFor(e.getValue()), 1, Integer::sum);
        }
        SimpleContainer bag = builder.getWorkItems();
        need.forEach((item, n) -> WorkSites.move(chest, bag, item, Math.min(64, n) - WorkSites.count(bag, item)));
        builder.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, target, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    private boolean place() {
        if (!(builder.level() instanceof ServerLevel level) || target == null || wanted == null) return false;
        if (!Blueprint.isMissing(level, target)) return true;   // someone else filled it; move on
        if (!level.isUnobstructed(wanted, target, CollisionContext.empty())) return false;
        Item item = Blueprint.itemFor(wanted);
        if (WFConfig.BUILDERS_NEED_MATERIALS.get() && !WorkSites.takeOne(builder.getWorkItems(), item)) return false;
        level.setBlock(target, wanted, 3);
        SoundType sound = wanted.getSoundType(level, target, builder);
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1F) / 2F, sound.getPitch() * 0.8F);
        level.gameEvent(builder, GameEvent.BLOCK_PLACE, target);
        builder.swing(InteractionHand.MAIN_HAND);
        return true;
    }
}
