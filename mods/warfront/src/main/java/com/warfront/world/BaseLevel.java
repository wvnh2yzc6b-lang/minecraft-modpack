package com.warfront.world;

import com.warfront.block.SummoningAltarBlockEntity;
import com.warfront.block.TowerBlockEntity;
import com.warfront.config.WFConfig;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import com.warfront.mana.ManaNetwork;
import com.warfront.mana.ManaNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A base is everything on one mana network: the wells and pylons linked to a spot, plus the towers and summoning
 * altars within their reach. Its level comes from how many of those buildings it has, and the level sets how many
 * war beasts its commander may field.
 */
public final class BaseLevel {
    /** Buildings needed for levels 2, 3, 4 and 5. */
    private static final int[] THRESHOLDS = {4, 8, 14, 20};
    public static final int MAX_LEVEL = THRESHOLDS.length + 1;
    /** War beasts allowed per base level. */
    public static final int BEASTS_PER_LEVEL = 2;

    private BaseLevel() {}

    /** How many allied wells, pylons, towers and altars make up the base around {@code from}. */
    public static int buildings(Level level, BlockPos from, String factionKey) {
        List<ManaNodeBlockEntity> nodes = ManaNetwork.nodes(level, from, factionKey);
        int count = nodes.size();
        double reach = WFConfig.MANA_LINK_RANGE.get();
        double reachSq = reach * reach;
        int chunkReach = ((int) reach >> 4) + 1;
        Set<ChunkPos> chunks = new HashSet<>();
        for (ManaNodeBlockEntity node : nodes) {
            ChunkPos c = new ChunkPos(node.getBlockPos());
            for (int dx = -chunkReach; dx <= chunkReach; dx++) {
                for (int dz = -chunkReach; dz <= chunkReach; dz++) chunks.add(new ChunkPos(c.x + dx, c.z + dz));
            }
        }
        MinecraftServer server = level.getServer();
        for (ChunkPos c : chunks) {
            if (!level.hasChunk(c.x, c.z)) continue;
            LevelChunk chunk = level.getChunk(c.x, c.z);
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                String key;
                if (be instanceof TowerBlockEntity tower) key = tower.factionKey(server);
                else if (be instanceof SummoningAltarBlockEntity altar && altar.hasOwner()) key = altar.factionKey(server);
                else continue;
                if (Factions.relation(server, factionKey, key) != Relation.ALLY) continue;
                BlockPos pos = be.getBlockPos();
                for (ManaNodeBlockEntity node : nodes) {
                    if (node.getBlockPos().distSqr(pos) <= reachSq) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    /** The level a base with this many buildings has, 1 to {@link #MAX_LEVEL}. */
    public static int forBuildings(int buildings) {
        int lvl = 1;
        for (int t : THRESHOLDS) if (buildings >= t) lvl++;
        return lvl;
    }

    /** Buildings still needed for the next level, or 0 at the top level. */
    public static int toNextLevel(int buildings) {
        for (int t : THRESHOLDS) if (buildings < t) return t - buildings;
        return 0;
    }

    /** How many war beasts a base of this level supports, never above the {@code beastLimit} setting. */
    public static int beastCap(int baseLevel) {
        return Math.min(WFConfig.BEAST_LIMIT.get(), baseLevel * BEASTS_PER_LEVEL);
    }
}
