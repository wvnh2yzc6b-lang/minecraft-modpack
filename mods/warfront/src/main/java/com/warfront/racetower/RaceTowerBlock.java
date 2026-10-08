package com.warfront.racetower;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A race tower block. Placing one checks race, base level and the race-tower cap first. */
public class RaceTowerBlock extends BaseEntityBlock {
    public static final MapCodec<RaceTowerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RaceTowerType.CODEC.fieldOf("tower").forGetter(RaceTowerBlock::type),
            propertiesCodec()
    ).apply(i, RaceTowerBlock::new));

    private final RaceTowerType type;

    public RaceTowerBlock(RaceTowerType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public RaceTowerType type() {
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
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null && !player.getAbilities().instabuild) {
            String refusal = RaceTowers.refusal(ctx.getLevel(), ctx.getClickedPos(), player, type);
            if (refusal != null) {
                if (!ctx.getLevel().isClientSide) player.displayClientMessage(Component.literal(refusal).withStyle(ChatFormatting.RED), true);
                return null;
            }
        }
        return super.getStateForPlacement(ctx);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RaceTowerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof RaceTowerBlockEntity tower) {
            tower.setOwner(player.getUUID());
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, WFRegistry.RACE_TOWER_BE.get(), RaceTowerBlockEntity::serverTick);
    }
}
