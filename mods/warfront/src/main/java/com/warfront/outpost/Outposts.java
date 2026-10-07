package com.warfront.outpost;

import com.warfront.army.SoldierRole;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.config.WFConfig;
import com.warfront.faction.NpcFaction;
import com.warfront.registry.WFRegistry;
import com.warfront.world.WarbandSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Siege outposts. Every fifth wave (config) the attackers raise an outpost 30 to 50 blocks from the War Standard: a
 * watchtower camp early, a small walled fort from wave 10. The wave can't be won while it stands. It keeps sending
 * raiders, and at its heart sits the faction's raid chest; empty it and break it, and the outpost comes down.
 */
public final class Outposts {
    private Outposts() {}

    public static boolean shouldRaise(int wave) {
        int every = WFConfig.OUTPOST_INTERVAL.get();
        return every > 0 && wave > 0 && wave % every == 0;
    }

    /** The faction's building materials: wall, floor, accent (tent or banner), tower. */
    private record Palette(Block wall, Block floor, Block tent, Block tower) {}

    private static Palette palette(NpcFaction f) {
        return switch (f) {
            case MARAUDERS -> new Palette(Blocks.SPRUCE_LOG, Blocks.COARSE_DIRT, Blocks.BROWN_WOOL, Blocks.SPRUCE_PLANKS);
            case BLACK_LEGION -> new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE, Blocks.BLACK_WOOL, Blocks.POLISHED_DEEPSLATE);
            case BURNING_HORDE -> new Palette(Blocks.NETHER_BRICKS, Blocks.BLACKSTONE, Blocks.RED_WOOL, Blocks.RED_NETHER_BRICKS);
            case THE_SWARM -> new Palette(Blocks.SCULK, Blocks.SCULK, Blocks.LIME_WOOL, Blocks.DEEPSLATE_TILES);
            case SILVERWOOD_REAVERS -> new Palette(Blocks.BIRCH_LOG, Blocks.MOSS_BLOCK, Blocks.GREEN_WOOL, Blocks.BIRCH_PLANKS);
            case IRONBEARD_CLAN -> new Palette(Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.BLUE_WOOL, Blocks.CHISELED_STONE_BRICKS);
            case FALLEN_HOST -> new Palette(Blocks.QUARTZ_BRICKS, Blocks.SMOOTH_QUARTZ, Blocks.GRAY_WOOL, Blocks.QUARTZ_PILLAR);
        };
    }

    /** Raises an outpost for this wave. Returns the raid chest's position, or null if there was nowhere to put it. */
    @Nullable
    public static BlockPos raise(ServerLevel level, WarStandardBlockEntity standard, NpcFaction faction, int wave, UUID warband) {
        BlockPos home = standard.getBlockPos();
        RandomSource r = level.random;
        BlockPos center = null;
        for (int tries = 0; tries < 12 && center == null; tries++) {
            float angle = r.nextFloat() * Mth.TWO_PI;
            int dist = 30 + r.nextInt(21);
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(home.getX() + Mth.floor(Mth.cos(angle) * dist), 0, home.getZ() + Mth.floor(Mth.sin(angle) * dist)));
            if (!level.isLoaded(at)) continue;
            BlockState ground = level.getBlockState(at.below());
            if (ground.isSolid() && level.getFluidState(at.below()).isEmpty()) center = at;
        }
        if (center == null) return null;
        List<BlockPos> placed = new ArrayList<>();
        Palette pal = palette(faction);
        boolean fort = wave >= 10;
        int half = fort ? 8 : 5;
        // Floor and clearance.
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                put(level, placed, center.offset(dx, -1, dz), pal.floor().defaultBlockState(), true);
            }
        }
        // Walls: a palisade ring (camp) or stone walls (fort), with a gate facing the standard.
        int height = fort ? 3 : 2;
        int gateX = Integer.signum(home.getX() - center.getX()), gateZ = Integer.signum(home.getZ() - center.getZ());
        for (int i = -half; i <= half; i++) {
            for (int[] edge : new int[][]{{i, -half}, {i, half}, {-half, i}, {half, i}}) {
                int x = edge[0], z = edge[1];
                boolean gate = Math.abs(gateX) >= Math.abs(gateZ)
                        ? x == half * gateX && Math.abs(z) <= 1 : z == half * gateZ && Math.abs(x) <= 1;
                if (gate) continue;
                for (int y = 0; y < height; y++) put(level, placed, center.offset(x, y, z), pal.wall().defaultBlockState(), false);
            }
        }
        if (fort) {
            // Two corner towers.
            for (int[] c : new int[][]{{-half, -half}, {half, half}}) {
                for (int y = 0; y < 5; y++) {
                    for (int dx = 0; dx <= 1; dx++) {
                        for (int dz = 0; dz <= 1; dz++) {
                            put(level, placed, center.offset(c[0] + dx * Integer.signum(-c[0]), y, c[1] + dz * Integer.signum(-c[1])),
                                    pal.tower().defaultBlockState(), false);
                        }
                    }
                }
            }
        } else {
            // A wooden watchtower in one corner: four posts and a platform.
            int tx = -half + 1, tz = -half + 1;
            for (int y = 0; y < 4; y++) {
                for (int[] p : new int[][]{{0, 0}, {2, 0}, {0, 2}, {2, 2}}) {
                    put(level, placed, center.offset(tx + p[0], y, tz + p[1]), pal.wall().defaultBlockState(), false);
                }
            }
            for (int dx = 0; dx <= 2; dx++) {
                for (int dz = 0; dz <= 2; dz++) put(level, placed, center.offset(tx + dx, 4, tz + dz), pal.tower().defaultBlockState(), false);
            }
            // A tent and a campfire.
            for (int dz = 0; dz < 3; dz++) {
                put(level, placed, center.offset(2, 0, dz - 3), pal.tent().defaultBlockState(), false);
                put(level, placed, center.offset(4, 0, dz - 3), pal.tent().defaultBlockState(), false);
                put(level, placed, center.offset(3, 1, dz - 3), pal.tent().defaultBlockState(), false);
            }
            put(level, placed, center.offset(-2, 0, 2), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), false);
        }
        // The raid chest at the heart.
        BlockState chest = WFRegistry.RAID_CHEST.get().defaultBlockState().setValue(RaidChestBlock.FACTION, faction.ordinal());
        level.setBlock(center, chest, 3);
        placed.add(center.immutable());
        if (level.getBlockEntity(center) instanceof RaidChestBlockEntity be) {
            be.link(home, warband, placed);
            fill(be, faction, wave, r);
        }
        // A few guards hold the outpost.
        List<SoldierRole> guards = new ArrayList<>(List.of(SoldierRole.SHIELDBEARER, SoldierRole.ARCHER, SoldierRole.SWORDSMAN));
        if (fort) guards.add(SoldierRole.CAPTAIN);
        WarbandSpawner.spawnInto(level, warband, faction, guards, center.offset(2, 0, 2), Vec3.atBottomCenterOf(center), null,
                Math.min(4, 1 + wave / 3)).forEach(s -> s.setPersistenceRequired());
        return center;
    }

    private record Removal(ServerLevel level, BlockPos chest, long at) {}

    private static final List<Removal> REMOVALS = new ArrayList<>();

    /** Takes an outpost down after {@code delay} ticks, by breaking its chest. */
    public static void scheduleRemoval(ServerLevel level, BlockPos chest, int delay) {
        REMOVALS.add(new Removal(level, chest.immutable(), level.getGameTime() + delay));
    }

    public static void clearAll() {
        REMOVALS.clear();
    }

    /** Every tick: due removals. */
    public static void tick() {
        if (REMOVALS.isEmpty()) return;
        REMOVALS.removeIf(r -> {
            if (r.level().getGameTime() < r.at()) return false;
            if (r.level().getBlockEntity(r.chest()) instanceof RaidChestBlockEntity chest) {
                chest.clearContent();
                r.level().setBlock(r.chest(), Blocks.AIR.defaultBlockState(), 3);   // onRemove tears the outpost down
            }
            return true;
        });
    }

    private static void put(ServerLevel level, List<BlockPos> placed, BlockPos pos, BlockState state, boolean floor) {
        BlockState there = level.getBlockState(pos);
        // Only build into open space (or level the floor), so taking the outpost down leaves the land as it was.
        if (floor ? !there.isAir() && !there.canBeReplaced() : !there.canBeReplaced()) return;
        level.setBlock(pos, state, 2);
        placed.add(pos.immutable());
    }

    /** Loot scaled to the wave, with each faction's own goods. */
    public static void fill(RaidChestBlockEntity chest, NpcFaction f, int wave, RandomSource r) {
        int n = 0;
        chest.setItem(n++, new ItemStack(WFRegistry.WAR_MARK.get(), Math.min(64, 8 + wave * 2)));
        chest.setItem(n++, new ItemStack(WFRegistry.MANA_SHARD.get(), Math.min(64, 6 + wave * 2)));
        chest.setItem(n++, new ItemStack(WFRegistry.MANA_CRYSTAL.get(), 1 + wave / 5));
        ItemStack[] goods = switch (f) {
            case MARAUDERS -> new ItemStack[]{new ItemStack(Items.LEATHER, 8 + wave), new ItemStack(Items.BONE, 6), new ItemStack(Items.IRON_AXE)};
            case BLACK_LEGION -> new ItemStack[]{new ItemStack(Items.IRON_INGOT, 6 + wave / 2), new ItemStack(Items.SHIELD), new ItemStack(Items.IRON_SWORD)};
            case BURNING_HORDE -> new ItemStack[]{new ItemStack(Items.BLAZE_POWDER, 4 + wave / 3), new ItemStack(Items.MAGMA_CREAM, 4), new ItemStack(Items.GOLD_INGOT, 3)};
            case THE_SWARM -> new ItemStack[]{new ItemStack(Items.SLIME_BALL, 6), new ItemStack(Items.ECHO_SHARD, 1 + wave / 10), new ItemStack(Items.SCULK, 8)};
            case SILVERWOOD_REAVERS -> new ItemStack[]{new ItemStack(Items.ARROW, 32), new ItemStack(Items.BOW), new ItemStack(Items.GOLDEN_APPLE, 1 + wave / 10)};
            case IRONBEARD_CLAN -> new ItemStack[]{new ItemStack(Items.IRON_INGOT, 10), new ItemStack(Items.GOLD_INGOT, 6), new ItemStack(Items.DIAMOND, 1 + wave / 10)};
            case FALLEN_HOST -> new ItemStack[]{new ItemStack(Items.FEATHER, 16), new ItemStack(Items.GOLD_INGOT, 8), new ItemStack(Items.GLOWSTONE_DUST, 12)};
        };
        for (ItemStack g : goods) chest.setItem(n++, g);
        if (wave >= 15) chest.setItem(n, new ItemStack(Items.DIAMOND, 1 + r.nextInt(2)));
        chest.setChanged();
    }
}
