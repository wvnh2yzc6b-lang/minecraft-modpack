package com.warfront.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TowerType implements StringRepresentable {
    /** Shoots arrows at enemies. */
    ARROW(25),
    /** Summons evoker fangs beneath enemies. */
    ARCANE(50),
    /** Heals allies around it. */
    HEALING(40);

    public static final Codec<TowerType> CODEC = StringRepresentable.fromEnum(TowerType::values);

    public final int interval;

    TowerType(int interval) {
        this.interval = interval;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
