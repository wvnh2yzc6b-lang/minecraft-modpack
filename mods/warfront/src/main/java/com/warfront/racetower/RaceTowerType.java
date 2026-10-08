package com.warfront.racetower;

import com.mojang.serialization.Codec;
import com.warfront.faction.Race;
import net.minecraft.util.StringRepresentable;

/**
 * Race towers: three per race, unlocked at base level 2, 3 and 5, only for that race. The level 5 towers are
 * capstones, one per base each. Interval in ticks, mana per shot or pulse, reach in blocks.
 */
public enum RaceTowerType implements StringRepresentable {
    BALLISTA("ballista", "Ballista", Race.HUMAN, 2, 50, 4, 28,
            "Long-range bolt that pierces a line of enemies; extra damage to beasts and large foes."),
    THORNWOOD_SENTINEL("thornwood_sentinel", "Thornwood Sentinel", Race.ELF, 2, 60, 3, 12,
            "Roots the nearest enemies in vines and marks them Glowing for your archers."),
    RUNE_CANNON("rune_cannon", "Rune Cannon", Race.DWARF, 2, 70, 5, 20,
            "Slow, heavy shot with splash damage on impact; never breaks blocks."),
    WAR_DRUM_TOTEM("war_drum_totem", "War Drum Totem", Race.ORC, 2, 40, 2, 12,
            "Orcs near it build Rage faster; each beat may send nearby enemies running."),
    SOUL_PYRE("soul_pyre", "Soul Pyre", Race.DEMON, 2, 30, 2, 12,
            "Sets enemies alight; kills near it charge it, and at 10 charges it bursts in fire."),
    SUN_LANCE("sun_lance", "Sun Lance", Race.ANGEL, 2, 40, 3, 20,
            "Holy beam, strong by day under open sky, weak at night; extra damage to demons, Hive and undead."),
    LURKER_PIT("lurker_pit", "Lurker Pit", Race.HIVE, 2, 40, 2, 4,
            "Hidden in the ground until enemies come close, then sprays acid that eats their armor; stronger underground.");

    public static final Codec<RaceTowerType> CODEC = StringRepresentable.fromEnum(RaceTowerType::values);

    public final String id;
    public final String displayName;
    public final Race race;
    public final int unlockLevel;
    public final int interval;
    public final float manaCost;
    public final int reach;
    public final String description;

    RaceTowerType(String id, String displayName, Race race, int unlockLevel, int interval, float manaCost, int reach,
                  String description) {
        this.id = id;
        this.displayName = displayName;
        this.race = race;
        this.unlockLevel = unlockLevel;
        this.interval = interval;
        this.manaCost = manaCost;
        this.reach = reach;
        this.description = description;
    }

    /** Capstones (base level 5 towers) are limited to one of each per base. */
    public boolean capstone() {
        return unlockLevel >= 5;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
