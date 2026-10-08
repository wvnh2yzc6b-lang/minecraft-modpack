package com.warfront.tunnel;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rune Mine: hidden in the floor, goes off under an enemy only: heavy damage to enemies in a small radius, a blast
 * that breaks no blocks, and the mine is spent.
 */
public class RuneMineBlock extends TrapBlock {
    public static final MapCodec<RuneMineBlock> CODEC = simpleCodec(RuneMineBlock::new);
    public static final float DAMAGE = 16F;
    public static final double RADIUS = 3.5;

    public RuneMineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living) trigger(server, pos, living);
        super.stepOn(level, pos, state, entity);
    }

    /** Detonates under {@code target} if it is an enemy; true if it went off (the mine is then gone). */
    public static boolean trigger(ServerLevel level, BlockPos pos, LivingEntity target) {
        if (!(level.getBlockState(pos).getBlock() instanceof RuneMineBlock)) return false;
        if (!(level.getBlockEntity(pos) instanceof TrapBlockEntity trap) || !trap.isEnemy(level, target)) return false;
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS), e -> trap.isEnemy(level, e))) {
            double d = e.position().distanceTo(c);
            if (d > RADIUS + 0.5) continue;
            e.hurt(level.damageSources().explosion(null, null), (float) (DAMAGE * (1.0 - 0.5 * Math.min(1.0, d / RADIUS))));
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) e.knockback(0.8, -away.x, -away.z);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ENCHANT, c.x, c.y, c.z, 40, 1.2, 0.6, 1.2, 0.5);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.2F, 0.9F);
        trap.hit();
        // Spent: the floor block goes, nothing else is touched.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return true;
    }
}
