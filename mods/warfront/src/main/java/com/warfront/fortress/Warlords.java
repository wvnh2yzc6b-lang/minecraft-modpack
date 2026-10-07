package com.warfront.fortress;

import com.warfront.alert.Alerts;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Relation;
import com.warfront.registry.WFRegistry;
import com.warfront.war.Campaign;
import com.warfront.world.WarbandSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The seven warlords. Until the owner designs them, each is its race's Champion grown huge, in its faction's trim,
 * with a signature attack drawn from the race's power, a boss bar, and three phases: at two thirds and one third
 * health it calls defenders and attacks more often. Beating one marks it in your campaign and drops its trophy
 * banner and its unique gear.
 */
public final class Warlords {
    private static final Map<BlockPos, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<BlockPos, Integer> PHASES = new HashMap<>();
    private static final Map<BlockPos, Long> NEXT_ATTACK = new HashMap<>();
    private static final double FIGHT_RANGE = 64;

    private Warlords() {}

    public static String title(NpcFaction f) {
        return switch (f) {
            case MARAUDERS -> "Gorzak Skullbreaker";
            case BLACK_LEGION -> "Lord-Marshal Varric";
            case BURNING_HORDE -> "Azhrael the Unburnt";
            case THE_SWARM -> "The Brood Mother";
            case SILVERWOOD_REAVERS -> "Thalanor of the Thorns";
            case IRONBEARD_CLAN -> "Thane Brokk Ironbeard";
            case FALLEN_HOST -> "Seraphiel the Fallen";
        };
    }

    /** The warlord rises from the seat. */
    public static SoldierEntity raise(ServerLevel level, FortressCoreBlockEntity core, UUID warband) {
        NpcFaction f = core.faction();
        BlockPos at = core.getBlockPos().offset(0, 0, 2);
        SoldierEntity w = WarbandSpawner.spawnInto(level, warband, f, List.of(SoldierRole.CHAMPION), at,
                Vec3.atBottomCenterOf(core.getBlockPos()), null, 4).get(0);
        w.makeWarlord();
        w.makeCampaignWarlord(title(f));
        w.setPersistenceRequired();
        w.getPersistentData().putLong(FortressCoreBlockEntity.FORTRESS_TAG, core.getBlockPos().asLong());
        level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.8F);
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(at) < FIGHT_RANGE * FIGHT_RANGE) {
                Alerts.banner(p, "warlord", title(f), "Warlord of the " + f.displayName + ". Bring your army.", colorOf(f));
            }
        }
        PHASES.put(core.getBlockPos(), 1);
        return w;
    }

    private static int colorOf(NpcFaction f) {
        return f.color.getColor() == null ? 0xFFFFFF : f.color.getColor();
    }

    @Nullable
    private static SoldierEntity find(ServerLevel level, FortressCoreBlockEntity core) {
        UUID id = core.getWarlord();
        if (id == null) return null;
        Entity e = level.getEntity(id);
        return e instanceof SoldierEntity s && s.isAlive() ? s : null;
    }

    /** Once a second while the fortress is awake: boss bar, phases and the signature attack. */
    public static void tick(ServerLevel level, FortressCoreBlockEntity core) {
        SoldierEntity w = find(level, core);
        BlockPos key = core.getBlockPos();
        if (w == null) {
            clearBar(core);
            return;
        }
        NpcFaction f = core.faction();
        ServerBossEvent bar = BARS.computeIfAbsent(key, k -> new ServerBossEvent(Component.literal(title(f)).withStyle(f.color),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6));
        float frac = w.getHealth() / w.getMaxHealth();
        bar.setProgress(Mth.clamp(frac, 0F, 1F));
        for (ServerPlayer p : level.players()) {
            boolean near = p.distanceToSqr(w) < FIGHT_RANGE * FIGHT_RANGE;
            if (near && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            else if (!near && bar.getPlayers().contains(p)) bar.removePlayer(p);
        }
        int phase = frac > 0.66F ? 1 : frac > 0.33F ? 2 : 3;
        int was = PHASES.getOrDefault(key, 1);
        if (phase > was) {
            PHASES.put(key, phase);
            // A new phase: the warlord calls the garrison to its side.
            List<SoldierRole> call = phase == 2 ? List.of(SoldierRole.SHIELDBEARER, SoldierRole.SWORDSMAN, SoldierRole.ARCHER)
                    : List.of(SoldierRole.CAPTAIN, SoldierRole.SWORDSMAN, SoldierRole.SWORDSMAN, SoldierRole.ARCHER);
            UUID band = core.getWarband();
            if (band != null) {
                for (SoldierEntity s : WarbandSpawner.spawnInto(level, band, f, call, w.blockPosition().offset(3, 0, 3), w.position(), null, 3)) {
                    s.setPersistenceRequired();
                    s.getPersistentData().putLong(FortressCoreBlockEntity.FORTRESS_TAG, key.asLong());
                }
            }
            level.playSound(null, w.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.6F);
            bar.setColor(phase == 3 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW);
        }
        long now = level.getGameTime();
        if (now >= NEXT_ATTACK.getOrDefault(key, 0L)) {
            LivingEntity target = target(level, w);
            if (target != null) {
                signature(level, w, f, target);
                NEXT_ATTACK.put(key, now + (phase == 1 ? 200 : phase == 2 ? 140 : 100));
            }
        }
    }

    /** The nearest enemy of the warlord: a player or a player's soldier. */
    @Nullable
    private static LivingEntity target(ServerLevel level, SoldierEntity w) {
        if (w.getTarget() != null && w.getTarget().isAlive()) return w.getTarget();
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, w.getBoundingBox().inflate(24),
                e -> e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && Factions.relation(w, e) == Relation.ENEMY);
        near.sort((a, b) -> Double.compare(a.distanceToSqr(w), b.distanceToSqr(w)));
        return near.isEmpty() ? null : near.get(0);
    }

    /** Each warlord's signature attack, drawn from its race's power. */
    public static void signature(ServerLevel level, SoldierEntity w, NpcFaction f, LivingEntity target) {
        switch (f.race) {
            case DEMON -> {   // Soul Burst
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, w.getX(), w.getY(0.5), w.getZ(), 60, 2.5, 1, 2.5, 0.05);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, w.getBoundingBox().inflate(5),
                        e -> e != w && Factions.relation(w, e) == Relation.ENEMY)) {
                    e.hurt(level.damageSources().indirectMagic(w, w), 7F);
                    e.igniteForSeconds(4F);
                }
                w.playSound(SoundEvents.WITHER_SHOOT, 1.2F, 0.7F);
            }
            case ORC -> {     // Frenzy
                w.addEffect(new MobEffectInstance(WFRegistry.FRENZY, 160, 0));
                w.playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.0F);
            }
            case ELF -> {     // a volley of long shots
                for (int i = 0; i < 5; i++) {
                    Arrow arrow = new Arrow(EntityType.ARROW, level);
                    arrow.setOwner(w);
                    arrow.setPos(w.getX(), w.getEyeY() - 0.1, w.getZ());
                    double dx = target.getX() - w.getX(), dz = target.getZ() - w.getZ(), dy = target.getY(0.4) - arrow.getY();
                    arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.15, dz, 1.9F, 6F);
                    arrow.setBaseDamage(4.0);
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    level.addFreshEntity(arrow);
                }
                w.playSound(SoundEvents.ARROW_SHOOT, 1.5F, 0.8F);
            }
            case DWARF -> {   // rune lightning
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(target.position());
                    level.addFreshEntity(bolt);
                }
            }
            case ANGEL -> {   // holy smite
                target.hurt(level.damageSources().indirectMagic(w, w), 8F);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 4, target.getZ(), 40, 0.2, 3, 0.2, 0.02);
                w.playSound(SoundEvents.BEACON_POWER_SELECT, 1.5F, 1.4F);
            }
            case HIVE -> {    // burrow ambush: under the ground and up behind the target, with a brood
                Vec3 behind = target.position().subtract(target.getLookAngle().multiply(2.5, 0, 2.5));
                level.sendParticles(ParticleTypes.SCULK_SOUL, w.getX(), w.getY(), w.getZ(), 30, 0.6, 0.3, 0.6, 0.05);
                w.teleportTo(behind.x, target.getY(), behind.z);
                UUID band = w.getWarbandId();
                if (band != null) WarbandSpawner.spawnInto(level, band, NpcFaction.THE_SWARM,
                        List.of(SoldierRole.SWORDSMAN, SoldierRole.SWORDSMAN), w.blockPosition(), target.position(), null, 3);
                w.playSound(SoundEvents.WARDEN_DIG, 1.5F, 1.0F);
            }
            case HUMAN -> {   // shield wall
                for (SoldierEntity s : level.getEntitiesOfClass(SoldierEntity.class, w.getBoundingBox().inflate(10),
                        s -> Factions.relation(w, s) == Relation.ALLY)) {
                    s.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1));
                }
                w.playSound(SoundEvents.SHIELD_BLOCK, 1.5F, 0.7F);
            }
        }
    }

    /** The warlord fell: its trophy banner and gear drop, and the victor's campaign marks it beaten. */
    public static void rewards(ServerLevel level, FortressCoreBlockEntity core, @Nullable ServerPlayer victor) {
        NpcFaction f = core.faction();
        BlockPos at = core.getBlockPos().above();
        net.minecraft.world.level.block.Block.popResource(level, at, TrophyBanners.item(f));
        net.minecraft.world.level.block.Block.popResource(level, at, WarlordGear.item(f));
        net.minecraft.world.level.block.Block.popResource(level, at, new ItemStack(Items.DIAMOND, 4));
        clearBar(core);
        level.playSound(null, at, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (ServerPlayer p : level.players()) {
            if (p != victor && p.blockPosition().distSqr(at) > FIGHT_RANGE * FIGHT_RANGE) continue;
            beaten(p, f);
        }
    }

    /** Marks a warlord beaten for a player; all seven make a Warlord. */
    public static void beaten(Player p, NpcFaction f) {
        Campaign.setWarlordBeaten(p, f, true);
        Alerts.banner(p, "warlord_beaten", title(f) + " has fallen!", "Their trophy banner and warlord gear are yours.", colorOf(f));
        if (Campaign.warlordsBeaten(p) >= NpcFaction.values().length && !p.getData(WFRegistry.WARLORD_TITLE)) {
            p.setData(WFRegistry.WARLORD_TITLE, true);
            p.refreshDisplayName();
            ItemStack seal = new ItemStack(WFRegistry.SEAL_OF_SEVEN.get());
            if (!p.getInventory().add(seal)) p.drop(seal, false);
            Alerts.banner(p, "warlord_title", "WARLORD", "All seven have fallen. The War Maps whisper of one more.", 0xE2B55A);
            p.sendSystemMessage(Component.literal("You are now a Warlord. The seven War Maps fit together into a seal...")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    public static void clearBar(FortressCoreBlockEntity core) {
        ServerBossEvent bar = BARS.remove(core.getBlockPos());
        if (bar != null) bar.removeAllPlayers();
        PHASES.remove(core.getBlockPos());
        NEXT_ATTACK.remove(core.getBlockPos());
    }

    public static void clearAll() {
        BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        BARS.clear();
        PHASES.clear();
        NEXT_ATTACK.clear();
    }

    /** A warlord died: find its fortress and finish the fight. */
    public static void died(SoldierEntity w, @Nullable Entity killer) {
        if (!(w.level() instanceof ServerLevel level) || !w.getPersistentData().contains(FortressCoreBlockEntity.FORTRESS_TAG) || !w.isWarlord()) return;
        BlockPos pos = BlockPos.of(w.getPersistentData().getLong(FortressCoreBlockEntity.FORTRESS_TAG));
        if (!(level.getBlockEntity(pos) instanceof FortressCoreBlockEntity core) || !w.getUUID().equals(core.getWarlord())) return;
        ServerPlayer victor = killer instanceof ServerPlayer p ? p
                : killer instanceof SoldierEntity s && s.getOwner() instanceof ServerPlayer owner ? owner : null;
        core.defeat(level, victor);
    }

    /** Boxes for tests. */
    static AABB around(BlockPos p, double r) {
        return new AABB(p).inflate(r);
    }
}
