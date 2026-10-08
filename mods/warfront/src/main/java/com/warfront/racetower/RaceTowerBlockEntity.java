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

    static List<LivingEntity> enemies(ServerLevel level, String key, BlockPos pos, int reach) {
        Vec3 c = Vec3.atCenterOf(pos);
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(reach),
                e -> e.distanceToSqr(c) <= reach * reach && TowerBlockEntity.isEnemy(level, key, e));
    }

    @Nullable
    static LivingEntity nearestSeen(ServerLevel level, String key, BlockPos pos, Vec3 eye, int reach) {
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
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 3, 0).inflate(1, 0, 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Charges", charges);
        tag.putBoolean("Powered", powered);
        tag.putBoolean("Revealed", revealed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        charges = tag.getInt("Charges");
        powered = !tag.contains("Powered") || tag.getBoolean("Powered");
        revealed = tag.getBoolean("Revealed");
    }
}
