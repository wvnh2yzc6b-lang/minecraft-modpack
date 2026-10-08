package com.warfront.combat;

import com.warfront.Warfront;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Rallied by a human's banner: hits 20% harder and won't rout (see SoldierEntity). */
public class RalliedEffect extends MobEffect {
    public RalliedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE2B55A);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, Warfront.id("rallied_damage"), 0.20,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
