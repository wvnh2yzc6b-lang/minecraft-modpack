package com.warfront.skill;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Renown: a player's battle experience. Each level gives a skill point. Tuned so a player is about level 10 by a
 * level 3 base and about 25 by the end of the campaign.
 */
public final class Renown {
    public static final int MAX_LEVEL = 50;
    public static final int WAVE = 30, WAVE_PER = 5, CAMP = 80, WARLORD = 300, HIDDEN_WARLORD = 600, HERO_KILL = 25, BEAST_KILL = 30;

    private Renown() {}

    /** Renown needed to go from {@code level} to the next. */
    public static int toNext(int level) {
        return 50 + 25 * level;
    }

    /** Renown needed in total to reach {@code level}. */
    public static int total(int level) {
        int sum = 0;
        for (int l = 0; l < level; l++) sum += toNext(l);
        return sum;
    }

    public static int levelOf(int renown) {
        int level = 0;
        while (level < MAX_LEVEL && renown >= total(level + 1)) level++;
        return level;
    }

    /** Renown for a kill: small for common foes, more for Captains, Champions and beasts. */
    public static int forKill(LivingEntity victim) {
        if (victim instanceof SoldierEntity s) {
            if (s.isWarlord()) return WARLORD / 3;
            if (s.getRole() == SoldierRole.BEAST) return BEAST_KILL;
            if (s.getRole() == SoldierRole.CAPTAIN || s.getRole() == SoldierRole.CHAMPION) return HERO_KILL;
        }
        return Math.max(1, Math.min(8, Math.round(victim.getMaxHealth() / 5F)));
    }
}
