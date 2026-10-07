package com.warfront.upkeep;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Blocks raiders broke, so builders repair raid damage before anything else. Kept until rebuilt (not saved). */
public final class RaidDamage {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> BROKEN = new HashMap<>();

    private RaidDamage() {}

    public static void log(Level level, BlockPos pos) {
        BROKEN.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    public static boolean contains(Level level, BlockPos pos) {
        Set<BlockPos> s = BROKEN.get(level.dimension());
        return s != null && s.contains(pos);
    }

    public static void repaired(Level level, BlockPos pos) {
        Set<BlockPos> s = BROKEN.get(level.dimension());
        if (s != null) s.remove(pos);
    }

    public static void clearAll() {
        BROKEN.clear();
    }
}
