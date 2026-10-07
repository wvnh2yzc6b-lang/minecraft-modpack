package com.warfront.mana;

import com.warfront.block.ManaWellBlockEntity;
import com.warfront.config.WFConfig;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mana as base power. Wells store mana; wells and pylons each reach {@code linkRange} blocks, and anything
 * within reach of a node, or of a chain of nodes, can draw from every well on that chain. Towers and
 * summoning altars are the consumers. Only nodes allied to the consumer's faction take part.
 */
public final class ManaNetwork {
    /** Loaded wells and pylons, per dimension. Kept up to date by {@link ManaNodeBlockEntity}. */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> NODES = new HashMap<>();
    /** Stops a runaway search on a huge network. */
    private static final int MAX_NODES = 512;

    private ManaNetwork() {}

    static void add(Level level, BlockPos pos) {
        NODES.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    static void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = NODES.get(level.dimension());
        if (set != null) set.remove(pos);
    }

    public static void clear() {
        NODES.clear();
    }

    /** Every well a consumer at {@code from} can draw on for {@code factionKey}, nearest first. */
    public static List<ManaWellBlockEntity> wells(Level level, BlockPos from, String factionKey) {
        List<ManaWellBlockEntity> wells = new ArrayList<>();
        for (ManaNodeBlockEntity node : nodes(level, from, factionKey)) {
            if (node instanceof ManaWellBlockEntity well) wells.add(well);
        }
        wells.sort(Comparator.comparingDouble(w -> w.getBlockPos().distSqr(from)));
        return wells;
    }

    /** Every allied well and pylon reachable from {@code from}, directly or through a chain of nodes. */
    public static List<ManaNodeBlockEntity> nodes(Level level, BlockPos from, String factionKey) {
        Set<BlockPos> all = NODES.get(level.dimension());
        List<ManaNodeBlockEntity> found = new ArrayList<>();
        if (all == null || all.isEmpty()) return found;
        List<BlockPos> nodes = new ArrayList<>(all);
        double reach = WFConfig.MANA_LINK_RANGE.get();
        double reachSq = reach * reach;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(from);
        while (!queue.isEmpty() && seen.size() < MAX_NODES) {
            BlockPos cur = queue.poll();
            for (BlockPos n : nodes) {
                if (seen.contains(n) || n.distSqr(cur) > reachSq) continue;
                if (!(level.getBlockEntity(n) instanceof ManaNodeBlockEntity node)) continue;
                String key = node.factionKey(level.getServer());
                if (Factions.relation(level.getServer(), factionKey, key) != Relation.ALLY) continue;
                seen.add(n);
                found.add(node);
                queue.add(n);
            }
        }
        return found;
    }

    /** Total mana a consumer at {@code from} can draw. */
    public static float available(Level level, BlockPos from, String factionKey) {
        float sum = 0F;
        for (ManaWellBlockEntity w : wells(level, from, factionKey)) sum += w.getMana();
        return sum;
    }

    /** Takes {@code amount} mana from the wells in reach, nearest first. Takes nothing if there isn't enough. */
    public static boolean draw(Level level, BlockPos from, String factionKey, float amount) {
        if (amount <= 0F) return true;
        List<ManaWellBlockEntity> wells = wells(level, from, factionKey);
        float sum = 0F;
        for (ManaWellBlockEntity w : wells) sum += w.getMana();
        if (sum < amount) return false;
        float left = amount;
        for (ManaWellBlockEntity w : wells) {
            left -= w.take(left);
            if (left <= 0F) break;
        }
        return true;
    }
}
