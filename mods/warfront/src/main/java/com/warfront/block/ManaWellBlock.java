package com.warfront.block;

import com.mojang.serialization.MapCodec;
import com.warfront.mana.ManaNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Stores mana for nearby towers and altars. Right-click with shards or crystals to fill it (sneak: the whole stack). */
public class ManaWellBlock extends BaseEntityBlock {
    public static final MapCodec<ManaWellBlock> CODEC = simpleCodec(ManaWellBlock::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);

    public ManaWellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaWellBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof ManaWellBlockEntity well) {
            well.setOwner(player.getUUID());
            // Angels' wells drink the sun.
            if (com.warfront.faction.Race.of(player) == com.warfront.faction.Race.ANGEL) well.setSunwell(true);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, com.warfront.registry.WFRegistry.MANA_WELL_BE.get(),
                (l, p, s, well) -> {
                    if (l.getGameTime() % 20 == 0) well.sunTick(l.isDay());
                });
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (ManaWellBlockEntity.fuelValue(stack) <= 0F) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ManaWellBlockEntity well)) return ItemInteractionResult.FAIL;
        int fits = well.fits(stack);
        if (fits == 0) {
            player.displayClientMessage(Component.literal("This Mana Well is full.").withStyle(ChatFormatting.AQUA), true);
            return ItemInteractionResult.FAIL;
        }
        int used = player.isShiftKeyDown() ? fits : 1;
        well.addFuel(stack, used);
        if (!player.getAbilities().instabuild) stack.shrink(used);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.8F, 1.5F);
        describe(well, player);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ManaWellBlockEntity well) describe(well, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void describe(ManaWellBlockEntity well, Player player) {
        float network = ManaNetwork.available(well.getLevel(), well.getBlockPos(), well.factionKey(player.getServer()));
        player.displayClientMessage(Component.literal("Mana Well: " + (int) well.getMana() + " / "
                + (int) ManaWellBlockEntity.capacity() + "  |  Linked network: " + (int) network)
                .withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ManaWellBlockEntity well)) return 0;
        return Math.round(well.getMana() / ManaWellBlockEntity.capacity() * 15F);
    }
}
