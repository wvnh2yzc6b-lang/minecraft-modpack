package com.warfront.upkeep;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Fed from the Mess Hall this morning: morale holds 10% better and wounds slowly close out of combat (see
 * SoldierEntity). A plain mob effect, so other mods can see it.
 */
public class WellFedEffect extends MobEffect {
    public WellFedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD9A441);
    }
}
