package com.warfront.faction;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.Locale;
import java.util.Optional;

/**
 * Iron's Spells schools. Each race casts one school stronger and one weaker by changing that school's
 * spell power attribute. Iron's Spells is optional: without it the attributes don't exist and nothing happens.
 */
public enum SpellSchool {
    FIRE, ICE, LIGHTNING, HOLY, ENDER, BLOOD, EVOCATION, NATURE, ELDRITCH;

    /** Bonus to a race's own school, as a fraction of base spell power. */
    public static final double BONUS = 0.25;
    /** Penalty to a race's opposite school. */
    public static final double PENALTY = -0.10;

    public String displayName() {
        String n = name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** The school's spell power attribute, or empty when Iron's Spells isn't installed. */
    public Optional<Holder.Reference<Attribute>> powerAttribute() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("irons_spellbooks",
                name().toLowerCase(Locale.ROOT) + "_spell_power");
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(Registries.ATTRIBUTE, id));
    }
}
