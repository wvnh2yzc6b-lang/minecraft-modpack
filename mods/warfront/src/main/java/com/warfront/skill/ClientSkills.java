package com.warfront.skill;

import com.warfront.network.SkillStatePayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The local player's skill state and the tree's paths, as the server last sent them. Plain data; safe on either side. */
public final class ClientSkills {
    private static volatile Set<String> unlocked = Set.of();
    private static volatile List<String> slots = List.of("", "", "", "");
    private static volatile List<SkillPath> paths = List.of();
    public static volatile int renown, level, into, toNext, points, respecCost;
    /** Per slot: ticks to the next charge when received, charges ready, charges in all. */
    public static volatile int[] slotTicks = new int[4], slotReady = new int[4], slotCharges = new int[4];
    /** Client clock when the state arrived, to count cooldowns down between updates. */
    public static volatile long receivedAt;
    /** Set by the client when a state arrives, so open screens can refresh. */
    public static volatile int version;

    private ClientSkills() {}

    public static boolean has(String node) {
        return unlocked.contains(node);
    }

    public static List<String> slots() {
        return slots;
    }

    public static List<SkillPath> paths() {
        return paths;
    }

    public static void setPaths(List<SkillPath> list) {
        paths = List.copyOf(list);
    }

    public static SkillNode node(String id) {
        for (SkillPath p : paths) for (SkillNode n : p.nodes()) if (n.id().equals(id)) return n;
        return null;
    }

    public static void update(SkillStatePayload s, int slotCount) {
        renown = s.get(0);
        level = s.get(1);
        into = s.get(2);
        toNext = s.get(3);
        points = s.get(4);
        respecCost = s.get(5);
        int[] t = new int[slotCount], r = new int[slotCount], c = new int[slotCount];
        for (int i = 0; i < slotCount; i++) {
            t[i] = s.get(6 + i * 3);
            r[i] = s.get(7 + i * 3);
            c[i] = s.get(8 + i * 3);
        }
        slotTicks = t;
        slotReady = r;
        slotCharges = c;
        unlocked = new HashSet<>(s.unlocked());
        List<String> sl = new ArrayList<>(s.slots());
        while (sl.size() < slotCount) sl.add("");
        slots = List.copyOf(sl);
        receivedAt = System.currentTimeMillis();
        version++;
    }

    /** Seconds until a slot's next charge, counted down since the last update. */
    public static int secondsLeft(int slot) {
        long gone = (System.currentTimeMillis() - receivedAt) / 50;
        long left = slotTicks[slot] - gone;
        return left <= 0 ? 0 : (int) ((left + 19) / 20);
    }
}
