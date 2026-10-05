package com.warfront.army;

import java.util.Locale;

/**
 * Battlefield roles. {@code rank} decides placement in formations: lower ranks stand in front.
 */
public enum SoldierRole {
    // dmg is the base attack before the weapon's own damage is added.
    //            rank  hp  dmg  armor speed  melee
    SHIELDBEARER(0,     30, 1.0, 6,    0.27,  true,  "Holds the line. Raises a shield against charges and arrows."),
    SPEARMAN(1,         22, 0.0, 3,    0.29,  true,  "Long reach. Strikes over the shield wall from the second rank."),
    SWORDSMAN(2,        26, 1.0, 4,    0.31,  true,  "Front-line fighter with high damage."),
    CAPTAIN(3,          34, 1.0, 6,    0.30,  true,  "Rallies nearby allies, boosting strength and morale."),
    ARCHER(4,           20, 2.0, 2,    0.30,  false, "Fires synchronized volleys from the back ranks."),
    HEALER(5,           20, 1.0, 2,    0.30,  false, "Tends to wounded allies behind the lines.");

    public final int rank;
    public final double health;
    public final double damage;
    public final double armor;
    public final double speed;
    public final boolean melee;
    public final String description;

    SoldierRole(int rank, double health, double damage, double armor, double speed, boolean melee, String description) {
        this.rank = rank;
        this.health = health;
        this.damage = damage;
        this.armor = armor;
        this.speed = speed;
        this.melee = melee;
        this.description = description;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        String n = id();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    public static SoldierRole byOrdinal(int i) {
        SoldierRole[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
