package com.warfront.army;

import com.warfront.faction.Race;

/** Which body (model, size and stat profile) a unit uses. */
public enum UnitBody {
    /** Standard humanoid soldier. */
    HUMANOID(0.0, 1.0, 1.0, false),
    /** Imps, one demon species among several: small, gaunt, bat-winged and scorpion-tailed. */
    IMP(-0.33, 0.75, 1.12, true),
    /** Hive spearmen: lean, four-armed ant warriors with a double-bladed polearm. */
    LANCER(0.05, 1.0, 1.05, false),
    /** Goblins, the orcs' farmers and builders: small, long-eared humanoids on the player rig. */
    GOBLIN(-0.3, 0.85, 1.1, false),
    /** The Hive's war beast: a lobster centaur on eight legs, about player height and two and a half blocks long. */
    HIVE_BEAST(0.25, 1.0, 0.9, false);

    /** Added to the SCALE attribute (multiplied by base), on top of the race's size. */
    public final double scale;
    public final double healthMultiplier;
    public final double speedMultiplier;
    /** Winged bodies take no fall damage. */
    public final boolean winged;

    UnitBody(double scale, double healthMultiplier, double speedMultiplier, boolean winged) {
        this.scale = scale;
        this.healthMultiplier = healthMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.winged = winged;
    }

    /** Model and texture name for this body and role, e.g. "imp_impaler". */
    public String variant(SoldierRole role) {
        return switch (this) {
            case HUMANOID -> "humanoid";
            case IMP -> role == SoldierRole.ARCHER ? "imp_firecaster" : "imp_impaler";
            case LANCER -> "hive_lancer";
            case HIVE_BEAST -> "hive_beast";
            case GOBLIN -> "goblin";
        };
    }

    /** Bodies drawn with the vanilla player rig (plus gear), rather than a generated creature model. */
    public boolean usesPlayerRig() {
        return this == HUMANOID || this == GOBLIN;
    }

    public static UnitBody of(Race race, SoldierRole role) {
        // Demons field imps only as Impalers and Firecasters; every other demon is full-sized.
        if (race == Race.DEMON && (role == SoldierRole.SPEARMAN || role == SoldierRole.ARCHER)) return IMP;
        if (race == Race.HIVE && role == SoldierRole.SPEARMAN) return LANCER;
        if (race == Race.HIVE && role == SoldierRole.BEAST) return HIVE_BEAST;
        if (race == Race.ORC && role.worker()) return GOBLIN;
        return HUMANOID;
    }
}
