package com.warfront.entity.ai;

import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.entity.work.WorkSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Farmers tend the land around their post: harvest ripe crops (and melons and pumpkins), replant
 * from the seeds they carry, till earth near water, and carry the harvest to the nearest chest.
 */
public class FarmerGoal extends Goal {
    public static final int FIELD_RADIUS = 8;
    private static final int CARRY_LIMIT = 32;

    private enum Job { HARVEST, PLANT, TILL, DEPOSIT, FUEL }

    private final SoldierEntity farmer;
    @Nullable private Job job;
    @Nullable private BlockPos target;
    private int cooldown;
    private int ticks;

    public FarmerGoal(SoldierEntity farmer) {
        this.farmer = farmer;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (farmer.getRole() != SoldierRole.FARMER || farmer.getOrder() != Order.HOLD || farmer.isRouting()) return false;
        if (--cooldown > 0) return false;
        cooldown = 20;
        return findJob();
    }

    @Override
    public boolean canContinueToUse() {
        return job != null && target != null && ticks < 400 && farmer.getOrder() == Order.HOLD;
    }

    @Override
    public void start() {
        ticks = 0;
        moveTo();
    }

    @Override
    public void stop() {
        job = null;
        target = null;
        farmer.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (job == null || target == null) return;
        ticks++;
        farmer.getLookControl().setLookAt(Vec3.atCenterOf(target));
        if (farmer.position().distanceToSqr(Vec3.atBottomCenterOf(target)) > 2.4 * 2.4) {
            if (ticks % 20 == 0 || farmer.getNavigation().isDone()) moveTo();
            return;
        }
        farmer.getNavigation().stop();
        if (ticks % 8 != 0) return;   // a short beat to line up the swing
        work();
        stop();
        cooldown = 6;
    }

    private void moveTo() {
        if (target != null) farmer.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.9);
    }

    // ------------------------------------------------------------------ finding work

    private boolean findJob() {
        BlockPos post = farmer.getPost();
        if (post == null || !(farmer.level() instanceof ServerLevel level)) return false;
        SimpleContainer bag = farmer.getWorkItems();
        if (produce(bag) >= CARRY_LIMIT || !WorkSites.hasSpace(bag)) {
            // Shards go to the nearest Mana Well, food to the Mess Hall, the rest to a chest.
            if (WorkSites.count(bag, com.warfront.registry.WFRegistry.MANA_SHARD.get()) > 0) {
                BlockPos well = WorkSites.findWell(level, post);
                if (well != null) return set(Job.FUEL, well);
            }
            if (hasFood(bag)) {
                BlockPos hall = WorkSites.findMessHall(level, post);
                if (hall != null) return set(Job.DEPOSIT, hall);
            }
            BlockPos chest = WorkSites.findStorage(level, post, WorkSites::hasSpace);
            if (chest != null) return set(Job.DEPOSIT, chest);
        }
        BlockPos plant = null, till = null;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        boolean seeds = hasSeeds(bag);
        for (BlockPos p : BlockPos.betweenClosed(post.offset(-FIELD_RADIUS, -2, -FIELD_RADIUS), post.offset(FIELD_RADIUS, 2, FIELD_RADIUS))) {
            BlockState s = level.getBlockState(p);
            double d = p.distSqr(farmer.blockPosition());
            if (ripe(level, p, s)) {
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            } else if (seeds && plant == null && s.getBlock() instanceof FarmBlock && level.isEmptyBlock(p.above())) {
                plant = p.above().immutable();
            } else if (seeds && till == null && tillable(level, p, s)) {
                till = p.immutable();
            }
        }
        if (best != null) return set(Job.HARVEST, best);
        if (plant != null) return set(Job.PLANT, plant);
        if (till != null) return set(Job.TILL, till);
        return false;
    }

    private boolean set(Job j, BlockPos p) {
        job = j;
        target = p;
        return true;
    }

    private static boolean ripe(ServerLevel level, BlockPos p, BlockState s) {
        if (s.getBlock() instanceof CropBlock crop) return crop.isMaxAge(s);
        if (s.is(Blocks.MELON) || s.is(Blocks.PUMPKIN)) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState n = level.getBlockState(p.relative(d));
                if (n.getBlock() instanceof AttachedStemBlock && n.getValue(AttachedStemBlock.FACING) == d.getOpposite()) return true;
            }
        }
        return false;
    }

    /** Dirt or grass under open air, within reach of water as vanilla farmland hydration counts it. */
    private static boolean tillable(ServerLevel level, BlockPos p, BlockState s) {
        if (!(s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK))) return false;
        if (!level.isEmptyBlock(p.above())) return false;
        for (BlockPos w : BlockPos.betweenClosed(p.offset(-4, 0, -4), p.offset(4, 1, 4))) {
            if (level.getFluidState(w).is(net.minecraft.tags.FluidTags.WATER)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ doing work

    private void work() {
        if (!(farmer.level() instanceof ServerLevel level) || target == null || job == null) return;
        SimpleContainer bag = farmer.getWorkItems();
        farmer.swing(InteractionHand.MAIN_HAND);
        BlockState s = level.getBlockState(target);
        switch (job) {
            case HARVEST -> {
                if (!ripe(level, target, s)) return;
                for (ItemStack drop : Block.getDrops(s, level, target, null, farmer, farmer.getMainHandItem())) {
                    ItemStack rest = WorkSites.insert(bag, drop);
                    if (!rest.isEmpty()) Block.popResource(level, target, rest);
                }
                level.destroyBlock(target, false, farmer);
                if (s.getBlock() instanceof CropBlock crop) replant(level, bag, crop);
            }
            case PLANT -> {
                Block seed = seedBlock(bag);
                if (seed == null || !level.isEmptyBlock(target)) return;
                WorkSites.takeOne(bag, seed.asItem());
                level.setBlockAndUpdate(target, seed.defaultBlockState());
                level.playSound(null, target, SoundEvents.CROP_PLANTED, SoundSource.NEUTRAL, 1F, 1F);
            }
            case TILL -> {
                if (!tillable(level, target, s)) return;
                level.setBlockAndUpdate(target, Blocks.FARMLAND.defaultBlockState());
                level.playSound(null, target, SoundEvents.HOE_TILL, SoundSource.NEUTRAL, 1F, 1F);
                level.gameEvent(farmer, GameEvent.BLOCK_CHANGE, target);
            }
            case FUEL -> {
                if (!(level.getBlockEntity(target) instanceof com.warfront.block.ManaWellBlockEntity well)) return;
                for (int i = 0; i < bag.getContainerSize(); i++) {
                    ItemStack stack = bag.getItem(i);
                    if (com.warfront.block.ManaWellBlockEntity.fuelValue(stack) > 0F) bag.setItem(i, well.fuelInput.insertItem(0, stack, false));
                }
                level.playSound(null, target, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            case DEPOSIT -> {
                Container chest = WorkSites.containerAt(level, target);
                if (chest == null) return;
                for (int i = 0; i < bag.getContainerSize(); i++) {
                    ItemStack stack = bag.getItem(i);
                    if (stack.isEmpty() || isSeed(stack.getItem()) && seedsKept(bag, i)) continue;
                    bag.setItem(i, WorkSites.insert(chest, stack));
                }
                level.playSound(null, target, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.1F);
            }
        }
    }

    private void replant(ServerLevel level, SimpleContainer bag, CropBlock crop) {
        Item seed = crop.getCloneItemStack(level, target, crop.defaultBlockState()).getItem();
        if (!level.isEmptyBlock(target) || !level.getBlockState(target.below()).is(Blocks.FARMLAND)) return;
        if (WorkSites.takeOne(bag, seed)) {
            level.setBlockAndUpdate(target, crop.defaultBlockState());
            level.playSound(null, target, SoundEvents.CROP_PLANTED, SoundSource.NEUTRAL, 1F, 1F);
        }
    }

    /** Keeps the first 32 seeds of each kind for replanting. */
    private static boolean seedsKept(SimpleContainer bag, int slot) {
        Item seed = bag.getItem(slot).getItem();
        int before = 0;
        for (int i = 0; i < slot; i++) if (bag.getItem(i).is(seed)) before += bag.getItem(i).getCount();
        return before < 32;
    }

    public static boolean isSeed(Item item) {
        return item instanceof BlockItem b && b.getBlock() instanceof CropBlock;
    }

    private static boolean hasSeeds(SimpleContainer bag) {
        return seedBlock(bag) != null;
    }

    @Nullable
    private static Block seedBlock(SimpleContainer bag) {
        for (int i = 0; i < bag.getContainerSize(); i++) {
            Item it = bag.getItem(i).getItem();
            if (isSeed(it)) return ((BlockItem) it).getBlock();
        }
        return null;
    }

    private static boolean hasFood(SimpleContainer bag) {
        for (int i = 0; i < bag.getContainerSize(); i++) {
            if (com.warfront.upkeep.MessHallBlockEntity.isFood(bag.getItem(i))) return true;
        }
        return false;
    }

    private static int produce(SimpleContainer bag) {
        int n = 0;
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack s = bag.getItem(i);
            if (!s.isEmpty() && !isSeed(s.getItem())) n += s.getCount();
        }
        return n;
    }

}
