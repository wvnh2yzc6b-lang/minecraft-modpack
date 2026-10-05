package com.warfront.block;

import com.warfront.config.WFConfig;
import com.warfront.entity.FactionArrow;
import com.warfront.faction.FactionData;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
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

    public TowerBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.TOWER_BE.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
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
                if (target != null) shootArrow(server, key, eye, target);
            }
            case ARCANE -> {
                LivingEntity target = findEnemy(server, key, eye, box, range);
                if (target != null) summonFangs(server, pos, target);
            }
            case HEALING -> healAllies(server, key, pos);
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

    private static void healAllies(ServerLevel level, String key, BlockPos pos) {
        AABB box = new AABB(pos).inflate(8);
        boolean any = false;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e.getHealth() < e.getMaxHealth())) {
            String other = Factions.keyOf(level.getServer(), e);
            if (Factions.relation(level.getServer(), key, other) != Relation.ALLY) continue;
            e.heal(2.0F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1.0, e.getZ(),
                    4, 0.3, 0.4, 0.3, 0.0);
            any = true;
        }
        if (any) level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6F, 1.4F);
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
