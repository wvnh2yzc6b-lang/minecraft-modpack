package com.warfront.skill;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived skill states on the server: an active skill running (Blade Storm, Shield Wall...) or an armed "next
 * hit" (Precise Strike, Piercing Shot...). Keyed by player and node id, with the game time it ends.
 */
public final class SkillEffects {
    private static final Map<UUID, Map<String, Long>> UNTIL = new ConcurrentHashMap<>();

    private SkillEffects() {}

    private static long now(Player p) {
        return p.level().getGameTime();
    }

    public static void arm(Player p, String key, int ticks) {
        UNTIL.computeIfAbsent(p.getUUID(), u -> new HashMap<>()).put(key, now(p) + ticks);
    }

    public static boolean active(Player p, String key) {
        Map<String, Long> m = UNTIL.get(p.getUUID());
        Long until = m == null ? null : m.get(key);
        return until != null && until > now(p);
    }

    /** Ticks left on an effect (0 when not running). */
    public static long left(Player p, String key) {
        Map<String, Long> m = UNTIL.get(p.getUUID());
        Long until = m == null ? null : m.get(key);
        return until == null ? 0 : Math.max(0, until - now(p));
    }

    /** Uses up an armed effect: true if it was armed. */
    public static boolean consume(Player p, String key) {
        boolean was = active(p, key);
        Map<String, Long> m = UNTIL.get(p.getUUID());
        if (m != null) m.remove(key);
        return was;
    }

    public static void clear(Player p) {
        UNTIL.remove(p.getUUID());
    }

    public static void clearAll() {
        UNTIL.clear();
    }
}
