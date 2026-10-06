package com.warfront.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TowerType implements StringRepresentable {
    /** Shoots arrows at enemies. */
    ARROW(25, 1),
    /** Summons evoker fangs beneath enemies. */
    ARCANE(50, 4),
    /** Heals allies around it. */
    HEALING(40, 2);

    public static final Codec<TowerType> CODEC = StringRepresentable.fromEnum(TowerType::values);

    public final int interval;
    /** Mana drawn from the network each time the tower fires or heals. */
    public final int manaCost;

    TowerType(int interval, int manaCost) {
        this.interval = interval;
        this.manaCost = manaCost;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
