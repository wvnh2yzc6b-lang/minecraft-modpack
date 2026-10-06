package com.warfront.army;

import java.util.Locale;

/**
 * Battlefield roles. {@code rank} decides placement in formations: lower ranks stand in front.
 */
public enum SoldierRole {
    // dmg is the base attack before the weapon's own damage is added. gear is the armor their
    // (painted-on) equipment provides at tier 1; it grows with tier.
    //            rank  hp  dmg  armor gear speed  melee  mana
    SHIELDBEARER(0,     30, 1.0, 6,    11,  0.27,  true,  15,  "Holds the line. Raises a shield against charges and arrows."),
    SPEARMAN(1,         22, 0.0, 3,    7,   0.29,  true,  15,  "Long reach. Strikes over the shield wall from the second rank."),
    SWORDSMAN(2,        26, 1.0, 4,    7,   0.31,  true,  20,  "Front-line fighter with high damage."),
    CAPTAIN(3,          34, 1.0, 6,    15,  0.30,  true,  50,  "Rallies nearby allies, boosting strength and morale."),
    CHAMPION(3,         40, 2.0, 8,    18,  0.30,  true,  60,  "The race's elite warrior, with a unique battle ability."),
    ARCHER(4,           20, 2.0, 2,    4,   0.30,  false, 20,  "Fires synchronized volleys from the back ranks."),
    HEALER(5,           20, 1.0, 2,    2,   0.30,  false, 30,  "Tends to wounded allies behind the lines.");

    public final int rank;
    public final double health;
    public final double damage;
    public final double armor;
    /** Armor granted by the unit's equipment, which is drawn into its skin rather than worn as items. */
    public final double gearArmor;
    public final double speed;
    public final boolean melee;
    /** Base mana cost to summon (before the race multiplier). */
    public final int manaCost;
    public final String description;

    SoldierRole(int rank, double health, double damage, double armor, double gearArmor, double speed, boolean melee,
                int manaCost, String description) {
        this.rank = rank;
        this.health = health;
        this.damage = damage;
        this.armor = armor;
        this.gearArmor = gearArmor;
        this.speed = speed;
        this.melee = melee;
        this.manaCost = manaCost;
        this.description = description;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        String n = id();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** Mana cost to summon this role as the given race. */
    public int manaCost(com.warfront.faction.Race race) {
        return Math.max(1, (int) Math.round(manaCost * race.manaCost));
    }

    public static SoldierRole byOrdinal(int i) {
        SoldierRole[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
