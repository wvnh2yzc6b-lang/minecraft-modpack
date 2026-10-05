package com.warfront.faction;

import com.warfront.Warfront;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Playable races. Each race changes the attributes of players and of the soldiers they recruit.
 */
public enum Race {
    //        color                   hp    speed  atk   armor scale  morale  routs
    HUMAN(ChatFormatting.GOLD,         0,   0.00,  0.0,  0,    1.00,  1.5,    true,
            "Balanced and disciplined. Morale recovers quickly."),
    ELF(ChatFormatting.GREEN,         -2,   0.10,  0.0,  0,    1.05,  1.0,    true,
            "Swift and keen-eyed. Faster movement, deadly archers."),
    DWARF(ChatFormatting.AQUA,         4,  -0.08,  0.0,  2,    0.85,  1.0,    false,
            "Stout and stubborn. Extra health and armor; dwarven soldiers never rout."),
    ORC(ChatFormatting.DARK_GREEN,     2,   0.00,  1.5,  0,    1.10,  0.6,    true,
            "Brutal and towering. Hits harder, but morale is fragile.");

    private static final ResourceLocation HEALTH_ID = Warfront.id("race_health");
    private static final ResourceLocation SPEED_ID = Warfront.id("race_speed");
    private static final ResourceLocation ATTACK_ID = Warfront.id("race_attack");
    private static final ResourceLocation ARMOR_ID = Warfront.id("race_armor");
    private static final ResourceLocation SCALE_ID = Warfront.id("race_scale");

    public final ChatFormatting color;
    public final double health;
    public final double speed;
    public final double attack;
    public final double armor;
    public final double scale;
    public final double moraleRegen;
    public final boolean routs;
    public final String description;

    Race(ChatFormatting color, double health, double speed, double attack, double armor, double scale,
         double moraleRegen, boolean routs, String description) {
        this.color = color;
        this.health = health;
        this.speed = speed;
        this.attack = attack;
        this.armor = armor;
        this.scale = scale;
        this.moraleRegen = moraleRegen;
        this.routs = routs;
        this.description = description;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        String n = id();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    @Nullable
    public static Race byId(@Nullable String id) {
        if (id == null) return null;
        for (Race r : values()) {
            if (r.id().equalsIgnoreCase(id)) return r;
        }
        return null;
    }

    public static Race byOrdinal(int i) {
        Race[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    /** Applies (or refreshes) this race's attribute modifiers on an entity. */
    public void apply(LivingEntity entity) {
        set(entity, Attributes.MAX_HEALTH, HEALTH_ID, health, AttributeModifier.Operation.ADD_VALUE);
        set(entity, Attributes.MOVEMENT_SPEED, SPEED_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(entity, Attributes.ATTACK_DAMAGE, ATTACK_ID, attack, AttributeModifier.Operation.ADD_VALUE);
        set(entity, Attributes.ARMOR, ARMOR_ID, armor, AttributeModifier.Operation.ADD_VALUE);
        set(entity, Attributes.SCALE, SCALE_ID, scale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    public static void clear(LivingEntity entity) {
        remove(entity, Attributes.MAX_HEALTH, HEALTH_ID);
        remove(entity, Attributes.MOVEMENT_SPEED, SPEED_ID);
        remove(entity, Attributes.ATTACK_DAMAGE, ATTACK_ID);
        remove(entity, Attributes.ARMOR, ARMOR_ID);
        remove(entity, Attributes.SCALE, SCALE_ID);
    }

    private static void set(LivingEntity entity, Holder<Attribute> attr, ResourceLocation id, double amount,
                            AttributeModifier.Operation op) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) {
            inst.addPermanentModifier(new AttributeModifier(id, amount, op));
        }
    }

    private static void remove(LivingEntity entity, Holder<Attribute> attr, ResourceLocation id) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }
}
