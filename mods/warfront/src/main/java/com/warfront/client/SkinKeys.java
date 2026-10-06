package com.warfront.client;

import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;

import java.util.Locale;

/** Texture folder name for a soldier's synced skin index: the race, or the NPC faction. */
public final class SkinKeys {
    private SkinKeys() {}

    public static String of(int skin) {
        Race[] races = Race.values();
        if (skin < races.length) return races[skin].id();
        return NpcFaction.values()[skin - races.length].name().toLowerCase(Locale.ROOT);
    }
}
