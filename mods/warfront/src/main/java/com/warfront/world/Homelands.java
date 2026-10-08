package com.warfront.world;

import com.warfront.faction.NpcFaction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Optional crossovers with The Aether and Deeper and Darker, by dimension id only (no dependency): the Aether is the
 * angels' homeland and the Fallen Host's, the Otherside the Hive's and the Swarm's. Without those mods nothing here
 * ever matches.
 */
public final class Homelands {
    public static final ResourceLocation AETHER = ResourceLocation.fromNamespaceAndPath("aether", "the_aether");
    public static final ResourceLocation OTHERSIDE = ResourceLocation.fromNamespaceAndPath("deeperdarker", "otherside");

    private Homelands() {}

    public static boolean isAether(ResourceLocation dimension) {
        return AETHER.equals(dimension);
    }

    public static boolean isOtherside(ResourceLocation dimension) {
        return OTHERSIDE.equals(dimension);
    }

    public static boolean isAether(Level level) {
        return isAether(level.dimension().location());
    }

    public static boolean isOtherside(Level level) {
        return isOtherside(level.dimension().location());
    }

    /** The faction whose homeland a dimension is, or null for an ordinary dimension. */
    @Nullable
    public static NpcFaction homeFaction(ResourceLocation dimension) {
        if (isAether(dimension)) return NpcFaction.FALLEN_HOST;
        if (isOtherside(dimension)) return NpcFaction.THE_SWARM;
        return null;
    }
}
