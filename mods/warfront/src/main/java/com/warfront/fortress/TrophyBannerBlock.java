package com.warfront.fortress;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A beaten warlord's banner. Planted within 8 blocks of your War Standard, it gives your units an aura. */
public class TrophyBannerBlock extends BaseEntityBlock {
    public static final MapCodec<TrophyBannerBlock> CODEC = simpleCodec(TrophyBannerBlock::new);
    public static final IntegerProperty FACTION = IntegerProperty.create("faction", 0, 6);
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public TrophyBannerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACTION, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACTION);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyBannerBlockEntity(pos, state);
    }
}
