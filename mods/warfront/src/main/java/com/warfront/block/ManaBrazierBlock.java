package com.warfront.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A glowing crystal brazier. Four of them mark out a summoning altar. */
public class ManaBrazierBlock extends Block {
    public static final MapCodec<ManaBrazierBlock> CODEC = simpleCodec(ManaBrazierBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

    public ManaBrazierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5 + random.nextGaussian() * 0.12,
                    pos.getY() + 0.95, pos.getZ() + 0.5 + random.nextGaussian() * 0.12, 0, 0.02, 0);
        }
    }
}
