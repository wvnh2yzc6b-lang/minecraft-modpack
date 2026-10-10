package com.warfront.skill;

import com.warfront.Warfront;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * The optional link to Iron's Spells: its attributes by id and its spell mana through reflection, so Warfront
 * builds and runs without it. Any failure turns the mana link off for the session (logged once).
 */
public final class IronsBridge {
    private static Boolean loaded;
    private static boolean broken;
    private static Method getData, getMana, setMana;

    private IronsBridge() {}

    public static boolean loaded() {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded("irons_spellbooks");
        return loaded;
    }

    public static Optional<Holder.Reference<Attribute>> attribute(String path) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(Registries.ATTRIBUTE,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", path)));
    }

    private static boolean link() {
        if (!loaded() || broken) return false;
        if (getData != null) return true;
        try {
            Class<?> data = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
            getData = data.getMethod("getPlayerMagicData", LivingEntity.class);
            getMana = data.getMethod("getMana");
            setMana = data.getMethod("setMana", float.class);
            return true;
        } catch (Throwable t) {
            broken = true;
            Warfront.LOGGER.warn("Warfront: couldn't link Iron's Spells mana ({}); mana skills use their fallbacks", t.toString());
            return false;
        }
    }

    /** Whether spell mana can be read and changed. */
    public static boolean manaLinked() {
        return link();
    }

    public static float mana(LivingEntity e) {
        if (!link()) return 0;
        try {
            return (float) getMana.invoke(getData.invoke(null, e));
        } catch (Throwable t) {
            broken = true;
            return 0;
        }
    }

    public static void setMana(LivingEntity e, float amount) {
        if (!link()) return;
        try {
            setMana.invoke(getData.invoke(null, e), Math.max(0, Math.min(amount, maxMana(e))));
        } catch (Throwable t) {
            broken = true;
        }
    }

    public static float maxMana(LivingEntity e) {
        return attribute("max_mana").map(a -> {
            AttributeInstance inst = e.getAttribute(a);
            return inst == null ? 0F : (float) inst.getValue();
        }).orElse(0F);
    }
}
