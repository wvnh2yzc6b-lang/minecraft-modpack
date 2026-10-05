package com.warfront.army;

public enum Formation {
    LINE("Battle Line", "Ranks of eight, front-liners first and archers at the back."),
    SHIELD_WALL("Shield Wall", "A tight, wide wall of shields with spears behind it."),
    WEDGE("Wedge", "An arrowhead to break enemy lines."),
    SQUARE("Square", "Melee troops in a ring facing out, archers and healers protected inside."),
    SKIRMISH("Skirmish", "Loose, wide spacing to avoid arrows and area attacks.");

    public final String title;
    public final String description;

    Formation(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public static Formation byOrdinal(int i) {
        Formation[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    public Formation next() {
        return byOrdinal(ordinal() + 1);
    }
}
