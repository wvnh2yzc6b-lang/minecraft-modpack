package com.warfront.combat;

import com.warfront.Warfront;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The dwarves' Oath of Stone: rooted in place (+80% knockback resistance), 30% less damage taken (GameEvents). */
public class OathOfStoneEffect extends MobEffect {
    public OathOfStoneEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8A9099);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, Warfront.id("oath_knockback"), 0.8,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
