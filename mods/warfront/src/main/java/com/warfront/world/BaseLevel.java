package com.warfront.world;

import com.warfront.army.SoldierRole;
import com.warfront.block.SummoningAltarBlockEntity;
import com.warfront.block.TowerBlockEntity;
import com.warfront.block.WarStandardBlockEntity;
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
 * A base is everything on one mana network: the wells and pylons linked to a spot, plus the towers, summoning altars
 * and War Standards within their reach. Its level needs two things at once: enough buildings, and enough siege waves
 * won at a War Standard on the network. The lower of the two sets the level, so a base can't be built up without
 * fighting for it, or fought up without building. The level sets how many war beasts its commander may field and
 * whether Captains and Champions can be summoned.
 */
public final class BaseLevel {
    /** Buildings needed for levels 2, 3, 4 and 5. */
    private static final int[] BUILDINGS = {6, 12, 20, 30};
    /** Siege waves won needed for levels 2, 3, 4 and 5. */
    private static final int[] WAVES = {3, 8, 15, 25};
    public static final int MAX_LEVEL = BUILDINGS.length + 1;
    /** War beasts allowed per base level. */
    public static final int BEASTS_PER_LEVEL = 2;

    private BaseLevel() {}

    /** A base's buildings, the most siege waves won at one of its War Standards, and the level they give. */
    public record Status(int buildings, int waves, int testLevel) {
        public Status(int buildings, int waves) {
            this(buildings, waves, 0);
        }

        /** The level; a test-mode override (shown as TEST) wins over what was built and fought for. */
        public int level() {
            if (testLevel > 0) return Math.min(MAX_LEVEL, testLevel);
            return Math.min(tier(buildings, BUILDINGS), tier(waves, WAVES));
        }

        public boolean isTest() {
            return testLevel > 0;
        }

        /** What the base still lacks for {@code target} level, in words; empty if it is there. */
        public String missingFor(int target) {
            if (target <= 1 || target > MAX_LEVEL) return "";
        if (testLevel > 0) return "change the TEST level in the Test Panel";
            int b = Math.max(0, BUILDINGS[target - 2] - buildings);
            int w = Math.max(0, WAVES[target - 2] - waves);
            String build = b > 0 ? "build " + b + " more wells, pylons, towers or altars on this network" : "";
            String fight = w > 0 ? "win " + w + " more siege waves at a War Standard on this network" : "";
            if (!build.isEmpty() && !fight.isEmpty()) return build + " and " + fight;
            return build + fight;
        }
    }

    private static int tier(int value, int[] thresholds) {
        int lvl = 1;
        for (int t : thresholds) if (value >= t) lvl++;
        return lvl;
    }

    /** Surveys the base around {@code from} for {@code factionKey}. */
    public static Status of(Level level, BlockPos from, String factionKey) {
        List<ManaNodeBlockEntity> nodes = ManaNetwork.nodes(level, from, factionKey);
        int buildings = nodes.size();
        int testLevel = 0;
        for (ManaNodeBlockEntity node : nodes) testLevel = Math.max(testLevel, node.getTestLevel());
        int waves = 0;
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
                else if (be instanceof WarStandardBlockEntity standard) key = standard.factionKey(server);
                else continue;
                if (Factions.relation(server, factionKey, key) != Relation.ALLY || !inReach(nodes, be.getBlockPos(), reachSq)) continue;
                if (be instanceof WarStandardBlockEntity standard) waves = Math.max(waves, standard.getWavesWon());
                else buildings++;
            }
        }
        return new Status(buildings, waves, testLevel);
    }

    private static boolean inReach(List<ManaNodeBlockEntity> nodes, BlockPos pos, double reachSq) {
        for (ManaNodeBlockEntity node : nodes) {
            if (node.getBlockPos().distSqr(pos) <= reachSq) return true;
        }
        return false;
    }

    /** The base level a role needs before an altar will summon it: Captains 2, Champions 4, everyone else 1. */
    public static int requiredLevel(SoldierRole role) {
        return switch (role) {
            case CAPTAIN -> 2;
            case CHAMPION -> 4;
            default -> 1;
        };
    }

    /** How many war beasts a base of this level supports, never above the {@code beastLimit} setting. */
    public static int beastCap(int baseLevel) {
        return Math.min(WFConfig.BEAST_LIMIT.get(), baseLevel * BEASTS_PER_LEVEL);
    }
}
