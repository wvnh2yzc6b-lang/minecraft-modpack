package com.warfront.army;

import com.warfront.faction.Race;

/** Which body (model, size and stat profile) a unit uses. */
public enum UnitBody {
    /** Standard humanoid soldier. */
    HUMANOID(0.0, 1.0, 1.0, false),
    /** Demon low ranks: small, gaunt, bat-winged and scorpion-tailed. */
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
        return switch (role) {
            case SHIELDBEARER -> "imp_bulwark";
            case SPEARMAN -> "imp_impaler";
            case ARCHER -> "imp_firecaster";
            case FARMER -> "imp_farmer";
            case BUILDER -> "imp_builder";
            case GUARD -> "imp_guard";
            default -> "imp";
        };
    }

    public static UnitBody of(Race race, SoldierRole role) {
        if (race == Race.DEMON) {
            return switch (role) {
                case SHIELDBEARER, SPEARMAN, SWORDSMAN, ARCHER, FARMER, BUILDER, GUARD -> IMP;
                default -> HUMANOID;
            };
        }
        return HUMANOID;
    }
}
