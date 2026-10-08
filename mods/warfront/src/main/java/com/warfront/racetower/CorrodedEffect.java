package com.warfront.racetower;

import com.warfront.Warfront;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Hive acid eating at armor: -3 armor per level. */
public class CorrodedEffect extends MobEffect {
    public CorrodedEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FD02A);
        addAttributeModifier(Attributes.ARMOR, Warfront.id("corroded_armor"), -3.0, AttributeModifier.Operation.ADD_VALUE);
    }
}
