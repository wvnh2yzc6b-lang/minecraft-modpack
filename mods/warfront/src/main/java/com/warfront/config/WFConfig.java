package com.warfront.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WFConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_ARMY_SIZE;
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

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("army");
        MAX_ARMY_SIZE = b.comment("Maximum number of soldiers a single commander may lead.")
                .defineInRange("maxArmySize", 40, 1, 500);
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
        b.pop();

        b.push("towers");
        TOWER_RANGE = b.comment("Detection range of defensive towers in blocks.")
                .defineInRange("range", 20, 4, 64);
        b.pop();

        SPEC = b.build();
    }

    private WFConfig() {}
}
