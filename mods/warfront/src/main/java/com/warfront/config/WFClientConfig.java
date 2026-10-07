package com.warfront.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side options (config/warfront-client.toml). */
public final class WFClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CHAT_ALERTS;
    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.EnumValue<HudCorner> HUD_CORNER;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;

    public enum HudCorner { BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        CHAT_ALERTS = b.comment("Also print toasts and siege banners as chat lines (the old behaviour).")
                .define("chatAlerts", false);
        b.push("hud");
        HUD_ENABLED = b.comment("Show the combat HUD (race power, army, siege).").define("enabled", true);
        HUD_CORNER = b.comment("Where the combat HUD sits.").defineEnum("corner", HudCorner.BOTTOM_LEFT);
        HUD_SCALE = b.comment("Combat HUD size.").defineInRange("scale", 1.0, 0.5, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private WFClientConfig() {}
}
