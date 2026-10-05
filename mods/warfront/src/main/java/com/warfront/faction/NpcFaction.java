package com.warfront.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Predicate;

/** Hostile computer-controlled war factions that raid players and besiege their strongholds. */
public enum NpcFaction {
    MARAUDERS("Marauder Horde", ChatFormatting.RED, Race.ORC, 1.0,
            b -> b.is(BiomeTags.IS_SAVANNA) || b.is(Tags.Biomes.IS_PLAINS)),
    BLACK_LEGION("Black Legion", ChatFormatting.DARK_GRAY, Race.HUMAN, 1.0,
            b -> b.is(Tags.Biomes.IS_PLAINS) || b.is(BiomeTags.IS_TAIGA)),
    BURNING_HORDE("Burning Horde", ChatFormatting.GOLD, Race.DEMON, 1.0,
            b -> b.is(BiomeTags.IS_BADLANDS) || b.is(Tags.Biomes.IS_DESERT) || b.is(BiomeTags.IS_NETHER)),
    THE_SWARM("The Swarm", ChatFormatting.LIGHT_PURPLE, Race.HIVE, 1.6,
            b -> b.is(BiomeTags.IS_JUNGLE) || b.is(Tags.Biomes.IS_SWAMP) || b.is(Tags.Biomes.IS_CAVE)),
    SILVERWOOD_REAVERS("Silverwood Reavers", ChatFormatting.GREEN, Race.ELF, 1.0,
            b -> b.is(BiomeTags.IS_FOREST)),
    IRONBEARD_CLAN("Ironbeard Clan", ChatFormatting.AQUA, Race.DWARF, 0.9,
            b -> b.is(BiomeTags.IS_MOUNTAIN) || b.is(Tags.Biomes.IS_SNOWY)),
    FALLEN_HOST("Fallen Host", ChatFormatting.YELLOW, Race.ANGEL, 0.8,
            b -> b.is(Tags.Biomes.IS_MOUNTAIN_PEAK) || b.is(BiomeTags.IS_END));

    public final String displayName;
    public final ChatFormatting color;
    public final Race race;
    /** Warband size multiplier; the Swarm comes in numbers. */
    public final double sizeMultiplier;
    private final Predicate<Holder<Biome>> homeland;

    NpcFaction(String displayName, ChatFormatting color, Race race, double sizeMultiplier,
               Predicate<Holder<Biome>> homeland) {
        this.displayName = displayName;
        this.color = color;
        this.race = race;
        this.sizeMultiplier = sizeMultiplier;
        this.homeland = homeland;
    }

    public String key() {
        return "npc:" + name().toLowerCase(Locale.ROOT);
    }

    /** Skin index used by the renderer; NPC skins follow the race skins. */
    public int skin() {
        return Race.values().length + ordinal();
    }

    @Nullable
    public static NpcFaction byKey(String key) {
        for (NpcFaction f : values()) {
            if (f.key().equals(key)) return f;
        }
        return null;
    }

    public static NpcFaction random(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }

    /** Picks an attacking faction, strongly favouring those whose homeland is this biome. */
    public static NpcFaction pick(Holder<Biome> biome, Level level, RandomSource random) {
        if (level.dimension() == Level.NETHER) return BURNING_HORDE;
        int[] weights = new int[values().length];
        int total = 0;
        for (NpcFaction f : values()) {
            int w = f.homeland.test(biome) ? 8 : 1;
            weights[f.ordinal()] = w;
            total += w;
        }
        int roll = random.nextInt(total);
        for (NpcFaction f : values()) {
            roll -= weights[f.ordinal()];
            if (roll < 0) return f;
        }
        return MARAUDERS;
    }
}
