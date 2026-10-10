package com.warfront.skill;

import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import com.warfront.registry.WFRegistry;
import com.warfront.test.TestActions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** What each active skill does, and the skill effects that run over time. */
public final class SkillActives {
    /** Persistent-data flag on projectiles the skill hooks have already seen (or spawned). */
    public static final String SEEN = "warfront_skill_seen";
    public static final String PIERCE = "warfront_pierce";
    public static final String RAIN = "warfront_rain";

    private static final Map<UUID, Vec3> RAIN_AT = new ConcurrentHashMap<>();
    private static final Set<UUID> SHIELD_COOLING = ConcurrentHashMap.newKeySet();
    private static final Queue<Runnable> LATER = new ConcurrentLinkedQueue<>();
    private static final ResourceLocation OVERCHARGE_ID = Warfront.id("skill/overcharge_power");
    private static final ResourceLocation SURGE_ID = Warfront.id("skill/arcane_surge_cooldown");
    private static final ResourceLocation LEY_ID = Warfront.id("skill/ley_tap_regen");
    private static final ResourceLocation DRILL_ID = Warfront.id("skill/drillmaster");
    private static final ResourceLocation SHARED_ARMOR_ID = Warfront.id("skill/shared_armor");

    private SkillActives() {}

    /** Runs on the next server tick (spawning entities from inside a join event isn't safe). */
    public static void later(Runnable r) {
        LATER.add(r);
    }

    public static void clearAll() {
        RAIN_AT.clear();
        SHIELD_COOLING.clear();
        LATER.clear();
        SkillEffects.clearAll();
    }

    /** True when {@code e} is someone {@code p} fights: monsters, raiders, enemy factions. */
    public static boolean hostile(Player p, Entity e) {
        if (!(e instanceof LivingEntity le) || e == p || !le.isAlive()) return false;
        if (e instanceof Player other && (other.isCreative() || other.isSpectator())) return false;
        return Factions.relation(p, e) == Relation.ENEMY;
    }

    private static void say(Player p, String text) {
        p.displayClientMessage(net.minecraft.network.chat.Component.literal(text).withStyle(net.minecraft.ChatFormatting.AQUA), true);
    }

    private static int ticks(SkillNode n) {
        return Math.max(1, n.duration()) * 20;
    }

    /** Starts an active skill. Returns null when it went off, else why it didn't (no cooldown is spent then). */
    @Nullable
    public static String activate(Player p, SkillNode n, float forward, float strafe) {
        switch (n.id()) {
            case "precise_strike", "piercing_shot", "volley" -> {
                SkillEffects.arm(p, n.id(), ticks(n));
                say(p, n.name() + ": your next " + (n.id().equals("precise_strike") ? "hit" : "shot") + " within " + n.duration() + "s.");
                p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.8F, 1.5F);
            }
            case "cleave" -> cleave(p, n);
            case "blade_storm", "shield_wall", "guardians_aegis", "spellblade", "shadow_dance", "kindle_arms" -> {
                SkillEffects.arm(p, n.id(), ticks(n));
                p.level().playSound(null, p.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 0.7F, 1.4F);
                say(p, n.name() + " for " + n.duration() + "s.");
            }
            case "shield_bash" -> {
                return shieldBash(p, n);
            }
            case "rain_of_arrows" -> {
                Vec3 at = p.pick(n.value("range", 48), 1.0F, false).getLocation();
                RAIN_AT.put(p.getUUID(), at);
                SkillEffects.arm(p, n.id(), ticks(n));
                p.level().playSound(null, p.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.2F, 0.6F);
            }
            case "overcharge" -> {
                SkillEffects.arm(p, n.id(), ticks(n));
                IronsBridge.attribute("spell_power").ifPresent(a -> {
                    AttributeInstance inst = p.getAttribute(a);
                    if (inst != null) {
                        inst.removeModifier(OVERCHARGE_ID);
                        inst.addTransientModifier(new AttributeModifier(OVERCHARGE_ID, n.value("power", 0.4), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                    }
                });
                say(p, "Overcharged for " + n.duration() + "s.");
            }
            case "arcane_surge" -> {
                SkillEffects.arm(p, n.id(), ticks(n));
                IronsBridge.attribute("cooldown_reduction").ifPresent(a -> {
                    AttributeInstance inst = p.getAttribute(a);
                    if (inst != null) {
                        inst.removeModifier(SURGE_ID);
                        inst.addTransientModifier(new AttributeModifier(SURGE_ID, 0.5, AttributeModifier.Operation.ADD_VALUE));
                    }
                });
                if (IronsBridge.manaLinked()) IronsBridge.setMana(p, IronsBridge.maxMana(p));
                p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
                say(p, "Arcane Surge: " + n.duration() + "s of free spells and halved cooldowns.");
            }
            case "vanish" -> {
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks(n), 0, false, false));
                double r = n.value("radius", 16);
                for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r), m -> m.getTarget() == p)) m.setTarget(null);
                if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(1), p.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
                p.level().playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            case "dash" -> dash(p, n, forward, strafe);
            case "call_to_arms" -> {
                return callToArms(p, n);
            }
            case "last_order" -> {
                return lastOrder(p, n);
            }
            default -> {
                return n.name() + " isn't built yet.";
            }
        }
        return null;
    }

    private static void cleave(Player p, SkillNode n) {
        double range = n.value("range", 3.5);
        float damage = (float) (p.getAttributeValue(Attributes.ATTACK_DAMAGE) * n.value("damage", 1.0));
        Vec3 look = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z).normalize();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range, 1, range), e -> hostile(p, e))) {
            Vec3 to = new Vec3(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
            if (to.length() > range + e.getBbWidth() / 2 || (to.lengthSqr() > 0.01 && to.normalize().dot(look) < 0.3)) continue;
            e.hurt(p.damageSources().playerAttack(p), damage);
            e.knockback(0.4, -look.x, -look.z);
        }
        if (p.level() instanceof ServerLevel sl) {
            for (int i = -1; i <= 1; i++) {
                Vec3 at = p.position().add(look.yRot(i * 0.6F).scale(1.8)).add(0, p.getBbHeight() * 0.6, 0);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    @Nullable
    private static String shieldBash(Player p, SkillNode n) {
        double range = n.value("range", 4);
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, p.getBoundingBox().expandTowards(p.getLookAngle().scale(range)).inflate(1),
                e -> hostile(p, e), range * range);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) return "No enemy in front of you to bash.";
        target.addEffect(new MobEffectInstance(WFRegistry.STUNNED, (int) Math.round(n.value("seconds", 1.5) * 20), 0), p);
        target.hurt(p.damageSources().playerAttack(p), 2F);
        target.knockback(0.6, Mth.sin(p.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(p.getYRot() * Mth.DEG_TO_RAD));
        p.level().playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.7F);
        return null;
    }

    private static void dash(Player p, SkillNode n, float forward, float strafe) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 dir;
        if (Math.abs(forward) + Math.abs(strafe) < 0.01F) {
            dir = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        } else {
            dir = new Vec3(strafe * Mth.cos(yaw) - forward * Mth.sin(yaw), 0, forward * Mth.cos(yaw) + strafe * Mth.sin(yaw)).normalize();
        }
        double power = n.value("power", 1.6);
        p.setDeltaMovement(dir.x * power, 0.25, dir.z * power);
        p.hurtMarked = true;
        p.fallDistance = 0;
        if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 8, 0.2, 0.1, 0.2, 0.02);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    @Nullable
    private static String callToArms(Player p, SkillNode n) {
        if (!(p.level() instanceof ServerLevel level)) return "Not here.";
        double far = n.value("distance", 32);
        List<SoldierEntity> away = new ArrayList<>();
        for (SoldierEntity s : level.getEntities(WFRegistry.SOLDIER.get(), s -> s.isAlive() && s.isOwnedBy(p) && !s.getRole().worker()
                && !s.isFallen() && s.countsTowardArmy() && s.distanceTo(p) > far)) {
            away.add(s);
        }
        if (away.isEmpty()) return "None of your units are far enough away to call (over " + (int) far + " blocks).";
        away.sort(Comparator.comparingDouble(s -> s.distanceTo(p)));
        int count = (int) n.value("count", 6);
        for (int i = 0; i < Math.min(count, away.size()); i++) {
            SoldierEntity s = away.get(i);
            double ang = i * Math.PI * 2 / Math.min(count, away.size());
            s.teleportTo(p.getX() + Math.cos(ang) * 2, p.getY(), p.getZ() + Math.sin(ang) * 2);
            s.getNavigation().stop();
            level.sendParticles(ParticleTypes.PORTAL, s.getX(), s.getY(1), s.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.PLAYERS, 1.5F, 1.0F);
        say(p, Math.min(count, away.size()) + " units answer the call.");
        return null;
    }

    @Nullable
    private static String lastOrder(Player p, SkillNode n) {
        double r = n.value("radius", 24);
        List<SoldierEntity> fallen = p.level().getEntitiesOfClass(SoldierEntity.class, p.getBoundingBox().inflate(r),
                s -> s.isOwnedBy(p) && s.isFallen());
        if (fallen.isEmpty()) return "No fallen hero of yours within " + (int) r + " blocks.";
        fallen.forEach(SoldierEntity::revive);
        say(p, "Last Order: " + fallen.size() + " hero" + (fallen.size() == 1 ? "" : "es") + " rise again.");
        return null;
    }

    // ------------------------------------------------------------------ ticking

    public static void tick(MinecraftServer server) {
        Runnable r;
        while ((r = LATER.poll()) != null) r.run();
        int t = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive()) continue;
            long storm = SkillEffects.left(p, "blade_storm");
            if (storm > 0 && storm % 10 == 0) bladeStorm(p);
            long rain = SkillEffects.left(p, "rain_of_arrows");
            if (rain > 0 && rain % 4 == 0) rain(p);
            if (t % 5 == 0 && Skills.has(p, "momentum") && p.isBlocking()) momentum(p);
            if (Skills.has(p, "shield_training")) shieldTraining(p);
            if (t % 20 == 0) second(p);
        }
    }

    private static void bladeStorm(ServerPlayer p) {
        SkillNode n = SkillRegistry.node("blade_storm");
        double r = n == null ? 3 : n.value("radius", 3);
        float damage = (float) (p.getAttributeValue(Attributes.ATTACK_DAMAGE) * (n == null ? 0.6 : n.value("damage", 0.6)));
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r, 1, r), e -> hostile(p, e))) {
            if (e.distanceTo(p) > r + 0.5) continue;
            e.hurt(p.damageSources().playerAttack(p), damage);
        }
        ServerLevel sl = p.serverLevel();
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + p.tickCount * 0.3;
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + Math.cos(a) * 1.6, p.getY(0.6), p.getZ() + Math.sin(a) * 1.6, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.2F);
    }

    private static void rain(ServerPlayer p) {
        Vec3 at = RAIN_AT.get(p.getUUID());
        if (at == null) return;
        SkillNode n = SkillRegistry.node("rain_of_arrows");
        double r = n == null ? 3 : n.value("radius", 3);
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < 3; i++) {
            double ang = p.getRandom().nextDouble() * Math.PI * 2, d = Math.sqrt(p.getRandom().nextDouble()) * r;
            Arrow arrow = new Arrow(EntityType.ARROW, level);
            arrow.setOwner(p);
            arrow.setPos(at.x + Math.cos(ang) * d, at.y + 14, at.z + Math.sin(ang) * d);
            arrow.setDeltaMovement(0, -2.2, 0);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.getPersistentData().putBoolean(SEEN, true);
            arrow.getPersistentData().putBoolean(RAIN, true);
            level.addFreshEntity(arrow);
        }
    }

    private static void momentum(ServerPlayer p) {
        Vec3 move = p.getDeltaMovement();
        if (move.horizontalDistanceSqr() < 0.002) return;
        SkillNode n = SkillRegistry.node("momentum");
        double push = n == null ? 0.9 : n.value("push", 0.9);
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(0.8), e -> hostile(p, e))) {
            Vec3 away = new Vec3(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
            if (away.lengthSqr() < 1e-4) continue;
            away = away.normalize();
            e.knockback(push, -away.x, -away.z);
        }
    }

    private static void shieldTraining(ServerPlayer p) {
        ItemStack shield = p.getOffhandItem().getItem() instanceof ShieldItem ? p.getOffhandItem()
                : p.getMainHandItem().getItem() instanceof ShieldItem ? p.getMainHandItem() : ItemStack.EMPTY;
        if (shield.isEmpty()) {
            SHIELD_COOLING.remove(p.getUUID());
            return;
        }
        boolean cooling = p.getCooldowns().isOnCooldown(shield.getItem());
        if (cooling && SHIELD_COOLING.add(p.getUUID())) {
            SkillNode n = SkillRegistry.node("shield_training");
            p.getCooldowns().addCooldown(shield.getItem(), (int) (n == null ? 50 : n.value("disable_ticks", 50)));
        } else if (!cooling) {
            SHIELD_COOLING.remove(p.getUUID());
        }
    }

    private static void second(ServerPlayer p) {
        if (!SkillEffects.active(p, "overcharge")) IronsBridge.attribute("spell_power").ifPresent(a -> removeModifier(p, a, OVERCHARGE_ID));
        if (!SkillEffects.active(p, "arcane_surge")) IronsBridge.attribute("cooldown_reduction").ifPresent(a -> removeModifier(p, a, SURGE_ID));
        else if (IronsBridge.manaLinked()) IronsBridge.setMana(p, IronsBridge.maxMana(p));
        if (!SkillEffects.active(p, "rain_of_arrows")) RAIN_AT.remove(p.getUUID());

        boolean home = Skills.has(p, "ley_tap") && !com.warfront.mana.ManaNetwork.nodes(p.level(), p.blockPosition(),
                Factions.keyOf(p.server, p)).isEmpty();
        if (IronsBridge.loaded()) {
            IronsBridge.attribute("mana_regen").ifPresent(a -> {
                AttributeInstance inst = p.getAttribute(a);
                if (inst == null) return;
                inst.removeModifier(LEY_ID);
                if (home) inst.addTransientModifier(new AttributeModifier(LEY_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            });
        } else if (home) {
            Skills.shortenCooldowns(p, 20);
        }

        // Commander auras on the units around you.
        boolean drill = Skills.has(p, "drillmaster"), shared = Skills.has(p, "shared_armor");
        double drillAmount = Skills.value(p, "drillmaster", "health");
        double share = Skills.value(p, "shared_armor", "share"), shareRange = Skills.value(p, "shared_armor", "radius");
        for (SoldierEntity s : TestActions.owned(p)) {
            setModifier(s, Attributes.MAX_HEALTH, DRILL_ID, drill ? drillAmount : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            double armor = shared && s.distanceTo(p) <= shareRange ? p.getArmorValue() * share : 0;
            setModifier(s, Attributes.ARMOR, SHARED_ARMOR_ID, armor, AttributeModifier.Operation.ADD_VALUE);
        }

        if (Skills.state(p).slots.stream().anyMatch(id -> !id.isEmpty())) Skills.sync(p);
    }

    private static void removeModifier(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, ResourceLocation id) {
        AttributeInstance inst = e.getAttribute(a);
        if (inst != null) inst.removeModifier(id);
    }

    private static void setModifier(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a,
                                    ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(a);
        if (inst == null) return;
        AttributeModifier had = inst.getModifier(id);
        if (had != null && had.amount() == amount) return;
        inst.removeModifier(id);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(id, amount, op));
        if (e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }

    // ------------------------------------------------------------------ projectiles

    /** A projectile the player just loosed: Steady Aim, Piercing Shot, Volley, Retrieval. */
    public static void shot(Player p, net.minecraft.world.entity.projectile.Projectile proj) {
        proj.getPersistentData().putBoolean(SEEN, true);
        if (Skills.has(p, "steady_aim")) {
            double trueness = Skills.value(p, "steady_aim", "true");
            Vec3 v = proj.getDeltaMovement();
            double speed = v.length();
            if (speed > 0.1) {
                Vec3 blended = v.normalize().scale(1 - trueness).add(p.getLookAngle().scale(trueness)).normalize().scale(speed);
                proj.setDeltaMovement(blended);
            }
        }
        if (SkillEffects.consume(p, "piercing_shot")) {
            proj.getPersistentData().putBoolean(PIERCE, true);
            if (proj instanceof AbstractArrow a) {
                SkillNode n = SkillRegistry.node("piercing_shot");
                a.setBaseDamage(a.getBaseDamage() * (1 + (n == null ? 0.5 : n.value("damage", 0.5))));
            }
        }
        if (SkillEffects.consume(p, "volley")) {
            SkillNode n = SkillRegistry.node("volley");
            int extra = n == null ? 4 : (int) n.value("extra", 4);
            later(() -> volley(proj, extra));
        }
        if (proj instanceof AbstractArrow a && a.pickup == AbstractArrow.Pickup.ALLOWED && !p.getAbilities().instabuild
                && Skills.has(p, "retrieval") && p.getRandom().nextDouble() < Skills.value(p, "retrieval", "chance")) {
            ItemStack back = a.getPickupItemStackOrigin().copyWithCount(1);
            if (!back.isEmpty()) {
                a.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                if (!p.getInventory().add(back)) p.drop(back, false);
            }
        }
    }

    private static void volley(net.minecraft.world.entity.projectile.Projectile proj, int extra) {
        if (!(proj.level() instanceof ServerLevel level) || proj.isRemoved()) return;
        Vec3 v = proj.getDeltaMovement();
        for (int i = 0; i < extra; i++) {
            Entity copy = proj.getType().create(level);
            if (copy == null) continue;
            copy.load(proj.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
            copy.setUUID(UUID.randomUUID());
            copy.getPersistentData().putBoolean(SEEN, true);
            float spread = (i < extra / 2 ? -1 : 1) * (0.12F + 0.12F * (i % Math.max(1, extra / 2)));
            copy.setDeltaMovement(v.yRot(spread));
            if (copy instanceof AbstractArrow a) a.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            level.addFreshEntity(copy);
        }
    }

    /** Which entities a piercing projectile has already hit. */
    public static boolean firstPierce(Entity proj, Entity target) {
        var tag = proj.getPersistentData();
        Set<Integer> hit = new HashSet<>();
        for (int id : tag.getIntArray("warfront_pierced")) hit.add(id);
        if (!hit.add(target.getId())) return false;
        tag.putIntArray("warfront_pierced", hit.stream().mapToInt(Integer::intValue).toArray());
        return true;
    }
}
