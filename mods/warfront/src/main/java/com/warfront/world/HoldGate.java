package com.warfront.world;

import com.mojang.datafixers.util.Pair;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * The dwarves' starting hold: the nearest mountain gets a carved stone gate flanked by braziers, a 3x3 tunnel about 12
 * blocks into the slope and a starter chest at its end. The new dwarf starts there with their respawn set.
 */
public final class HoldGate {
    public static final int SEARCH_RADIUS = 1600;
    public static final int TUNNEL = 12;

    private HoldGate() {}

    /** Sends a new dwarf player to a hold gate on the nearest mountain; false (nothing moved) if none is in reach. */
    public static boolean sendToMountain(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimensionType().hasSkyLight()) return false;
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(b -> b.is(BiomeTags.IS_MOUNTAIN),
                player.blockPosition(), SEARCH_RADIUS, 32, 64);
        if (found == null) return false;
        BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getFirst());
        Direction into = uphill(level, at);
        // Step back down the slope a little so the gate sits in the hillside, not on the peak.
        BlockPos mouth = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.relative(into.getOpposite(), 4));
        BlockPos inside = build(level, mouth, into);
        player.teleportTo(level, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, into.getOpposite().toYRot(), 0F);
        player.setRespawnPosition(level.dimension(), inside, into.getOpposite().toYRot(), true, false);
        return true;
    }

    /** The horizontal direction the ground rises most steeply. */
    static Direction uphill(ServerLevel level, BlockPos at) {
        Direction best = Direction.NORTH;
        int rise = Integer.MIN_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX() + d.getStepX() * TUNNEL,
                    at.getZ() + d.getStepZ() * TUNNEL);
            if (h > rise) {
                rise = h;
                best = d;
            }
        }
        return best;
    }

    /**
     * Builds the gate at {@code mouth} (the floor block in front of the gate is below it) with the tunnel running
     * {@code into}. Returns where to stand at the tunnel's inner end, beside the chest.
     */
    public static BlockPos build(ServerLevel level, BlockPos mouth, Direction into) {
        Direction side = into.getClockWise();
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState rune = WFRegistry.RUNE_STONE.get().defaultBlockState();
        // The tunnel: 3 wide, 3 tall, TUNNEL long; a stone-brick floor and rune stone walls and roof.
        for (int i = 0; i <= TUNNEL; i++) {
            BlockPos row = mouth.relative(into, i);
            for (int w = -2; w <= 2; w++) {
                for (int y = -1; y <= 3; y++) {
                    BlockPos p = row.relative(side, w).above(y);
                    boolean shell = Math.abs(w) == 2 || y == -1 || y == 3;
                    if (i == 0 && !shell) {
                        set(level, p, Blocks.AIR.defaultBlockState());
                    } else if (i == 0) {
                        set(level, p, y == 3 || Math.abs(w) == 2 ? chiseled : brick);   // the gate frame
                    } else if (shell) {
                        set(level, p, y == -1 ? brick : (i == TUNNEL ? brick : rune));
                    } else {
                        set(level, p, i == TUNNEL ? brick : Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        // A lintel over the gate and braziers either side of it.
        for (int w = -3; w <= 3; w++) set(level, mouth.relative(side, w).above(4), chiseled);
        for (int w : new int[]{-3, 3}) {
            BlockPos b = mouth.relative(side, w).relative(into.getOpposite());
            set(level, b.below(), brick);
            set(level, b, WFRegistry.MANA_BRAZIER.get().defaultBlockState());
        }
        // Torches along the walls and the starter chest at the end.
        for (int i = 3; i < TUNNEL; i += 4) {
            for (int w : new int[]{-1, 1}) {
                Direction facing = w < 0 ? side : side.getOpposite();
                set(level, mouth.relative(into, i).relative(side, w).above(1),
                        Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing));
            }
        }
        BlockPos chestPos = mouth.relative(into, TUNNEL - 1).relative(side, 1);
        set(level, chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, side.getOpposite()));
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) fillChest(chest);
        return mouth.relative(into, TUNNEL - 2);
    }

    private static void fillChest(ChestBlockEntity chest) {
        ItemStack[] goods = {new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.BREAD, 12), new ItemStack(Items.TORCH, 32),
                new ItemStack(WFRegistry.RUNE_STONE_ITEM.get(), 16), new ItemStack(WFRegistry.MANA_SHARD.get(), 8),
                new ItemStack(WFRegistry.MANA_WELL_ITEM.get()), new ItemStack(Items.IRON_INGOT, 6)};
        for (int i = 0; i < goods.length; i++) chest.setItem(i, goods[i]);
        chest.setChanged();
    }

    private static void set(ServerLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, 3);
    }

    @Nullable
    public static BlockPos testBuild(ServerPlayer player) {
        Direction into = player.getDirection();
        BlockPos mouth = player.blockPosition().relative(into, 3);
        return build(player.serverLevel(), mouth, into);
    }
}
