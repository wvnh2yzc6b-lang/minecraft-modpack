package com.warfront.skill;

import com.warfront.Warfront;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.network.SkillTreePayload;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The skill tree's passives and the hooks its active skills need. */
@EventBusSubscriber(modid = Warfront.MODID)
public final class SkillHooks {
    /** Weapons the Blade path counts: swords, axes, and modded melee weapons in the conventional tags. */
    public static final TagKey<Item> MELEE_WEAPONS = ItemTags.create(Warfront.id("skill_melee_weapons"));
    /** Ranged weapons the Marksman path counts (bows, crossbows and tridents count anyway). */
    public static final TagKey<Item> RANGED_WEAPONS = ItemTags.create(Warfront.id("skill_ranged_weapons"));
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    public static final String MARK = "warfront_hunters_mark", MARK_UNTIL = "warfront_hunters_mark_until";

    private static final Map<UUID, UUID> DANCE_TARGET = new ConcurrentHashMap<>();
    private static boolean inAegis;

    private SkillHooks() {}

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new SkillRegistry());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SkillActives.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SkillActives.clearAll();
        DANCE_TARGET.clear();
    }

    // ------------------------------------------------------------------ players

    private static void welcome(Player player) {
        Skills.refresh(player);
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new SkillTreePayload(SkillRegistry.encode(), false));
            Skills.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        welcome(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        welcome(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SkillEffects.clear(event.getEntity());
        DANCE_TARGET.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ damage

    public static boolean isRanged(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem || stack.is(RANGED_WEAPONS);
    }

    /** Armor's share of a hit, by the vanilla formula: what's left after armor (0..1). */
    static float armorFactor(LivingEntity v, float damage) {
        float armor = v.getArmorValue();
        float toughness = (float) v.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float f = 2.0F + toughness / 4.0F;
        float g = Mth.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        return Math.max(0.2F, 1.0F - g / 25.0F);
    }

    static boolean behind(Entity attacker, LivingEntity victim) {
        Vec3 facing = new Vec3(victim.getLookAngle().x, 0, victim.getLookAngle().z);
        Vec3 to = new Vec3(attacker.getX() - victim.getX(), 0, attacker.getZ() - victim.getZ());
        if (facing.lengthSqr() < 1e-4 || to.lengthSqr() < 1e-4) return false;
        return facing.normalize().dot(to.normalize()) < -0.3;
    }

    /** Slowed, stunned or routing: Opportunist's targets. */
    static boolean disabled(LivingEntity v) {
        return v.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || v.hasEffect(WFRegistry.STUNNED)
                || v instanceof SoldierEntity s && s.isRouting();
    }

    /** Captains, Champions, warlords and bosses: Headhunter's targets. */
    static boolean bigGame(LivingEntity v) {
        if (v instanceof SoldierEntity s) {
            return s.isWarlord() || s.getRole() == SoldierRole.CAPTAIN || s.getRole() == SoldierRole.CHAMPION
                    || com.warfront.finale.HiddenWarlord.isHim(s);
        }
        return v.getType().is(BOSSES) || v instanceof EnderDragon || v instanceof WitherBoss || v instanceof Warden;
    }

    /** The magic power the player's spells and Spellblade carry: their school's Iron's power, or Attunement without it. */
    static double magicPower(Player p) {
        Race race = Race.of(p);
        if (IronsBridge.loaded() && race != null) {
            var attr = race.school.powerAttribute();
            if (attr.isPresent() && p.getAttribute(attr.get()) != null) return p.getAttributeValue(attr.get());
        }
        return 1.0 + Skills.value(p, "attunement", "magic");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = event.getSource();
        Entity attacker = src.getEntity();
        Entity direct = src.getDirectEntity();
        if (direct != null && attacker == victim && direct.getPersistentData().getBoolean(SkillActives.RAIN)) {
            event.setCanceled(true);   // your own rain of arrows never hits you
            return;
        }
        if (victim instanceof Player pv && defend(event, pv, attacker, direct)) return;
        aegis(event, victim);
        if (attacker instanceof Player pa && pa != victim) {
            offense(event, pa, victim, direct);
        } else if (attacker instanceof SoldierEntity unit && unit != victim) {
            unitOffense(event, unit, victim);
        }
    }

    /** A skilled player is hit. Returns true if the hit was stopped. */
    private static boolean defend(LivingIncomingDamageEvent event, Player pv, Entity attacker, Entity direct) {
        DamageSource src = event.getSource();
        if (SkillEffects.active(pv, "shield_wall") && pv.isBlocking() && !src.is(DamageTypeTags.BYPASSES_SHIELD)) {
            event.setCanceled(true);
            pv.level().playSound(null, pv.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        boolean melee = attacker instanceof LivingEntity && direct == attacker;
        if (melee && Skills.has(pv, "slippery") && pv.getRandom().nextDouble() < Skills.value(pv, "slippery", "chance")) {
            event.setCanceled(true);
            pv.level().playSound(null, pv.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.4F);
            pv.displayClientMessage(net.minecraft.network.chat.Component.literal("Dodged!").withStyle(net.minecraft.ChatFormatting.AQUA), true);
            return true;
        }
        SkillNode armor = Skills.learned(pv, "arcane_armor");
        if (armor != null) {
            float amount = event.getAmount();
            if (IronsBridge.manaLinked()) {
                float part = (float) (amount * armor.value("share", 0.1));
                float cost = (float) (part * armor.value("mana_per_damage", 5));
                float mana = IronsBridge.mana(pv);
                if (mana >= cost && part > 0) {
                    IronsBridge.setMana(pv, mana - cost);
                    event.setAmount(amount - part);
                }
            } else {
                event.setAmount((float) (amount * (1 - armor.value("fallback", 0.05))));
            }
        }
        return false;
    }

    /** Guardian's Aegis: an ally near a guardian passes half the hit to them. */
    private static void aegis(LivingIncomingDamageEvent event, LivingEntity victim) {
        if (inAegis || event.getAmount() <= 0) return;
        SkillNode n = SkillRegistry.node("guardians_aegis");
        double r = n == null ? 6 : n.value("radius", 6);
        for (Player g : victim.level().getEntitiesOfClass(Player.class, victim.getBoundingBox().inflate(r),
                g -> g != victim && g.isAlive() && SkillEffects.active(g, "guardians_aegis") && Factions.relation(g, victim) == Relation.ALLY)) {
            float share = (float) (event.getAmount() * (n == null ? 0.5 : n.value("share", 0.5)));
            event.setAmount(event.getAmount() - share);
            inAegis = true;
            try {
                g.hurt(g.damageSources().magic(), share);
            } finally {
                inAegis = false;
            }
            if (g.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY(0.6), victim.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
            }
            return;
        }
    }

    private static void offense(LivingIncomingDamageEvent event, Player pa, LivingEntity victim, Entity direct) {
        DamageSource src = event.getSource();
        float amount = event.getAmount();
        double mult = 1.0;
        float add = 0;
        boolean melee = direct == pa;
        boolean projectile = direct instanceof Projectile && direct != pa;
        boolean blade = melee && pa.getMainHandItem().is(MELEE_WEAPONS);

        if (blade) {
            mult += Skills.value(pa, "keen_edge", "damage");
            SkillNode exec = Skills.learned(pa, "executioner");
            if (exec != null && victim.getHealth() < victim.getMaxHealth() * exec.value("threshold", 0.3)) mult += exec.value("damage", 0.25);
        }
        if (melee && SkillEffects.consume(pa, "precise_strike")) {
            SkillNode n = SkillRegistry.node("precise_strike");
            mult *= n == null ? 1.5 : n.value("crit", 1.5);
            mult /= armorFactor(victim, (float) (amount * mult));
            if (pa.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 15, 0.3, 0.3, 0.3, 0.3);
        }
        if (melee || projectile) {
            if (Skills.has(pa, "ambush")) {
                boolean hidden = pa.hasEffect(MobEffects.INVISIBILITY);
                if (hidden || behind(pa, victim)) {
                    mult += Skills.value(pa, "ambush", "damage");
                    if (hidden) pa.removeEffect(MobEffects.INVISIBILITY);
                }
            }
            if (Skills.has(pa, "opportunist") && disabled(victim)) mult += Skills.value(pa, "opportunist", "damage");
        }
        if (projectile) {
            mult += Skills.value(pa, "steady_aim", "damage");
            SkillNode hawk = Skills.learned(pa, "hawkeye");
            if (hawk != null && pa.distanceTo(victim) >= hawk.value("distance", 20)) mult += hawk.value("damage", 0.2);
            if (Skills.has(pa, "headhunter") && bigGame(victim)) mult += Skills.value(pa, "headhunter", "damage");
        }
        boolean magic = src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC);
        if (magic && !IronsBridge.loaded()) {
            mult += Skills.value(pa, "attunement", "magic");
            if (SkillEffects.consume(pa, "overcharge")) mult += 0.4;
        }
        if (melee && SkillEffects.active(pa, "spellblade")) {
            add += (float) (Skills.value(pa, "spellblade", "damage") * magicPower(pa));
            if (pa.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY(0.6), victim.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
        }
        if (melee && Skills.has(pa, "mana_strike")) {
            if (IronsBridge.manaLinked()) IronsBridge.setMana(pa, IronsBridge.mana(pa) + (float) Skills.value(pa, "mana_strike", "mana"));
            else Skills.shortenCooldowns(pa, (long) Skills.value(pa, "mana_strike", "ticks"));
        }
        event.setAmount((float) (amount * mult) + add);

        if (!(melee || projectile)) return;
        if (Skills.has(pa, "provoke") && victim instanceof Mob m && SkillActives.hostile(pa, m)) m.setTarget(pa);
        if (Skills.has(pa, "hit_and_run")) {
            pa.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) (Skills.value(pa, "hit_and_run", "seconds") * 20), 0, false, false));
        }
        if (SkillEffects.active(pa, "shadow_dance") && !victim.getUUID().equals(DANCE_TARGET.put(pa.getUUID(), victim.getUUID()))) {
            pa.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false));
        }
        SkillNode mark = Skills.learned(pa, "hunters_mark");
        if (mark != null) {
            int ticks = (int) (mark.value("seconds", 5) * 20);
            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false), pa);
            victim.getPersistentData().putUUID(MARK, pa.getUUID());
            victim.getPersistentData().putLong(MARK_UNTIL, victim.level().getGameTime() + ticks);
        }
    }

    private static void unitOffense(LivingIncomingDamageEvent event, SoldierEntity unit, LivingEntity victim) {
        Player owner = unit.getOwner();
        if (owner == null || !owner.isAlive()) return;
        double mult = 1.0;
        float add = 0;
        SkillNode kindle = SkillRegistry.node("kindle_arms");
        if (SkillEffects.active(owner, "kindle_arms") && unit.distanceTo(owner) <= (kindle == null ? 16 : kindle.value("radius", 16))) {
            Race race = Race.of(owner);
            ServerLevel sl = (ServerLevel) victim.level();
            if (race != null) switch (race) {
                case HUMAN -> {
                    add += 2;
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY(0.6), victim.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
                }
                case ELF -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), owner);
                case DWARF -> {
                    for (LivingEntity next : victim.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(4),
                            e -> e != victim && unit.isEnemy(e))) {
                        next.hurt(unit.damageSources().magic(), 2F);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, next.getX(), next.getY(0.6), next.getZ(), 10, 0.2, 0.4, 0.2, 0.1);
                        break;
                    }
                }
                case ORC -> unit.heal(event.getAmount() * 0.2F);
                case DEMON -> victim.igniteForSeconds(3F);
                case ANGEL -> {
                    Race vr = Race.of(victim);
                    if (victim.getType().is(EntityTypeTags.UNDEAD) || vr == Race.DEMON || vr == Race.HIVE) mult += 0.5;
                }
                case HIVE -> victim.addEffect(new MobEffectInstance(WFRegistry.CORRODED, 100, 0), owner);
            }
        }
        var data = victim.getPersistentData();
        if (data.hasUUID(MARK) && owner.getUUID().equals(data.getUUID(MARK)) && data.getLong(MARK_UNTIL) > victim.level().getGameTime()) {
            mult += Skills.value(owner, "hunters_mark", "damage");
        }
        float amount = (float) (event.getAmount() * mult) + add;
        event.setAmount(amount);
        if (Skills.has(owner, "blood_bond")) owner.heal(amount * (float) Skills.value(owner, "blood_bond", "share"));
    }

    @SubscribeEvent
    public static void onCritical(CriticalHitEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !event.isCriticalHit() || !(event.getTarget() instanceof LivingEntity target)) return;
        SkillNode n = Skills.learned(p, "bleeding_cuts");
        if (n != null && p.getMainHandItem().is(MELEE_WEAPONS)) {
            target.addEffect(new MobEffectInstance(WFRegistry.BLEEDING, (int) (n.value("seconds", 4) * 20) + 1, 0), p);
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player pv) || pv.level().isClientSide || !event.getBlocked()) return;
        SkillNode n = Skills.learned(pv, "steel_thorns");
        DamageSource src = event.getDamageSource();
        if (n != null && src.getEntity() instanceof LivingEntity att && src.getDirectEntity() == att && att != pv) {
            att.hurt(pv.damageSources().thorns(pv), (float) (event.getBlockedDamage() * n.value("reflect", 0.2)));
        }
    }

    // ------------------------------------------------------------------ kills

    /** Last Stand: once in a while, a killing blow leaves you standing. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeathLastStand(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide) return;
        SkillNode n = Skills.learned(p, "last_stand");
        if (n == null || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        SkillState s = Skills.state(p);
        long now = p.level().getGameTime();
        if (s.readyAt.getOrDefault("last_stand", 0L) > now) return;
        event.setCanceled(true);
        p.setHealth(1.0F);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
        s.readyAt.put("last_stand", now + (long) (n.value("cooldown", 120) * 20));
        p.setData(WFRegistry.SKILLS, s);
        p.level().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Last Stand!").withStyle(net.minecraft.ChatFormatting.GOLD), true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        Entity killer = event.getSource().getEntity();
        if (killer instanceof Player p && p != victim) {
            if (!SkillActives.enemyOf(p, victim)) return;
            Skills.gainRenown(p, Renown.forKill(victim));
            SkillNode lust = Skills.learned(p, "bloodlust");
            if (lust != null) {
                MobEffectInstance had = p.getEffect(WFRegistry.BLOODLUST);
                int amp = had == null ? 0 : Math.min((int) lust.value("stacks", 3) - 1, had.getAmplifier() + 1);
                p.addEffect(new MobEffectInstance(WFRegistry.BLOODLUST, (int) (lust.value("seconds", 5) * 20), amp));
            }
            if (Skills.has(p, "battle_hardened")) p.heal((float) Skills.value(p, "battle_hardened", "heal"));
        } else if (killer instanceof SoldierEntity unit && unit.getOwner() instanceof Player owner
                && Factions.relation(unit, victim) == Relation.ENEMY) {
            Skills.gainRenown(owner, Math.max(1, Renown.forKill(victim) / 2));
        }
    }

    // ------------------------------------------------------------------ projectiles and bows

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (arrow.getPersistentData().getBoolean(SkillActives.SEEN) || !(arrow.getOwner() instanceof Player p)) return;
        if (arrow instanceof ThrownTrident) {
            arrow.getPersistentData().putBoolean(SkillActives.SEEN, true);
            if (Skills.has(p, "steady_aim") || SkillEffects.active(p, "piercing_shot")) SkillActives.shot(p, arrow);
            return;
        }
        SkillActives.shot(p, arrow);
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (proj.level().isClientSide || !proj.getPersistentData().getBoolean(SkillActives.PIERCE)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) return;
        if (!(proj.getOwner() instanceof Player p) || !(proj instanceof AbstractArrow arrow)) return;
        event.setCanceled(true);   // keeps flying through
        if (!SkillActives.firstPierce(proj, target) || !SkillActives.hostile(p, target)) return;
        float damage = (float) Math.ceil(Mth.clamp(arrow.getDeltaMovement().length() * arrow.getBaseDamage(), 0.0, Integer.MAX_VALUE));
        target.hurt(p.damageSources().arrow(arrow, p), damage);
    }

    /** Quick Draw: bows draw and crossbows load 20% faster. */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player p) || !isRanged(event.getItem()) || !Skills.has(p, "quick_draw")) return;
        if (p.tickCount % 5 == 0 && event.getDuration() > 1) event.setDuration(event.getDuration() - 1);
    }

    /** Quiet Step: a sneaking player makes no vibrations. */
    @SubscribeEvent
    public static void onVanillaGameEvent(VanillaGameEvent event) {
        if (event.getCause() instanceof Player p && !p.level().isClientSide && p.isCrouching() && Skills.has(p, "quiet_step")) {
            event.setCanceled(true);
        }
    }
}
