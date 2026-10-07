package com.warfront.army;

/**
 * Watch duty for battle units. A unit on duty leaves the army (the baton no longer moves it), holds a post,
 * and raises the alarm to nearby troops when enemies come.
 */
public enum Duty {
    /** Marches and fights with the army. */
    NONE("with the army", 0),
    /** Stands at its post and walks a short beat around it. */
    GUARD("on guard", 6),
    /** Walks a wide circuit around its post. */
    PATROL("on patrol", 16);

    public final String title;
    /** How far from the post the unit walks its beat. */
    public final int beat;

    Duty(String title, int beat) {
        this.title = title;
        this.beat = beat;
    }

    public static Duty byOrdinal(int i) {
        Duty[] v = values();
        return i >= 0 && i < v.length ? v[i] : NONE;
    }
}
