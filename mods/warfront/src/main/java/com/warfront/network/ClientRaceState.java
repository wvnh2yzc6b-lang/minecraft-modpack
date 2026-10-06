package com.warfront.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Races of players known to this client. Plain data; safe to load on either side. */
public final class ClientRaceState {
    private static final Map<UUID, String> RACES = new ConcurrentHashMap<>();

    private ClientRaceState() {}

    public static void set(UUID player, String race) {
        if (race == null || race.isEmpty()) RACES.remove(player);
        else RACES.put(player, race);
    }

    public static String get(UUID player) {
        return RACES.getOrDefault(player, "");
    }
}
