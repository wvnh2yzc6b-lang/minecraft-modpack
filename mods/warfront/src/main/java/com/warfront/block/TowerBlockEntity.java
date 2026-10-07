package com.warfront.block;

import com.warfront.config.WFConfig;
import com.warfront.entity.FactionArrow;
import com.warfront.faction.FactionData;
import com.warfront.faction.Factions;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TowerBlockEntity extends BlockEntity {
    @Nullable private UUID owner;
    private int cooldown;
    /** How many times this tower has fired or healed since it loaded. */
    private int actions;
    /** Whether the last check found an enemy to shoot at; for diagnostics. */
    private boolean sawTarget;

    public TowerBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.TOWER_BE.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int actions() {
        return actions;
    }

    public boolean sawTarget() {
        return sawTarget;
    }

    /** The faction this tower fights for. */
    public String factionKey(@Nullable MinecraftServer server) {
        if (owner == null) return Factions.WILD;
        return server == null ? "p:" + owner : FactionData.get(server).keyOf(owner);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TowerBlockEntity tower) {
        if (--tower.cooldown > 0) return;
        if (!(state.getBlock() instanceof TowerBlock block)) return;
        TowerType type = block.getTowerType();
        tower.cooldown = type.interval;
        ServerLevel server = (ServerLevel) level;
        String key = tower.factionKey(server.getServer());
        Vec3 eye = Vec3.atCenterOf(pos).add(0, 1.2, 0);
        int range = WFConfig.TOWER_RANGE.get();
        AABB box = new AABB(pos).inflate(range);

        switch (type) {
            case ARROW -> {
                LivingEntity target = findEnemy(server, key, eye, box, range);
                tower.sawTarget = target != null;
                if (target != null && power(server, pos, key, type)) {
                    shootArrow(server, key, eye, target);
                    tower.actions++;
                }
            }
            case ARCANE -> {
                LivingEntity target = findEnemy(server, key, eye, box, range);
                if (target != null && power(server, pos, key, type)) {
                    summonFangs(server, pos, target);
                    tower.actions++;
                }
            }
            case HEALING -> {
                if (healAllies(server, key, pos, type)) tower.actions++;
            }
        }
    }

    /** Draws this shot's mana from the network; without it the tower sputters and does nothing. */
    private static boolean power(ServerLevel level, BlockPos pos, String key, TowerType type) {
        if (!WFConfig.TOWERS_NEED_MANA.get() || ManaNetwork.draw(level, pos, key, type.manaCost)) return true;
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.6F);
        starving(level, pos, key);
        return false;
    }

    private static final java.util.Map<BlockPos, Long> LAST_WARNING = new java.util.HashMap<>();

    /** Tells the tower's side (within 64 blocks) that the network ran dry; once every five minutes per tower. */
    private static void starving(ServerLevel level, BlockPos pos, String key) {
        long now = level.getGameTime();
        Long last = LAST_WARNING.get(pos);
        if (last != null && now - last < 6000) return;
        LAST_WARNING.put(pos.immutable(), now);
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(pos) < 64 * 64 && com.warfront.faction.Factions.relation(level.getServer(), key,
                    com.warfront.faction.Factions.keyOf(level.getServer(), p)) == com.warfront.faction.Relation.ALLY) {
                com.warfront.alert.Alerts.toast(p, "mana_empty", "Mana running dry", "A tower at " + pos.toShortString()
                        + " has no mana to fire. Fill a Mana Well.");
            }
        }
    }

    @Nullable
    private static LivingEntity findEnemy(ServerLevel level, String key, Vec3 eye, AABB box, int range) {
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!e.isAlive() || e.distanceToSqr(eye) > range * range) return false;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
            String other = Factions.keyOf(level.getServer(), e);
            return Factions.relation(level.getServer(), key, other) == Relation.ENEMY;
        });
        return list.stream()
                .filter(e -> level.clip(new ClipContext(eye, e.getEyePosition(), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(eye)))
                .orElse(null);
    }

    private static void shootArrow(ServerLevel level, String key, Vec3 eye, LivingEntity target) {
        FactionArrow arrow = new FactionArrow(level, eye.x, eye.y, eye.z, new ItemStack(Items.ARROW), key);
        double dx = target.getX() - eye.x;
        double dy = target.getY(0.5) - eye.y;
        double dz = target.getZ() - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + h * 0.12, dz, 2.0F, 2.0F);
        arrow.setBaseDamage(3.0);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        level.addFreshEntity(arrow);
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1.0F, 1.1F);
    }

    private static void summonFangs(ServerLevel level, BlockPos pos, LivingEntity target) {
        float yaw = (float) Mth.atan2(target.getZ() - pos.getZ(), target.getX() - pos.getX());
        for (int i = 0; i < 3; i++) {
            double ox = Mth.cos(yaw + i * 2.094F) * 0.8;
            double oz = Mth.sin(yaw + i * 2.094F) * 0.8;
            level.addFreshEntity(new EvokerFangs(level, target.getX() + ox, target.getY(), target.getZ() + oz,
                    yaw, i * 3, null));
        }
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                20, 0.3, 0.5, 0.3, 0.5);
        level.playSound(null, pos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 0.8F, 1.2F);
    }

    private static boolean healAllies(ServerLevel level, String key, BlockPos pos, TowerType type) {
        AABB box = new AABB(pos).inflate(8);
        List<LivingEntity> wounded = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive()
                && e.getHealth() < e.getMaxHealth()
                && Factions.relation(level.getServer(), key, Factions.keyOf(level.getServer(), e)) == Relation.ALLY);
        if (wounded.isEmpty() || !power(level, pos, key, type)) return false;
        boolean any = false;
        for (LivingEntity e : wounded) {
            e.heal(2.0F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1.0, e.getZ(),
                    4, 0.3, 0.4, 0.3, 0.0);
            any = true;
        }
        if (any) level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6F, 1.4F);
        return any;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
