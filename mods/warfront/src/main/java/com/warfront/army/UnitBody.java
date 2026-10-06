package com.warfront.army;

import com.warfront.faction.Race;

/** Which body (model, size and stat profile) a unit uses. */
public enum UnitBody {
    /** Standard humanoid soldier. */
    HUMANOID(0.0, 1.0, 1.0, false),
    /** Imps, one demon species among several: small, gaunt, bat-winged and scorpion-tailed. */
    IMP(-0.33, 0.75, 1.12, true);

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
        if (this != IMP) return "humanoid";
        return role == SoldierRole.ARCHER ? "imp_firecaster" : "imp_impaler";
    }

    public static UnitBody of(Race race, SoldierRole role) {
        // Demons field imps only as Impalers and Firecasters; every other demon is full-sized.
        return race == Race.DEMON && (role == SoldierRole.SPEARMAN || role == SoldierRole.ARCHER) ? IMP : HUMANOID;
    }
}
