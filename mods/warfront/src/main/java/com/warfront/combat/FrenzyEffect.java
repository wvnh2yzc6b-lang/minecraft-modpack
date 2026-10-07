package com.warfront.combat;

import com.warfront.Warfront;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * An orc's frenzy: hits harder, swings and runs faster, and takes more damage (see GameEvents).
 * A plain mob effect, so other mods can see, cure or extend it.
 */
public class FrenzyEffect extends MobEffect {
    public FrenzyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9E1B12);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, Warfront.id("frenzy_damage"), 0.30,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(Attributes.ATTACK_SPEED, Warfront.id("frenzy_attack_speed"), 0.20,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, Warfront.id("frenzy_speed"), 0.15,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
