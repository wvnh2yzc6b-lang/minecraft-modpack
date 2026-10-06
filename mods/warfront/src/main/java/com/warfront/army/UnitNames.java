package com.warfront.army;

import com.warfront.faction.Race;

/** Every race fields its own named version of each role. */
public final class UnitNames {
    private UnitNames() {}

    //                                   SHIELDBEARER      SPEARMAN         SWORDSMAN        CAPTAIN           CHAMPION        ARCHER              HEALER
    private static final String[] HUMAN = {"Footman",        "Pikeman",       "Man-at-Arms",   "Knight-Captain", "Paladin",      "Longbowman",       "Cleric"};
    private static final String[] ELF   = {"Thornguard",     "Glaive Warden", "Bladesinger",   "Sylvan Lord",    "Bladedancer",  "Ranger",           "Druid"};
    private static final String[] DWARF = {"Ironshield",     "Halberdier",    "Axe Thane",     "Hold Captain",   "Ironbreaker",  "Sharpshooter",     "Runepriest"};
    private static final String[] ORC   = {"Bulwark",        "Gutspear",      "Brute",         "Warboss",        "Berserker",    "Hunter",           "Shaman"};
    private static final String[] DEMON = {"Hellshield",     "Impaler",       "Fiend",         "Archfiend",      "Hellknight",   "Hellfire Archer",  "Blood Witch"};
    private static final String[] ANGEL = {"Aegis",          "Lancer",        "Valkyrie",      "Archangel",      "Seraph",       "Starbow",          "Lightbearer"};
    private static final String[] HIVE  = {"Carapace",       "Lancer-Drone",  "Ripper",        "Hive Tyrant",    "Ravager",      "Spitter",          "Synapse Mender"};

    public static String of(Race race, SoldierRole role) {
        String[] names = switch (race) {
            case HUMAN -> HUMAN;
            case ELF -> ELF;
            case DWARF -> DWARF;
            case ORC -> ORC;
            case DEMON -> DEMON;
            case ANGEL -> ANGEL;
            case HIVE -> HIVE;
        };
        return names[role.ordinal()];
    }

    /** What a race's Champion does. */
    public static String championAbility(Race race) {
        return switch (race) {
            case HUMAN -> "Paladin: grants Resistance to allies within 6 blocks.";
            case ELF -> "Bladedancer: dual blades; dodges 30% of attacks.";
            case DWARF -> "Ironbreaker: wields a mace, heavily armored, cannot be knocked back.";
            case ORC -> "Berserker: twin axes; deals +50% damage below half health.";
            case DEMON -> "Hellknight: netherite blade; sets enemies within 3 blocks ablaze.";
            case ANGEL -> "Seraph: smites the nearest enemy within 12 blocks every 6s (double vs undead and demons).";
            case HIVE -> "Ravager: huge; claws inflict poison and slowness.";
        };
    }
}
