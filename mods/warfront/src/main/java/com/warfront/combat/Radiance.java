package com.warfront.combat;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.Warfront;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Angel race power: Judgment. Radiance (0-100) gathers in daylight under open sky (+2 a second, +5 per hit struck in
 * sunlight) and fades at night or underground. At 100 the power key calls a sunbeam up to 48 blocks away: after a
 * one-second ring of light it strikes a 4-block radius for 12 damage (half again to undead, demons, the Hive and the
 * {@code warfront:smite_bonus} entity tag), blinding and outlining them. Starbow archers and the Seraph Champion call
 * it on their own, the Seraph's twice as wide. No blocks broken, no fires, never the caster's side.
 */
public final class Radiance {
    public static final int MAX = 100;
    public static final int SUN_GAIN = 2;
    public static final int HIT_GAIN = 5;
    public static final int FADE = 1;
    public static final int RANGE = 48;
    public static final double RADIUS = 4;
    public static final float DAMAGE = 12F;
    public static final float BONUS = 1.5F;
    public static final int TELEGRAPH = 20;
    /** Entity types other mods can mark for Judgment's bonus damage. */
    public static final TagKey<EntityType<?>> SMITE_BONUS = TagKey.create(Registries.ENTITY_TYPE, Warfront.id("smite_bonus"));

    private static final String KEY = "warfront_radiance";

    /** A sunbeam waiting to fall. */
    private record Strike(ServerLevel level, Vec3 at, String key, double radius, long due) {}

    private static final List<Strike> PENDING = new ArrayList<>();

    private Radiance() {}

    public static int current(LivingEntity e) {
        return e.getPersistentData().getInt(KEY);
    }

    public static void set(LivingEntity e, int value) {
        e.getPersistentData().putInt(KEY, Mth.clamp(value, 0, MAX));
    }

    /** Daylight with open sky above and no rain. */
    public static boolean sunlit(LivingEntity e) {
        BlockPos head = BlockPos.containing(e.getEyePosition());
        return e.level().isDay() && e.level().canSeeSky(head) && !e.level().isRainingAt(head);
    }

    public static void tickSecond(LivingEntity e) {
        tickSecond(e, sunlit(e));
    }

    public static void tickSecond(LivingEntity e, boolean sunlit) {
        int before = current(e);
        if (sunlit) set(e, before + SUN_GAIN);
        else set(e, before - FADE);
        if (e instanceof ServerPlayer p && before < MAX && current(e) >= MAX) {
            p.displayClientMessage(Component.literal("Radiance full: press R to call Judgment.").withStyle(ChatFormatting.YELLOW), true);
        }
        if (e instanceof SoldierEntity s && current(s) >= MAX) soldierJudgment(s);
    }

    /** A hit struck in sunlight. */
    public static void onHit(LivingEntity attacker) {
        if (sunlit(attacker)) set(attacker, current(attacker) + HIT_GAIN);
    }

    /** The player's power key: Judgment at whatever they look at within 48 blocks. */
    public static void judgment(ServerPlayer p) {
        if (current(p) < MAX) {
            RacePower.say(p, "Radiance " + current(p) + "/" + MAX + ": stand in the sun to gather more.");
            return;
        }
        Vec3 at = aim(p);
        if (at == null) {
            RacePower.say(p, "Nothing in reach to call Judgment on (48 blocks).");
            return;
        }
        set(p, 0);
        call(p.serverLevel(), at, Factions.keyOf(p.server, p), RADIUS);
    }

    private static Vec3 aim(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(RANGE));
        HitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 reach = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p.level(), p, eye, reach,
                new AABB(eye, reach).inflate(1), e -> e instanceof LivingEntity && e.isAlive() && e != p);
        if (hit != null) return hit.getEntity().position();
        return block.getType() == HitResult.Type.MISS ? null : block.getLocation();
    }

    /** Starts the telegraph: a ring of light and a rising chime; the beam falls a second later. */
    public static void call(ServerLevel level, Vec3 at, String key, double radius) {
        PENDING.add(new Strike(level, at, key, radius, level.getGameTime() + TELEGRAPH));
        level.playSound(null, BlockPos.containing(at), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.6F);
    }

    /** Every server tick: telegraph rings and falling beams. */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        PENDING.removeIf(s -> {
            long left = s.due() - s.level().getGameTime();
            if (left > 0) {
                if (left % 4 == 0) ring(s.level(), s.at(), s.radius());
                return false;
            }
            strike(s.level(), s.at(), s.key(), s.radius());
            return true;
        });
    }

    private static void ring(ServerLevel level, Vec3 at, double radius) {
        for (int i = 0; i < 32; i++) {
            double a = i * Mth.TWO_PI / 32;
            level.sendParticles(ParticleTypes.END_ROD, at.x + Math.cos(a) * radius, at.y + 0.1, at.z + Math.sin(a) * radius, 1, 0, 0.02, 0, 0);
        }
        level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F + level.random.nextFloat() * 0.6F);
    }

    /** The beam falls: damage to the caster's enemies in the radius. Returns who was hit and for how much. */
    public static Map<LivingEntity, Float> strike(ServerLevel level, Vec3 at, String key, double radius) {
        Map<LivingEntity, Float> hits = new LinkedHashMap<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 6, radius),
                e -> e.isAlive() && horizontal(e.position(), at) <= radius
                        && Factions.relation(level.getServer(), key, Factions.keyOf(level.getServer(), e)) == Relation.ENEMY)) {
            float dmg = DAMAGE * (smiteBonus(e) ? BONUS : 1F);
            e.hurt(level.damageSources().magic(), dmg);
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            hits.put(e, dmg);
        }
        for (int y = 0; y < 24; y++) {
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 4, 0.25, 0.4, 0.25, 0.01);
        }
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, BlockPos.containing(at), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
        return hits;
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static boolean smiteBonus(LivingEntity e) {
        Race race = Race.of(e);
        return e.getType().is(EntityTypeTags.UNDEAD) || e.getType().is(SMITE_BONUS) || race == Race.DEMON || race == Race.HIVE;
    }

    /** Starbows and the Seraph call Judgment on the thickest knot of enemies they can find. */
    private static void soldierJudgment(SoldierEntity s) {
        if (s.getRole() != SoldierRole.ARCHER && s.getRole() != SoldierRole.CHAMPION) return;
        if (!(s.level() instanceof ServerLevel level)) return;
        String key = s.getFactionKey();
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, s.getBoundingBox().inflate(32),
                e -> e.isAlive() && Factions.relation(level.getServer(), key, Factions.keyOf(level.getServer(), e)) == Relation.ENEMY);
        if (enemies.isEmpty()) return;
        double radius = s.getRole() == SoldierRole.CHAMPION ? RADIUS * 2 : RADIUS;
        LivingEntity best = null;
        int most = 0;
        for (LivingEntity e : enemies) {
            int n = 0;
            for (LivingEntity o : enemies) if (horizontal(o.position(), e.position()) <= radius) n++;
            if (n > most) {
                most = n;
                best = e;
            }
        }
        set(s, 0);
        call(level, best.position(), key, radius);
    }

    public static void clearAll() {
        PENDING.clear();
    }
}
