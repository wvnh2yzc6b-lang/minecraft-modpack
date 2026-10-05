package com.warfront.faction;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Hostile computer-controlled war factions that raid players and besiege their strongholds. */
public enum NpcFaction {
    MARAUDERS("Marauder Horde", ChatFormatting.RED, Race.ORC, 4),
    BLACK_LEGION("Black Legion", ChatFormatting.DARK_GRAY, Race.HUMAN, 5);

    public final String displayName;
    public final ChatFormatting color;
    public final Race race;
    public final int skin;

    NpcFaction(String displayName, ChatFormatting color, Race race, int skin) {
        this.displayName = displayName;
        this.color = color;
        this.race = race;
        this.skin = skin;
    }

    public String key() {
        return "npc:" + name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static NpcFaction byKey(String key) {
        for (NpcFaction f : values()) {
            if (f.key().equals(key)) return f;
        }
        return null;
    }
}
