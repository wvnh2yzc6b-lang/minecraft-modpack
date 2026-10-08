package com.warfront.racetower;

import com.warfront.army.SoldierRole;
import com.warfront.block.TowerBlockEntity;
import com.warfront.combat.Radiance;
import com.warfront.combat.Rage;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.FactionData;
import com.warfront.faction.Factions;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.mana.ManaNetwork;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** A race tower's state: its owner, cooldown, whether it can pay for a shot, and counters for tests and the codex. */
public class RaceTowerBlockEntity extends BlockEntity {
    @Nullable private UUID owner;
    private int cooldown;
    private int actions;
    private int charges;
    private boolean powered = true;
    private boolean revealed;
    /** Stone Warden: how much more punishment it can take. Watchtower Bell: whether a raid is already in range. */
    public static final int WARDEN_HEALTH = 200;
    private int health = WARDEN_HEALTH;
    private boolean alert;
    /** Capstones that act at each siege wave: the last wave they answered. */
    private int lastWave;

    public RaceTowerBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.RACE_TOWER_BE.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int actions() {
        return actions;
    }

    public int charges() {
        return charges;
    }

    public boolean isPowered() {
        return powered;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public int health() {
        return health;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public RaceTowerType type() {
        return getBlockState().getBlock() instanceof RaceTowerBlock b ? b.type() : RaceTowerType.BALLISTA;
    }

    public String factionKey(@Nullable MinecraftServer server) {
        if (owner == null) return Factions.WILD;
        return server == null ? "p:" + owner : FactionData.get(server).keyOf(owner);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) RaceTowers.track(level, worldPosition, true);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) RaceTowers.track(level, worldPosition, false);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) RaceTowers.track(level, worldPosition, false);
        super.onChunkUnloaded();
    }

    private void setPowered(boolean on) {
        if (powered == on) return;
        powered = on;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** A kill near a Soul Pyre charges it. */
    void charge() {
        charges++;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RaceTowerBlockEntity t) {
        if (--t.cooldown > 0 || !(level instanceof ServerLevel server)) return;
        RaceTowerType type = t.type();
        t.cooldown = type.interval;
        String key = t.factionKey(server.getServer());
        t.setPowered(!com.warfront.config.WFConfig.TOWERS_NEED_MANA.get() || ManaNetwork.available(server, pos, key) >= type.manaCost);
        Vec3 eye = Vec3.atCenterOf(pos).add(0, 1.4, 0);
        boolean acted = switch (type) {
            case BALLISTA -> ballista(server, pos, key, eye, type);
            case THORNWOOD_SENTINEL -> sentinel(server, pos, key, type);
            case RUNE_CANNON -> cannon(server, pos, key, eye, type);
            case WAR_DRUM_TOTEM -> drum(server, pos, key, type);
            case SOUL_PYRE -> pyre(server, pos, key, type, t);
            case SUN_LANCE -> lance(server, pos, key, eye, type);
            case LURKER_PIT -> lurker(server, pos, key, type, t);
            case WATCHTOWER_BELL -> bell(server, pos, key, type, t);
            case MOONWELL_GROVE -> moonwell(server, pos, key, type, t);
            case STONE_WARDEN -> warden(server, pos, key, type, t);
            case GOBLIN_CATAPULT -> catapult(server, pos, key, eye, type);
            case BRIMSTONE_CHAINS -> chains(server, pos, key, type);
            case CHOIR_BELL -> choir(server, pos, key, type);
            case BROOD_NEST -> nest(server, pos, key, type, t);
            case TREBUCHET -> trebuchet(server, pos, key, eye, type);
            case ELDER_TREANT_SPIRE -> treant(server, pos, key, type, t);
            case THUNDER_FORGE -> forge(server, pos, key, eye, type);
            case WAAAGH_BANNER -> waaagh(server, pos, key, type, t);
            case HELLGATE -> hellgate(server, pos, key, type, t);
            case SERAPHIC_OBELISK -> obelisk(server, pos, key, type);
            case SYNAPSE_SPIRE -> synapse(server, pos, key, type);
        };
        if (acted) {
            t.actions++;
            t.setChanged();
        }
    }

    // ------------------------------------------------------------------ targeting

    static float damage(float base) {
        return (float) (base * com.warfront.config.WFConfig.RACE_TOWER_DAMAGE.get());
    }

    static List<LivingEntity> enemies(ServerLevel level, String key, BlockPos pos, int baseReach) {
        int reach = (int) Math.round(baseReach * RaceTowers.rangeBonus(level, pos, key));
        Vec3 c = Vec3.atCenterOf(pos);
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(reach),
                e -> e.distanceToSqr(c) <= reach * reach && TowerBlockEntity.isEnemy(level, key, e));
    }

    @Nullable
    static LivingEntity nearestSeen(ServerLevel level, String key, BlockPos pos, Vec3 eye, int baseReach) {
        int reach = (int) Math.round(baseReach * RaceTowers.rangeBonus(level, pos, key));
        return enemies(level, key, pos, reach).stream()
                .filter(e -> level.clip(new ClipContext(eye, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e))
                        .getType() == HitResult.Type.MISS)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(eye))).orElse(null);
    }

    private static boolean pay(ServerLevel level, BlockPos pos, String key, RaceTowerType type) {
        return TowerBlockEntity.power(level, pos, key, type.manaCost);
    }

    // ------------------------------------------------------------------ the seven level 2 towers

    /** A bolt along a line: every enemy within a block of it takes the hit; beasts and big foes take half again. */
    private static boolean ballista(ServerLevel level, BlockPos pos, String key, Vec3 eye, RaceTowerType type) {
        LivingEntity target = nearestSeen(level, key, pos, eye, type.reach);
        if (target == null || !pay(level, pos, key, type)) return false;
        Vec3 dir = target.getBoundingBox().getCenter().subtract(eye).normalize();
        for (LivingEntity e : enemies(level, key, pos, type.reach)) {
            Vec3 c = e.getBoundingBox().getCenter();
            double along = c.subtract(eye).dot(dir);
            if (along < 0 || c.distanceTo(eye.add(dir.scale(along))) > 1.0 + e.getBbWidth() / 2) continue;
            boolean big = e.getBbWidth() > 1.2F || e instanceof SoldierEntity s && s.getRole() == SoldierRole.BEAST;
            e.hurt(level.damageSources().generic(), damage(big ? 12F : 8F));
        }
        for (double d = 0; d < type.reach; d += 0.75) {
            Vec3 p = eye.add(dir.scale(d));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, pos, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 1.2F, 0.6F);
        return true;
    }

    /** Vines root the three nearest enemies and outline them. */
    private static boolean sentinel(ServerLevel level, BlockPos pos, String key, RaceTowerType type) {
        Vec3 c = Vec3.atCenterOf(pos);
        List<LivingEntity> near = enemies(level, key, pos, type.reach).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(c))).limit(3).toList();
        if (near.isEmpty() || !pay(level, pos, key, type)) return false;
        for (LivingEntity e : near) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
            e.hurtMarked = true;
            level.sendParticles(ParticleTypes.COMPOSTER, e.getX(), e.getY() + 0.3, e.getZ(), 14, 0.4, 0.3, 0.4, 0.02);
        }
        level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        return true;
    }

    /** A heavy shot that bursts where it lands: splash damage, no block damage. */
    private static boolean cannon(ServerLevel level, BlockPos pos, String key, Vec3 eye, RaceTowerType type) {
        LivingEntity target = nearestSeen(level, key, pos, eye, type.reach);
        if (target == null || !pay(level, pos, key, type)) return false;
        Vec3 at = target.position();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3),
                e -> e.position().distanceTo(at) <= 3 && TowerBlockEntity.isEnemy(level, key, e))) {
            e.hurt(level.damageSources().explosion(null, null), damage(e == target ? 10F : 6F));
        }
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.5, 0.3, 0.5, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, eye.x, eye.y, eye.z, 8, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.9F, 0.8F);
        return true;
    }

    /** A drum beat: orcs nearby gain Rage; enemies close by may break and run. */
    private static boolean drum(ServerLevel level, BlockPos pos, String key, RaceTowerType type) {
        Vec3 c = Vec3.atCenterOf(pos);
        MinecraftServer server = level.getServer();
        List<LivingEntity> orcs = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(type.reach),
                e -> e.isAlive() && e.distanceToSqr(c) <= type.reach * type.reach && Race.of(e) == Race.ORC
                        && Factions.relation(server, key, Factions.keyOf(server, e)) == Relation.ALLY);
        List<LivingEntity> foes = enemies(level, key, pos, 8);
        if (orcs.isEmpty() && foes.isEmpty() || !pay(level, pos, key, type)) return false;
        for (LivingEntity o : orcs) Rage.gain(o, 5);
        for (LivingEntity f : foes) {
            if (level.random.nextFloat() >= 0.25F) continue;
            if (f instanceof SoldierEntity s) s.routFor(40);
            Vec3 away = f.position().subtract(c);
            f.knockback(0.6, -away.x, -away.z);
            f.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.NOTE, c.x, c.y + 1.2, c.z, 3, 0.3, 0.2, 0.3, 0.5);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS, 1.6F, 0.6F);
        return true;
    }

    /** Sets the nearest enemy alight; at 10 charges from nearby kills, a burst of fire. */
    private static boolean pyre(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        Vec3 c = Vec3.atCenterOf(pos);
        List<LivingEntity> foes = enemies(level, key, pos, type.reach);
        if (foes.isEmpty() || !pay(level, pos, key, type)) return false;
        if (t.charges >= 10) {
            t.charges = 0;
            for (LivingEntity f : enemies(level, key, pos, 8)) {
                f.igniteForSeconds(6);
                f.hurt(level.damageSources().inFire(), damage(6F));
            }
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 80, 3, 0.5, 3, 0.05);
            level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.5F, 0.5F);
            return true;
        }
        LivingEntity target = foes.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).get();
        target.igniteForSeconds(4);
        target.hurt(level.damageSources().inFire(), damage(2F));
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
        return true;
    }

    /** A beam of light: 8 by day under open sky, 3 otherwise; half again to demons, the Hive and the undead. */
    private static boolean lance(ServerLevel level, BlockPos pos, String key, Vec3 eye, RaceTowerType type) {
        LivingEntity target = nearestSeen(level, key, pos, eye, type.reach);
        if (target == null || !pay(level, pos, key, type)) return false;
        float dmg = sunlit(level, pos) ? 8F : 3F;
        if (Radiance.smiteBonus(target)) dmg *= 1.5F;
        target.hurt(level.damageSources().magic(), damage(dmg));
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 step = to.subtract(eye).normalize().scale(0.5);
        for (Vec3 p = eye; p.distanceTo(to) > 0.5; p = p.add(step)) level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.6F);
        return true;
    }

    public static boolean sunlit(Level level, BlockPos pos) {
        return level.isDay() && level.canSeeSky(pos.above()) && !level.isRainingAt(pos.above());
    }

    /** Hidden until an enemy comes within 4 blocks: then acid that eats armor; stronger underground. */
    private static boolean lurker(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        List<LivingEntity> foes = enemies(level, key, pos, type.reach);
        boolean show = !foes.isEmpty();
        if (show != t.revealed) {
            t.revealed = show;
            t.sync();
        }
        if (!show || !pay(level, pos, key, type)) return false;
        boolean deep = level.getBrightness(LightLayer.SKY, pos.above()) == 0;
        for (LivingEntity f : foes) {
            f.addEffect(new MobEffectInstance(WFRegistry.CORRODED, 100, deep ? 1 : 0));
            f.hurt(level.damageSources().magic(), damage(deep ? 6F : 4F));
            level.sendParticles(ParticleTypes.ITEM_SLIME, f.getX(), f.getY() + 0.5, f.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
        return true;
    }

    // ------------------------------------------------------------------ the seven level 3 towers

    static List<LivingEntity> allies(ServerLevel level, String key, BlockPos pos, int reach) {
        Vec3 c = Vec3.atCenterOf(pos);
        MinecraftServer server = level.getServer();
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(reach),
                e -> e.isAlive() && e.distanceToSqr(c) <= reach * reach
                        && Factions.relation(server, key, Factions.keyOf(server, e)) == Relation.ALLY);
    }

    /** Reveals the hidden (invisible) and the tunnelers (the Swarm) in range; rings once when a raid comes into range. */
    private static boolean bell(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        List<LivingEntity> foes = enemies(level, key, pos, type.reach);
        if (foes.isEmpty()) {
            t.alert = false;
            return false;
        }
        if (!pay(level, pos, key, type)) return false;
        for (LivingEntity f : foes) {
            boolean tunneler = f instanceof SoldierEntity s
                    && com.warfront.faction.NpcFaction.byKey(s.getFactionKey()) == com.warfront.faction.NpcFaction.THE_SWARM;
            if (f.isInvisible() || tunneler) f.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
        }
        if (!t.alert) {
            t.alert = true;
            level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.8F);
            level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.5F, 1.0F);
        }
        return true;
    }

    /** Heals elf units and the owner nearby, two health a pulse. */
    private static boolean moonwell(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        List<LivingEntity> hurt = allies(level, key, pos, type.reach).stream()
                .filter(e -> e.getHealth() < e.getMaxHealth() && (Race.of(e) == Race.ELF || e.getUUID().equals(t.owner))).toList();
        if (hurt.isEmpty() || !pay(level, pos, key, type)) return false;
        for (LivingEntity e : hurt) {
            e.heal(2F);
            level.sendParticles(ParticleTypes.GLOW, e.getX(), e.getY() + 1, e.getZ(), 5, 0.3, 0.4, 0.3, 0.01);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.4F);
        return true;
    }

    /** Taunts enemies into attacking it; those close enough wear it down, and at 0 it crumbles. */
    private static boolean warden(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        List<LivingEntity> foes = enemies(level, key, pos, type.reach);
        if (foes.isEmpty() || !pay(level, pos, key, type)) return false;
        Vec3 c = Vec3.atBottomCenterOf(pos.above());
        for (LivingEntity f : foes) {
            if (f instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().moveTo(c.x, c.y, c.z, 1.0);
            }
            if (f.distanceToSqr(c) < 2.6 * 2.6) {
                var attack = f.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                t.health -= (int) Math.max(1, attack == null ? 1 : attack.getValue());
                f.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0F, 0.7F);
            }
        }
        if (level.getGameTime() % 40 == 0) level.playSound(null, pos, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 0.6F, 0.6F);
        if (t.health <= 0) {
            level.destroyBlock(pos, false);
            return true;
        }
        t.setChanged();
        return true;
    }

    /** A bomb lobbed in an arc at the nearest enemy at least 4 blocks off; it bursts on landing, no block damage. */
    private static boolean catapult(ServerLevel level, BlockPos pos, String key, Vec3 eye, RaceTowerType type) {
        LivingEntity target = enemies(level, key, pos, type.reach).stream().filter(e -> e.distanceToSqr(eye) > 16)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(eye))).orElse(null);
        if (target == null || !pay(level, pos, key, type)) return false;
        Vec3 at = target.position();
        for (int i = 0; i <= 16; i++) {
            double f = i / 16.0;
            Vec3 p = eye.lerp(at, f).add(0, Math.sin(f * Math.PI) * 5, 0);
            level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3),
                e -> e.position().distanceTo(at) <= 3 && TowerBlockEntity.isEnemy(level, key, e))) {
            e.hurt(level.damageSources().explosion(null, null), damage(8F));
        }
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, 0.8, 0.3, 0.8, 0);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 1.1F);
        level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.5F);
        return true;
    }

    /** Hooks the nearest enemy, drags it toward the tower and holds it fast for three seconds. */
    private static boolean chains(ServerLevel level, BlockPos pos, String key, RaceTowerType type) {
        Vec3 c = Vec3.atCenterOf(pos).add(0, 1, 0);
        LivingEntity target = enemies(level, key, pos, type.reach).stream().filter(e -> e.distanceToSqr(c) > 6.25)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
        if (target == null || !pay(level, pos, key, type)) return false;
        Vec3 pull = c.subtract(target.position()).normalize().scale(1.4);
        target.setDeltaMovement(pull.x, 0.35, pull.z);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
        target.hurt(level.damageSources().inFire(), damage(2F));
        Vec3 from = target.getBoundingBox().getCenter();
        for (int i = 0; i <= 12; i++) {
            Vec3 p = from.lerp(c, i / 12.0);
            level.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.5F, 0.6F);
        return true;
    }

    /** Shields friends nearby with absorption and lifts their harmful effects. */
    private static boolean choir(ServerLevel level, BlockPos pos, String key, RaceTowerType type) {
        List<LivingEntity> friends = allies(level, key, pos, type.reach);
        if (friends.isEmpty() || !pay(level, pos, key, type)) return false;
        for (LivingEntity e : friends) {
            e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = e.getActiveEffects().stream()
                    .map(MobEffectInstance::getEffect)
                    .filter(h -> h.value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL).toList();
            bad.forEach(e::removeEffect);
            level.sendParticles(ParticleTypes.NOTE, e.getX(), e.getY() + 2, e.getZ(), 1, 0, 0, 0, 0.5);
        }
        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.2F, 1.6F);
        return true;
    }

    /** Hatches a swarmling (a Ripper that lasts 30 seconds) while enemies are near, three at most at a time. */
    private static boolean nest(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        if (t.owner == null || enemies(level, key, pos, type.reach).isEmpty()) return false;
        if (com.warfront.world.Summons.countFrom(level, pos, 48) >= 3 || !pay(level, pos, key, type)) return false;
        com.warfront.world.Summons.summon(level, t.owner, Race.HIVE, SoldierRole.SWORDSMAN, Vec3.atBottomCenterOf(pos.above()),
                600, null, pos);
        level.playSound(null, pos, SoundEvents.FROGSPAWN_HATCH, SoundSource.BLOCKS, 1.2F, 0.7F);
        return true;
    }

    // ------------------------------------------------------------------ the seven capstones (level 5)

    /** A boulder at the nearest enemy 6+ blocks off, or failing that at an enemy outpost's raid chest in range. */
    private static boolean trebuchet(ServerLevel level, BlockPos pos, String key, Vec3 eye, RaceTowerType type) {
        LivingEntity target = enemies(level, key, pos, type.reach).stream().filter(e -> e.distanceToSqr(eye) > 36)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(eye))).orElse(null);
        com.warfront.outpost.RaidChestBlockEntity outpost = target == null ? RaceTowers.enemyOutpost(level, pos, key, type.reach) : null;
        if (target == null && outpost == null || !pay(level, pos, key, type)) return false;
        Vec3 at = target != null ? target.position() : Vec3.atCenterOf(outpost.getBlockPos());
        arc(level, eye, at, 10);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3),
                e -> e.position().distanceTo(at) <= 3 && TowerBlockEntity.isEnemy(level, key, e))) {
            e.hurt(level.damageSources().explosion(null, null), damage(14F));
        }
        if (outpost != null) outpost.bombard(level, at, 15);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5F, 0.7F);
        level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.5F, 0.4F);
        return true;
    }

    private static void arc(ServerLevel level, Vec3 from, Vec3 to, double height) {
        for (int i = 0; i <= 24; i++) {
            double f = i / 24.0;
            Vec3 p = from.lerp(to, f).add(0, Math.sin(f * Math.PI) * height, 0);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** True once per new siege wave at an allied War Standard within reach. */
    private static boolean newWave(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        com.warfront.block.WarStandardBlockEntity standard = RaceTowers.siegeStandard(level, pos, key, type.reach);
        if (standard == null || standard.getWave() == t.lastWave) return false;
        t.lastWave = standard.getWave();
        t.setChanged();
        return true;
    }

    /** At each wave: a treant guardian (a hulking elf Shieldbearer) wakes and fights until the wave ends. */
    private static boolean treant(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        if (t.owner == null || !newWave(level, pos, key, type, t) || !pay(level, pos, key, type)) return false;
        com.warfront.block.WarStandardBlockEntity standard = RaceTowers.siegeStandard(level, pos, key, type.reach);
        SoldierEntity treant = com.warfront.world.Summons.summon(level, t.owner, Race.ELF, SoldierRole.SHIELDBEARER,
                Vec3.atBottomCenterOf(pos.above()), 0, standard == null ? pos : standard.getBlockPos(), pos);
        if (treant != null) {
            treant.setCustomName(net.minecraft.network.chat.Component.literal("Elder Treant"));
            var hp = treant.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (hp != null) hp.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    com.warfront.Warfront.id("treant_health"), 3.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            var scale = treant.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
            if (scale != null) scale.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    com.warfront.Warfront.id("treant_scale"), 0.6, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            treant.setHealth(treant.getMaxHealth());
        }
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.5F, 0.5F);
        return true;
    }

    /** Chain lightning: the nearest enemy, then up to four more, each within 6 blocks of the last. */
    private static boolean forge(ServerLevel level, BlockPos pos, String key, Vec3 eye, RaceTowerType type) {
        LivingEntity first = nearestSeen(level, key, pos, eye, type.reach);
        if (first == null || !pay(level, pos, key, type)) return false;
        List<LivingEntity> hit = new java.util.ArrayList<>();
        LivingEntity cur = first;
        Vec3 from = eye;
        while (cur != null && hit.size() < 5) {
            hit.add(cur);
            Vec3 to = cur.getBoundingBox().getCenter();
            for (int i = 0; i <= 10; i++) {
                Vec3 p = from.lerp(to, i / 10.0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
            }
            cur.hurt(level.damageSources().lightningBolt(), damage(8F));
            from = to;
            LivingEntity last = cur;
            cur = level.getEntitiesOfClass(LivingEntity.class, last.getBoundingBox().inflate(6),
                            e -> !hit.contains(e) && TowerBlockEntity.isEnemy(level, key, e))
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(last))).orElse(null);
        }
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 1.0F, 1.4F);
        return true;
    }

    /** At each wave: every orc unit within 16 blocks goes into a Frenzy. */
    private static boolean waaagh(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        if (!newWave(level, pos, key, type, t)) return false;
        return frenzy(level, pos, key, type);
    }

    static boolean frenzy(ServerLevel level, BlockPos pos, String key, RaceTowerType type) {
        List<LivingEntity> orcs = allies(level, key, pos, type.reach).stream()
                .filter(e -> e instanceof SoldierEntity && Race.of(e) == Race.ORC).toList();
        if (orcs.isEmpty() || !pay(level, pos, key, type)) return false;
        for (LivingEntity o : orcs) o.addEffect(new MobEffectInstance(WFRegistry.FRENZY, Rage.FRENZY_TICKS, 0));
        level.playSound(null, pos, SoundEvents.RAVAGER_ROAR, SoundSource.BLOCKS, 2.0F, 0.8F);
        return true;
    }

    /** At each wave: two Imp Impalers and two Imp Firecasters, gone when the wave ends. */
    private static boolean hellgate(ServerLevel level, BlockPos pos, String key, RaceTowerType type, RaceTowerBlockEntity t) {
        if (t.owner == null || !newWave(level, pos, key, type, t) || !pay(level, pos, key, type)) return false;
        com.warfront.block.WarStandardBlockEntity standard = RaceTowers.siegeStandard(level, pos, key, type.reach);
        BlockPos waveKey = standard == null ? pos : standard.getBlockPos();
        SoldierRole[] roles = {SoldierRole.SPEARMAN, SoldierRole.SPEARMAN, SoldierRole.ARCHER, SoldierRole.ARCHER};
        for (int i = 0; i < roles.length; i++) {
            double a = i * Math.PI / 2;
            com.warfront.world.Summons.summon(level, t.owner, Race.DEMON, roles[i],
                    Vec3.atBottomCenterOf(pos.above()).add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5), 0, waveKey, pos);
        }
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 40, 0.8, 0.8, 0.8, 0.05);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6F, 1.6F);
        return true;
    }

    /** A pillar of light on the strongest enemy in range: 20 damage, half again to the unholy. */
    private static boolean obelisk(ServerLevel level, BlockPos pos, String key, RaceTowerType type) {
        LivingEntity target = enemies(level, key, pos, type.reach).stream()
                .max(Comparator.comparingDouble(LivingEntity::getMaxHealth)).orElse(null);
        if (target == null || !pay(level, pos, key, type)) return false;
        float dmg = 20F * (Radiance.smiteBonus(target) ? 1.5F : 1F);
        target.hurt(level.damageSources().magic(), damage(dmg));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        for (int y = 0; y < 24; y++) level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + y, target.getZ(), 3, 0.2, 0.4, 0.2, 0);
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 1.0F, 1.6F);
        return true;
    }

    /** Confusion: each enemy mob in range turns on another enemy near it for a moment; players reel. */
    private static boolean synapse(ServerLevel level, BlockPos pos, String key, RaceTowerType type) {
        List<LivingEntity> foes = enemies(level, key, pos, type.reach);
        if (foes.size() < 1 || !pay(level, pos, key, type)) return false;
        for (LivingEntity f : foes) {
            if (f instanceof net.minecraft.world.entity.Mob mob) {
                LivingEntity other = foes.stream().filter(o -> o != f && o.distanceToSqr(f) < 64)
                        .min(Comparator.comparingDouble(o -> o.distanceToSqr(f))).orElse(null);
                if (other != null) mob.setTarget(other);
            }
            f.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            level.sendParticles(ParticleTypes.SCULK_SOUL, f.getX(), f.getY() + 1.8, f.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
        }
        level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1.0F, 1.4F);
        return true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Revealed", revealed);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Charges", charges);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Revealed", revealed);
        tag.putInt("Health", health);
        tag.putInt("LastWave", lastWave);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        charges = tag.getInt("Charges");
        powered = !tag.contains("Powered") || tag.getBoolean("Powered");
        revealed = tag.getBoolean("Revealed");
        health = tag.contains("Health") ? tag.getInt("Health") : WARDEN_HEALTH;
        lastWave = tag.getInt("LastWave");
    }
}
