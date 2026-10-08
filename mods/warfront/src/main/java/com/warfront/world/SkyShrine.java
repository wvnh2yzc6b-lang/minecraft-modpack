package com.warfront.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The angels' start: a small marble shrine on the highest point near spawn (white pillars, gold trim, a column of
 * light). With no real peak near, it stands on a short pillar at spawn instead.
 */
public final class SkyShrine {
    public static final int SEARCH = 112;
    public static final int STEP = 16;
    /** A spot must rise this far above the player's ground to count as a peak. */
    public static final int MIN_RISE = 12;

    private SkyShrine() {}

    /** Moves a new angel player to a shrine on a nearby peak (or on a pillar at spawn) and sets their respawn there. */
    public static void sendToPeak(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimensionType().hasSkyLight()) return;
        BlockPos origin = player.blockPosition();
        var gen = level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        BlockPos best = null;
        int bestY = ground + MIN_RISE - 1;
        for (int dx = -SEARCH; dx <= SEARCH; dx += STEP) {
            for (int dz = -SEARCH; dz <= SEARCH; dz += STEP) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                int y = gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                if (y > bestY) {
                    bestY = y;
                    best = new BlockPos(x, y, z);
                }
            }
        }
        BlockPos floor;
        if (best != null) {
            floor = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, best);
        } else {
            floor = new BlockPos(origin.getX(), ground + 6, origin.getZ());
            for (int y = ground; y < floor.getY(); y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) level.setBlock(new BlockPos(floor.getX() + dx, y, floor.getZ() + dz),
                            Blocks.QUARTZ_PILLAR.defaultBlockState(), 3);
                }
            }
        }
        BlockPos stand = build(level, floor);
        player.teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 2.5, player.getYRot(), 0F);
        player.setRespawnPosition(level.dimension(), stand.south(2), player.getYRot(), true, false);
    }

    /** Builds the shrine with its floor at {@code at}'s level; returns the center of the floor (where to stand). */
    public static BlockPos build(ServerLevel level, BlockPos at) {
        BlockState floor = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                level.setBlock(at.offset(dx, -1, dz), rim ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : floor, 3);
                for (int y = 0; y <= 5; y++) level.setBlock(at.offset(dx, y, dz), air, 3);
            }
        }
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            for (int y = 0; y < 4; y++) level.setBlock(at.offset(c[0], y, c[1]), pillar, 3);
            level.setBlock(at.offset(c[0], 4, c[1]), gold, 3);
        }
        // Gold trim joining the pillar tops, and the light: a sea lantern under the floor's heart and end rods rising.
        for (int i = -2; i <= 2; i++) {
            level.setBlock(at.offset(i, 4, -3), Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 3);
            level.setBlock(at.offset(i, 4, 3), Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 3);
            level.setBlock(at.offset(-3, 4, i), Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 3);
            level.setBlock(at.offset(3, 4, i), Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 3);
        }
        level.setBlock(at.below(), Blocks.SEA_LANTERN.defaultBlockState(), 3);
        level.setBlock(at, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        for (int y = 1; y <= 3; y++) level.setBlock(at.above(y), Blocks.END_ROD.defaultBlockState(), 3);
        return at;
    }
}
