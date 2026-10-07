package com.warfront.army;

import com.warfront.config.WFConfig;

/**
 * Five ranks for battle units: Recruit, Regular, Veteran, Elite, Legend. Kills and won siege waves give XP; each
 * rank adds health and damage, and Elite and Legend units never rout. Workers don't rank.
 */
public final class Veterancy {
    public static final String[] TITLES = {"Recruit", "Regular", "Veteran", "Elite", "Legend"};
    public static final int MAX_RANK = TITLES.length - 1;
    /** XP for surviving a won siege wave near the War Standard. */
    public static final int WAVE_XP = 25;

    private Veterancy() {}

    /** XP needed for {@code rank} (0 for Recruit). */
    public static int threshold(int rank) {
        return rank <= 0 ? 0 : WFConfig.RANK_XP[Math.min(rank, MAX_RANK) - 1].get();
    }

    public static int rankFor(int xp) {
        int rank = 0;
        while (rank < MAX_RANK && xp >= threshold(rank + 1)) rank++;
        return rank;
    }

    /** Health and damage bonus for a rank, as a fraction (0.1 per rank by default). */
    public static double bonus(int rank) {
        return rank * WFConfig.RANK_BONUS.get();
    }

    public static boolean neverRouts(int rank) {
        return rank >= 3;
    }

    public static boolean ranks(SoldierRole role) {
        return !role.worker() && !role.retired();
    }

    /** XP for killing something with this much max health. */
    public static int killXp(float victimMaxHealth) {
        return Math.max(1, Math.round(victimMaxHealth / 2F));
    }
}
