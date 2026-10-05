package com.warfront.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A defensive tower that acts on its own: arrow tower, arcane spire or healing shrine. */
public class TowerBlock extends BaseEntityBlock {
    public static final MapCodec<TowerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TowerType.CODEC.fieldOf("tower").forGetter(TowerBlock::getTowerType),
            propertiesCodec()
    ).apply(i, TowerBlock::new));

    private final TowerType type;

    public TowerBlock(TowerType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public TowerType getTowerType() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TowerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TowerBlockEntity tower) {
            tower.setOwner(player.getUUID());
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, WFRegistry.TOWER_BE.get(), TowerBlockEntity::serverTick);
    }
}
