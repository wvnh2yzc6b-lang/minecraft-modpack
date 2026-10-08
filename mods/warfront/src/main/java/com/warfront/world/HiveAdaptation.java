package com.warfront.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Hive is a cave race. Below {@link #DEPTH}, or anywhere the sky can't reach, Hive soldiers and players gain
 * Strength and Speed; in direct sunlight they are weakened. New Hive players start in a cave.
 *
 * Uses only vanilla effects, so other mods that read or clear effects see them normally.
 */
public final class HiveAdaptation {
    /** Below this height a Hive creature always counts as underground. */
    public static final int DEPTH = 40;

    private HiveAdaptation() {}

    public static boolean underground(LivingEntity e) {
        BlockPos p = e.blockPosition();
        // The Otherside (Deeper and Darker) is the Hive's homeland: underground everywhere.
        return Homelands.isOtherside(e.level()) || p.getY() < DEPTH || e.level().getBrightness(LightLayer.SKY, p) == 0;
    }

    public static boolean inSunlight(LivingEntity e) {
        Level level = e.level();
        BlockPos eyes = BlockPos.containing(e.getX(), e.getEyeY(), e.getZ());
        return !Homelands.isOtherside(level) && level.dimensionType().hasSkyLight() && level.isDay() && !level.isRaining() && level.canSeeSky(eyes);
    }

    /** Refreshes the underground or sunlight effects for the next {@code duration} ticks. */
    public static void apply(LivingEntity e, int duration) {
        if (underground(e)) {
            add(e, MobEffects.DAMAGE_BOOST, duration);
            add(e, MobEffects.MOVEMENT_SPEED, duration);
        } else if (inSunlight(e)) {
            add(e, MobEffects.WEAKNESS, duration);
        }
    }

    private static void add(LivingEntity e, Holder<MobEffect> effect, int duration) {
        e.addEffect(new MobEffectInstance(effect, duration, 0, true, false, true));
    }

    /** Moves a new Hive player into a nearby cave and makes it their spawn point; digs a chamber if none is found. */
    public static void sendToCave(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimensionType().hasSkyLight()) return;   // already underground in spirit: the Nether, the End
        BlockPos home = findCave(level, player.blockPosition());
        if (home == null) home = digChamber(level, player.blockPosition());
        player.teleportTo(level, home.getX() + 0.5, home.getY(), home.getZ() + 0.5, player.getYRot(), 0F);
        player.setRespawnPosition(level.dimension(), home, player.getYRot(), true, false);
    }

    @Nullable
    private static BlockPos findCave(ServerLevel level, BlockPos origin) {
        int top = Math.min(DEPTH, origin.getY() - 8);
        int bottom = level.getMinBuildHeight() + 6;
        for (int r = 0; r <= 48; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;   // walk outward ring by ring
                    int x = origin.getX() + dx, z = origin.getZ() + dz;
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    for (int y = top; y >= bottom; y--) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (standable(level, p) && level.getBrightness(LightLayer.SKY, p) == 0) return p;
                    }
                }
            }
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos p) {
        BlockState floor = level.getBlockState(p.below());
        return floor.isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                && level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                && level.getFluidState(p).isEmpty() && level.getFluidState(p.below()).isEmpty();
    }

    /** Hollows out a small chamber deep under {@code origin}, lit by a few shroomlights. */
    private static BlockPos digChamber(ServerLevel level, BlockPos origin) {
        int y = Math.max(level.getMinBuildHeight() + 12, Math.min(DEPTH - 10, origin.getY() - 20));
        BlockPos center = new BlockPos(origin.getX(), y, origin.getZ());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(center.offset(dx, -1, dz), Blocks.DEEPSLATE.defaultBlockState());
                for (int dy = 0; dy <= 2; dy++) level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(center.offset(dx, 3, dz), Blocks.DEEPSLATE.defaultBlockState());
            }
        }
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            level.setBlockAndUpdate(center.offset(c[0], 3, c[1]), Blocks.SHROOMLIGHT.defaultBlockState());
        }
        return center;
    }
}
