package com.warfront.army;

public enum Order {
    /** Move in formation behind the commander. */
    FOLLOW("Follow me", "Soldiers form up behind you and march with you."),
    /** Hold formation at a fixed point, facing a fixed direction. Engage only nearby threats. */
    HOLD("Hold the line", "Soldiers form up where you stand, facing where you look, and hold."),
    /** Break formation and attack anything hostile, advancing on the charge point. */
    CHARGE("Charge!", "Soldiers break ranks and charge the point you are looking at."),
    /** NPC warbands: march in formation towards an objective, behind their leader. */
    MARCH("March", "Advance in formation towards the objective.");

    public final String title;
    public final String description;

    Order(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public static Order byOrdinal(int i) {
        Order[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    /** The order a player's baton switches to next (MARCH is reserved for NPC armies). */
    public Order nextForPlayer() {
        return switch (this) {
            case FOLLOW -> HOLD;
            case HOLD -> CHARGE;
            default -> FOLLOW;
        };
    }
}
