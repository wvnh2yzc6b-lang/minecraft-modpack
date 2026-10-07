package com.warfront.upkeep;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The Mess Hall: a big food store your battle units eat from each morning. Right-click to open it. */
public class MessHallBlock extends BaseEntityBlock {
    public static final MapCodec<MessHallBlock> CODEC = simpleCodec(MessHallBlock::new);

    public MessHallBlock(Properties properties) {
        super(properties);
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
        return new MessHallBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof MessHallBlockEntity hall) {
            hall.setOwner(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MessHallBlockEntity hall) {
            player.openMenu(hall);
            int troops = hall.troopsInRange();
            double days = hall.daysOfFood(troops);
            player.displayClientMessage(Component.literal("Mess Hall: " + hall.foodPoints() + " food, "
                    + (troops == 0 ? "no troops in range" : String.format("%.1f days for %d troops", days, troops)))
                    .withStyle(days < 1 && troops > 0 ? ChatFormatting.RED : ChatFormatting.GOLD), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MessHallBlockEntity hall) {
            Containers.dropContents(level, pos, hall);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
