package com.warfront.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WFConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_ARMY_SIZE;
    public static final ModConfigSpec.IntValue BEAST_LIMIT;
    public static final ModConfigSpec.BooleanValue FRIENDLY_FIRE;
    public static final ModConfigSpec.BooleanValue STARTER_KIT;

    public static final ModConfigSpec.BooleanValue WARBANDS_ENABLED;
    public static final ModConfigSpec.IntValue WARBAND_INTERVAL;
    public static final ModConfigSpec.DoubleValue WARBAND_CHANCE;

    public static final ModConfigSpec.BooleanValue NATURAL_SIEGES;
    public static final ModConfigSpec.IntValue SIEGE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SIEGE_CHANCE;
    public static final ModConfigSpec.IntValue STANDARD_HEALTH;

    public static final ModConfigSpec.IntValue TOWER_RANGE;
    public static final ModConfigSpec.BooleanValue TOWERS_NEED_MANA;

    public static final ModConfigSpec.IntValue MANA_LINK_RANGE;
    public static final ModConfigSpec.IntValue WELL_CAPACITY;

    public static final ModConfigSpec.BooleanValue BUILDERS_NEED_MATERIALS;
    public static final ModConfigSpec.IntValue GUARD_ALARM_RADIUS;

    public static final ModConfigSpec.BooleanValue RAIDERS_BREAK_BLOCKS;
    public static final ModConfigSpec.DoubleValue MAX_BREAK_HARDNESS;
    public static final ModConfigSpec.IntValue WAVE_INTERMISSION;

    public static final ModConfigSpec.BooleanValue ADVISOR_QUEST;

    /** Per difficulty preset (Easy, Normal, Hard, Warlord): raid size, enemy health and damage, raids per day. */
    public static final ModConfigSpec.DoubleValue[] PRESET_RAID_SIZE = new ModConfigSpec.DoubleValue[4];
    public static final ModConfigSpec.DoubleValue[] PRESET_STRENGTH = new ModConfigSpec.DoubleValue[4];
    public static final ModConfigSpec.DoubleValue[] PRESET_RAIDS_PER_DAY = new ModConfigSpec.DoubleValue[4];
    public static final ModConfigSpec.IntValue GRACE_DAYS;
    public static final ModConfigSpec.DoubleValue AWAY_RAID_RATE;
    public static final ModConfigSpec.IntValue RAID_WARNING;
    public static final ModConfigSpec.IntValue SIEGE_MIN_DAYS;
    public static final ModConfigSpec.IntValue SIEGE_MAX_DAYS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("army");
        MAX_ARMY_SIZE = b.comment("Maximum number of soldiers a single commander may lead.")
                .defineInRange("maxArmySize", 40, 1, 500);
        BEAST_LIMIT = b.comment("The most war beasts one commander may field. A base allows 2 per base level (levels 1 to 5, earned by buildings and siege waves won), up to this.")
                .defineInRange("beastLimit", 10, 0, 500);
        FRIENDLY_FIRE = b.comment("Whether allied soldiers, players and towers can hurt each other.")
                .define("friendlyFire", false);
        STARTER_KIT = b.comment("Give new players a Commander's Baton and a few recruit contracts.")
                .define("starterKit", true);
        b.pop();

        b.push("warbands");
        WARBANDS_ENABLED = b.comment("Roaming marauder warbands that march on players in formation.")
                .define("enabled", true);
        WARBAND_INTERVAL = b.comment("Ticks between warband spawn attempts per player.")
                .defineInRange("intervalTicks", 6000, 200, 1_000_000);
        WARBAND_CHANCE = b.comment("Chance for each attempt to spawn a warband.")
                .defineInRange("chance", 0.12, 0.0, 1.0);
        b.pop();

        b.push("sieges");
        NATURAL_SIEGES = b.comment("War Standards are attacked by sieges at night while their owner is nearby.")
                .define("naturalSieges", true);
        SIEGE_COOLDOWN = b.comment("Minimum ticks between sieges on the same War Standard.")
                .defineInRange("cooldownTicks", 24000, 1200, 10_000_000);
        SIEGE_CHANCE = b.comment("Chance per check (every 30s at night) that a siege begins once the cooldown passed.")
                .defineInRange("chance", 0.2, 0.0, 1.0);
        STANDARD_HEALTH = b.comment("How many hits a War Standard can take from attackers before it falls.")
                .defineInRange("standardHealth", 30, 1, 1000);

        RAIDERS_BREAK_BLOCKS = b.comment("Raiders hack through blocks that stand between them and their objective.",
                        "Also requires the mobGriefing game rule.")
                .define("raidersBreakBlocks", true);
        MAX_BREAK_HARDNESS = b.comment("Hardest block raiders can break. Stone is 1.5, iron block 5, obsidian 50.")
                .defineInRange("maxBreakHardness", 10.0, 0.0, 100.0);
        WAVE_INTERMISSION = b.comment("Ticks between waves in a War Horn wave campaign.")
                .defineInRange("waveIntermissionTicks", 600, 100, 24000);
        b.pop();

        b.push("towers");
        TOWER_RANGE = b.comment("Detection range of defensive towers in blocks.")
                .defineInRange("range", 20, 4, 64);
        TOWERS_NEED_MANA = b.comment("Towers spend mana from a nearby Mana Well each time they fire or heal, and sit idle without it.")
                .define("needMana", true);
        b.pop();

        b.push("mana");
        MANA_LINK_RANGE = b.comment("How far, in blocks, a Mana Well or Pylon reaches: to towers, altars and other wells and pylons.")
                .defineInRange("linkRange", 16, 2, 64);
        WELL_CAPACITY = b.comment("How much mana one Mana Well can store.")
                .defineInRange("wellCapacity", 2000, 10, 1_000_000);
        b.pop();

        b.push("pacing");
        GRACE_DAYS = b.comment("No raids or sieges in a world's first days, and not before the advisor's quest reaches",
                        "'plant a War Standard' (whichever is later).")
                .defineInRange("graceDays", 3, 0, 100);
        AWAY_RAID_RATE = b.comment("How fast a player's raid clock runs while they're in another dimension than their base.")
                .defineInRange("awayDimensionRaidRate", 0.5, 0.0, 1.0);
        RAID_WARNING = b.comment("Ticks of warning before a raid or siege hits (2400 = 2 minutes).")
                .defineInRange("raidWarningTicks", 2400, 200, 24000);
        SIEGE_MIN_DAYS = b.comment("A siege comes every this many to siegeMaxDays nights (on Normal).")
                .defineInRange("siegeMinDays", 3, 1, 100);
        SIEGE_MAX_DAYS = b.defineInRange("siegeMaxDays", 4, 1, 100);
        String[] presets = {"easy", "normal", "hard", "warlord"};
        double[][] defaults = {{0.7, 0.8, 0.67}, {1.0, 1.0, 1.0}, {1.4, 1.25, 1.3}, {1.8, 1.5, 1.5}};
        for (int i = 0; i < 4; i++) {
            b.push(presets[i]);
            PRESET_RAID_SIZE[i] = b.comment("Raid and siege size multiplier.").defineInRange("raidSize", defaults[i][0], 0.1, 10.0);
            PRESET_STRENGTH[i] = b.comment("Enemy health and damage multiplier.").defineInRange("enemyStrength", defaults[i][1], 0.1, 10.0);
            PRESET_RAIDS_PER_DAY[i] = b.comment("Raids per in-game day, on average.").defineInRange("raidsPerDay", defaults[i][2], 0.0, 10.0);
            b.pop();
        }
        b.pop();

        b.push("advisor");
        ADVISOR_QUEST = b.comment("Each player gets an advisor who walks them through their first hour, step by step.",
                        "Turn off to skip the quest line (he still gives counsel).")
                .define("quest", true);
        b.pop();

        b.push("workers");
        BUILDERS_NEED_MATERIALS = b.comment("Builders must carry, or fetch from a nearby chest, every block they put back.",
                        "Turn off to let them rebuild from nothing.")
                .define("buildersNeedMaterials", true);
        GUARD_ALARM_RADIUS = b.comment("How far a guard's alarm reaches, in blocks. Allied troops within it join the fight.")
                .defineInRange("guardAlarmRadius", 16, 0, 64);
        b.pop();

        SPEC = b.build();
    }

    private WFConfig() {}
}
