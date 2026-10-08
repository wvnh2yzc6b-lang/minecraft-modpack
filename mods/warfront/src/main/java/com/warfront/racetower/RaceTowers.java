package com.warfront.racetower;

import com.warfront.config.WFConfig;
import com.warfront.faction.Factions;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.world.BaseLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Race tower rules: own race only, the base on that mana network must be at the tower's level, and a base holds at
 * most 2/4/6/10 race towers at levels 2-5 (config), capstones one of each. Losing base level keeps the towers you
 * have but blocks new ones over the cap.
 */
public final class RaceTowers {
    /** How far from a new tower other race towers count as the same base. */
    public static final int BASE_RADIUS = 48;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED = new HashMap<>();

    private RaceTowers() {}

    static void track(Level level, BlockPos pos, boolean add) {
        Set<BlockPos> set = LOADED.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        if (add) set.add(pos.immutable());
        else set.remove(pos);
    }

    public static void clearAll() {
        LOADED.clear();
    }

    /** Race towers of this faction loaded within the base radius of {@code pos}. */
    public static List<RaceTowerBlockEntity> near(Level level, BlockPos pos, String key, double radius) {
        List<RaceTowerBlockEntity> out = new ArrayList<>();
        MinecraftServer server = level.getServer();
        for (BlockPos p : LOADED.getOrDefault(level.dimension(), Set.of())) {
            if (p.distSqr(pos) > radius * radius) continue;
            if (level.getBlockEntity(p) instanceof RaceTowerBlockEntity t
                    && Factions.relation(server, key, t.factionKey(server)) == Relation.ALLY) out.add(t);
        }
        return out;
    }

    /** How many race towers a base of this level may hold. */
    public static int cap(int baseLevel) {
        List<? extends Integer> caps = WFConfig.RACE_TOWER_CAPS.get();
        int i = baseLevel - 2;
        if (i < 0) return 0;
        return caps.get(Math.min(i, caps.size() - 1));
    }

    /** Why this player can't place this tower here, or null if they can. */
    @Nullable
    public static String refusal(Level level, BlockPos pos, Player player, RaceTowerType type) {
        Race race = Race.byId(player.getData(com.warfront.registry.WFRegistry.RACE));
        if (race != type.race) return "Only " + type.race.displayName() + " players can raise a " + type.displayName + ".";
        if (level.isClientSide) return null;   // the server checks the base
        String key = Factions.keyOf(level.getServer(), player);
        int baseLevel = BaseLevel.of(level, pos, key).level();
        if (baseLevel < type.unlockLevel) {
            return "A " + type.displayName + " needs a level " + type.unlockLevel + " base on this mana network; this one is level "
                    + baseLevel + ".";
        }
        List<RaceTowerBlockEntity> placed = near(level, pos, key, BASE_RADIUS);
        if (type.capstone() && placed.stream().anyMatch(t -> t.type() == type)) {
            return "A base can hold only one " + type.displayName + ".";
        }
        int cap = cap(baseLevel);
        if (placed.size() >= cap) return "This base holds its most race towers (" + cap + " at level " + baseLevel + "). Raise the base level for more.";
        return null;
    }

    /** Towers within 8 blocks of an allied Watchtower Bell reach a quarter further. */
    public static double rangeBonus(Level level, BlockPos pos, String key) {
        MinecraftServer server = level.getServer();
        for (BlockPos p : LOADED.getOrDefault(level.dimension(), Set.of())) {
            if (p.equals(pos) || p.distSqr(pos) > 64) continue;
            if (level.getBlockEntity(p) instanceof RaceTowerBlockEntity t && t.type() == RaceTowerType.WATCHTOWER_BELL
                    && Factions.relation(server, key, t.factionKey(server)) == Relation.ALLY) return 1.25;
        }
        return 1.0;
    }

    /** A death: Soul Pyres within 12 blocks whose side the victim fought against take a charge. */
    public static void onDeath(LivingEntity victim) {
        Level level = victim.level();
        MinecraftServer server = level.getServer();
        if (server == null) return;
        for (BlockPos p : LOADED.getOrDefault(level.dimension(), Set.of())) {
            if (p.distSqr(victim.blockPosition()) > 12 * 12) continue;
            if (level.getBlockEntity(p) instanceof RaceTowerBlockEntity t && t.type() == RaceTowerType.SOUL_PYRE
                    && Factions.relation(server, t.factionKey(server), Factions.keyOf(server, victim)) == Relation.ENEMY) {
                t.charge();
            }
        }
    }
}
