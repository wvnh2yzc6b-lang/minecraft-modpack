package com.warfront.tunnel;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Spike Floor: looks like plain floor. When an enemy steps on it, spikes pop up for 6 damage and a short slow, then
 * sink back after 2 seconds. Never hurts the one who set it, their units or allies.
 */
public class SpikeFloorBlock extends TrapBlock {
    public static final MapCodec<SpikeFloorBlock> CODEC = simpleCodec(SpikeFloorBlock::new);
    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
    public static final float DAMAGE = 6F;
    public static final int RESET_TICKS = 40;

    public SpikeFloorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EXTENDED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EXTENDED);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living) trigger(server, pos, living);
        super.stepOn(level, pos, state, entity);
    }

    /** Springs the spikes on {@code target} if it is an enemy and the spikes are down; true if it struck. */
    public static boolean trigger(ServerLevel level, BlockPos pos, LivingEntity target) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SpikeFloorBlock) || state.getValue(EXTENDED)) return false;
        if (!(level.getBlockEntity(pos) instanceof TrapBlockEntity trap) || !trap.isEnemy(level, target)) return false;
        target.hurt(level.damageSources().cactus(), DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        level.setBlock(pos, state.setValue(EXTENDED, true), 3);
        level.scheduleTick(pos, state.getBlock(), RESET_TICKS);
        level.playSound(null, pos, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0,
                pos.getZ() + 0.5, 12, 0.3, 0.05, 0.3, 0.1);
        trap.hit();
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(EXTENDED)) {
            level.setBlock(pos, state.setValue(EXTENDED, false), 3);
            level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.4F, 1.4F);
        }
    }
}
