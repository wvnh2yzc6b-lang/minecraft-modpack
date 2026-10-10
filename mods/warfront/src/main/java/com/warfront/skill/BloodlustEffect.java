package com.warfront.skill;

import com.warfront.Warfront;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Bloodlust (Blade path): +10% attack speed per stack (amplifier + 1). */
public class BloodlustEffect extends MobEffect {
    public BloodlustEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB8231A);
        addAttributeModifier(Attributes.ATTACK_SPEED, Warfront.id("bloodlust_speed"), 0.10,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
