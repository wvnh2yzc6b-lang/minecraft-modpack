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
    //        color                   hp    speed  atk   armor scale  morale  routs  mana
    //        strong school, weak school (Iron's Spells, optional)
    HUMAN(ChatFormatting.GOLD,         0,   0.00,  0.0,  0,    1.00,  1.5,    true,  1.0,
            SpellSchool.EVOCATION, SpellSchool.ELDRITCH,
            "Balanced and disciplined. Morale recovers quickly; human healers heal more."),
    ELF(ChatFormatting.GREEN,         -2,   0.10,  0.0,  0,    1.05,  1.0,    true,  1.0,
            SpellSchool.NATURE, SpellSchool.FIRE,
            "Swift and keen-eyed master archers. Arrows fly faster and hit 25% harder; long shots hit harder still and mark the target."),
    DWARF(ChatFormatting.AQUA,         4,  -0.08,  0.0,  2,    0.85,  1.0,    false, 1.0,
            SpellSchool.LIGHTNING, SpellSchool.ENDER,
            "Stout and stubborn. Extra health and armor; dwarven soldiers never rout."),
    ORC(ChatFormatting.DARK_GREEN,     2,   0.00,  1.5,  0,    1.10,  0.6,    true,  1.0,
            SpellSchool.BLOOD, SpellSchool.EVOCATION,
            "Brutal and towering. Fighting builds rage that bursts into a Frenzy: faster, deadlier, reckless. Morale is fragile."),
    DEMON(ChatFormatting.DARK_RED,     2,   0.00,  1.0,  1,    1.00,  1.0,    true,  1.15,
            SpellSchool.FIRE, SpellSchool.HOLY,
            "Born of hellfire. Immune to fire and lava; demon soldiers set their foes ablaze."),
    ANGEL(ChatFormatting.YELLOW,       0,   0.05,  0.0,  0,    1.08,  2.0,    false, 1.2,
            SpellSchool.HOLY, SpellSchool.BLOOD,
            "Radiant and unbreakable. No fall damage, slow regeneration, +50% damage to undead and demons."),
    HIVE(ChatFormatting.LIGHT_PURPLE, -4,   0.12,  0.0,  1,    0.90,  1.0,    false, 0.6,
            SpellSchool.ELDRITCH, SpellSchool.HOLY,
            "One mind, many bodies. Cheap fast swarmers that never rout and grow stronger in packs.");

    private static final ResourceLocation HEALTH_ID = Warfront.id("race_health");
    private static final ResourceLocation SPEED_ID = Warfront.id("race_speed");
    private static final ResourceLocation ATTACK_ID = Warfront.id("race_attack");
    private static final ResourceLocation ARMOR_ID = Warfront.id("race_armor");
    private static final ResourceLocation SCALE_ID = Warfront.id("race_scale");
    private static final ResourceLocation SCHOOL_ID = Warfront.id("race_school");

    public final ChatFormatting color;
    public final double health;
    public final double speed;
    public final double attack;
    public final double armor;
    public final double scale;
    public final double moraleRegen;
    public final boolean routs;
    /** Multiplier on the mana cost of recruiting soldiers of this race. */
    public final double manaCost;
    /** Iron's Spells school this race casts stronger, and the one it casts weaker. */
    public final SpellSchool school;
    public final SpellSchool weakSchool;
    public final String description;

    Race(ChatFormatting color, double health, double speed, double attack, double armor, double scale,
         double moraleRegen, boolean routs, double manaCost, SpellSchool school, SpellSchool weakSchool,
         String description) {
        this.color = color;
        this.health = health;
        this.speed = speed;
        this.attack = attack;
        this.armor = armor;
        this.scale = scale;
        this.moraleRegen = moraleRegen;
        this.routs = routs;
        this.manaCost = manaCost;
        this.school = school;
        this.weakSchool = weakSchool;
        this.description = description;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        String n = id();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** One line on this race's magic, shown with its description. */
    public String magicLine() {
        return "Magic: +" + Math.round(SpellSchool.BONUS * 100) + "% " + school.displayName()
                + " spell power, " + Math.round(SpellSchool.PENALTY * 100) + "% " + weakSchool.displayName() + ".";
    }

    @Nullable
    public static Race byId(@Nullable String id) {
        if (id == null) return null;
        for (Race r : values()) {
            if (r.id().equalsIgnoreCase(id)) return r;
        }
        return null;
    }

    /** The race of a soldier or player, or null for anything else (or a player who has not chosen). */
    @Nullable
    public static Race of(net.minecraft.world.entity.Entity e) {
        if (e instanceof com.warfront.entity.SoldierEntity s) return s.getRace();
        if (e instanceof net.minecraft.world.entity.player.Player p) {
            return byId(p.getData(com.warfront.registry.WFRegistry.RACE));
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
        for (SpellSchool s : SpellSchool.values()) {
            double amount = s == school ? SpellSchool.BONUS : s == weakSchool ? SpellSchool.PENALTY : 0;
            s.powerAttribute().ifPresent(a -> set(entity, a, SCHOOL_ID, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
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
        for (SpellSchool s : SpellSchool.values()) {
            s.powerAttribute().ifPresent(a -> remove(entity, a, SCHOOL_ID));
        }
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
