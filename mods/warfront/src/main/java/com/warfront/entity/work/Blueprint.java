package com.warfront.entity.work;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder's memory of the buildings around its post: every solid, placeable block within a box
 * around the post. Anything that later goes missing (broken by raiders, blown up, burned) is
 * something to rebuild. Players' own changes update the blueprint instead (see WorkEvents).
 */
public final class Blueprint {
    public static final int RADIUS = 10;
    public static final int BELOW = 3;
    public static final int ABOVE = 12;

    private final BlockPos origin;
    private final int radius, below, above;
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();

    private Blueprint(BlockPos origin, int radius, int below, int above) {
        this.origin = origin.immutable();
        this.radius = radius;
        this.below = below;
        this.above = above;
    }

    public BlockPos origin() {
        return origin;
    }

    public int size() {
        return blocks.size();
    }

    public boolean covers(BlockPos pos) {
        int dx = pos.getX() - origin.getX(), dy = pos.getY() - origin.getY(), dz = pos.getZ() - origin.getZ();
        return Math.abs(dx) <= radius && Math.abs(dz) <= radius && dy >= -below && dy <= above;
    }

    /** Surveys the area around {@code origin}. */
    public static Blueprint survey(Level level, BlockPos origin) {
        return survey(level, origin, RADIUS, BELOW, ABOVE);
    }

    /** Surveys a box of the given size; the box must fit in RADIUS / BELOW / ABOVE. */
    public static Blueprint survey(Level level, BlockPos origin, int radius, int below, int above) {
        Blueprint bp = new Blueprint(origin, Math.min(radius, RADIUS), Math.min(below, BELOW), Math.min(above, ABOVE));
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -bp.below; dy <= bp.above; dy++) {
            for (int dx = -bp.radius; dx <= bp.radius; dx++) {
                for (int dz = -bp.radius; dz <= bp.radius; dz++) {
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(m);
                    if (worthKeeping(state)) bp.blocks.put(m.immutable(), state);
                }
            }
        }
        return bp;
    }

    /** Structural blocks a builder can carry and place back: no plants, fluids, containers or fire. */
    public static boolean worthKeeping(BlockState state) {
        if (state.isAir() || state.canBeReplaced() || state.hasBlockEntity()) return false;
        if (!state.getFluidState().isEmpty() && !state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) return false;
        if (state.getBlock() instanceof BonemealableBlock && !state.is(BlockTags.DIRT)) return false;
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FIRE) || state.is(BlockTags.CROPS)) return false;
        return itemFor(state) != Items.AIR;
    }

    public static Item itemFor(BlockState state) {
        return state.getBlock().asItem();
    }

    /** Records a block a player placed. */
    public void record(BlockPos pos, BlockState state) {
        if (!covers(pos)) return;
        if (worthKeeping(state)) blocks.put(pos.immutable(), state);
        else blocks.remove(pos);
    }

    /** Forgets a block a player removed on purpose. */
    public void forget(BlockPos pos) {
        blocks.remove(pos);
    }

    /** Whether the world at {@code pos} has lost its block: air, fluid or something flimsy now stands there. */
    public static boolean isMissing(Level level, BlockPos pos) {
        BlockState now = level.getBlockState(pos);
        return now.isAir() || now.canBeReplaced();
    }

    /** Positions that need rebuilding, lowest first (so walls go back up from the ground), then nearest. */
    public List<Map.Entry<BlockPos, BlockState>> damage(Level level, BlockPos from, int limit) {
        List<Map.Entry<BlockPos, BlockState>> out = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            if (level.isLoaded(e.getKey()) && isMissing(level, e.getKey())) out.add(e);
        }
        out.sort(Comparator.<Map.Entry<BlockPos, BlockState>>comparingInt(e -> e.getKey().getY())
                .thenComparingDouble(e -> e.getKey().distSqr(from)));
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    public int countDamage(Level level) {
        int n = 0;
        for (BlockPos p : blocks.keySet()) if (level.isLoaded(p) && isMissing(level, p)) n++;
        return n;
    }

    @Nullable
    public BlockState expected(BlockPos pos) {
        return blocks.get(pos);
    }

    // ------------------------------------------------------------------ persistence

    /** Stored as a palette of block states plus packed offsets from the origin. */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Origin", origin.asLong());
        tag.putIntArray("Bounds", new int[] {radius, below, above});
        Map<BlockState, Integer> palette = new LinkedHashMap<>();
        int[] packed = new int[blocks.size() * 2];
        int i = 0;
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            BlockPos p = e.getKey();
            int dx = p.getX() - origin.getX() + RADIUS, dy = p.getY() - origin.getY() + BELOW, dz = p.getZ() - origin.getZ() + RADIUS;
            packed[i++] = (dx << 16) | (dy << 8) | dz;
            packed[i++] = palette.computeIfAbsent(e.getValue(), s -> palette.size());
        }
        ListTag states = new ListTag();
        for (BlockState s : palette.keySet()) states.add(NbtUtils.writeBlockState(s));
        tag.put("Palette", states);
        tag.putIntArray("Blocks", packed);
        return tag;
    }

    public static Blueprint load(CompoundTag tag, HolderGetter<Block> blocks) {
        int[] b = tag.getIntArray("Bounds");
        Blueprint bp = b.length == 3 ? new Blueprint(BlockPos.of(tag.getLong("Origin")), b[0], b[1], b[2])
                : new Blueprint(BlockPos.of(tag.getLong("Origin")), RADIUS, BELOW, ABOVE);
        ListTag states = tag.getList("Palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>(states.size());
        for (int i = 0; i < states.size(); i++) palette.add(NbtUtils.readBlockState(blocks, states.getCompound(i)));
        int[] packed = tag.getIntArray("Blocks");
        for (int i = 0; i + 1 < packed.length; i += 2) {
            int v = packed[i], idx = packed[i + 1];
            if (idx < 0 || idx >= palette.size()) continue;
            BlockPos p = bp.origin.offset(((v >> 16) & 0xFF) - RADIUS, ((v >> 8) & 0xFF) - BELOW, (v & 0xFF) - RADIUS);
            bp.blocks.put(p, palette.get(idx));
        }
        return bp;
    }
}
