package com.warfront.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A player's Renown, learned nodes, skill slots and cooldowns (a player attachment, kept on death). */
public final class SkillState {
    public static final int SLOTS = 4;

    /** Renown earned in total (the level comes from it). */
    public int renown;
    /** Extra skill points beyond one per level (warlords give one each). */
    public int bonusPoints;
    /** Points paid for the learned nodes. */
    public int spent;
    public final List<String> unlocked;
    /** Active skills in the four slots ("" for empty). */
    public final List<String> slots;
    /** Game time each cooldown is fully over, by node id (plus internal timers like Last Stand). */
    public final Map<String, Long> readyAt;

    public static final Codec<SkillState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("renown", 0).forGetter(s -> s.renown),
            Codec.INT.optionalFieldOf("bonus", 0).forGetter(s -> s.bonusPoints),
            Codec.INT.optionalFieldOf("spent", 0).forGetter(s -> s.spent),
            Codec.STRING.listOf().optionalFieldOf("unlocked", List.of()).forGetter(s -> s.unlocked),
            Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(s -> s.slots),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("ready", Map.of()).forGetter(s -> s.readyAt)
    ).apply(i, SkillState::new));

    public SkillState() {
        this(0, 0, 0, List.of(), List.of(), Map.of());
    }

    public SkillState(int renown, int bonusPoints, int spent, List<String> unlocked, List<String> slots, Map<String, Long> readyAt) {
        this.renown = renown;
        this.bonusPoints = bonusPoints;
        this.spent = spent;
        this.unlocked = new ArrayList<>(unlocked);
        this.slots = new ArrayList<>(slots);
        while (this.slots.size() < SLOTS) this.slots.add("");
        while (this.slots.size() > SLOTS) this.slots.remove(this.slots.size() - 1);
        this.readyAt = new HashMap<>(readyAt);
    }

    public int level() {
        return Renown.levelOf(renown);
    }

    public int points() {
        return level() + bonusPoints - spent;
    }

    public boolean has(String node) {
        return unlocked.contains(node);
    }
}
