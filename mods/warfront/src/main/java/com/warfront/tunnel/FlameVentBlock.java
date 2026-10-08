package com.warfront.tunnel;

import com.mojang.serialization.MapCodec;
import com.warfront.block.TowerBlockEntity;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Flame Vent: set into a tunnel wall on the base's mana network. When an enemy is within 8 blocks in front of it, it
 * draws mana and sprays fire down the tunnel for 3 seconds. No mana, no fire.
 */
public class FlameVentBlock extends TrapBlock {
    public static final MapCodec<FlameVentBlock> CODEC = simpleCodec(FlameVentBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final int REACH = 8;
    public static final int BURST_TICKS = 60;
    public static final float MANA_COST = 3F;

    public FlameVentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    /** The stretch of tunnel the vent covers: 8 blocks out from its face, a block to each side. */
    public static AABB reach(BlockPos pos, Direction facing) {
        BlockPos near = pos.relative(facing);
        BlockPos far = pos.relative(facing, REACH);
        return new AABB(Vec3.atLowerCornerOf(near), Vec3.atLowerCornerOf(far).add(1, 1, 1))
                .inflate(facing.getAxis() == Direction.Axis.X ? 0 : 1, 1, facing.getAxis() == Direction.Axis.Z ? 0 : 1)
                .expandTowards(0, 1, 0);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, WFRegistry.TRAP_BE.get(), FlameVentBlock::serverTick);
    }

    private static void serverTick(Level level, BlockPos pos, BlockState state, TrapBlockEntity vent) {
        if (!(state.getBlock() instanceof FlameVentBlock) || !(level instanceof ServerLevel server)) return;
        if (vent.cooldown > 0) vent.cooldown--;
        Direction facing = state.getValue(FACING);
        if (vent.burst <= 0) {
            if (vent.cooldown > 0 || level.getGameTime() % 10 != 0) return;
            List<LivingEntity> enemies = server.getEntitiesOfClass(LivingEntity.class, reach(pos, facing), e -> vent.isEnemy(server, e));
            if (enemies.isEmpty()) return;
            if (!TowerBlockEntity.power(server, pos, vent.factionKey(server.getServer()), MANA_COST)) {
                vent.cooldown = 40;
                return;
            }
            vent.burst = BURST_TICKS;
            vent.hit();
            server.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        vent.burst--;
        if (vent.burst == 0) vent.cooldown = 20;
        Vec3 mouth = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.6));
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());
        server.sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0, dir.x, dir.y + 0.02, dir.z, 0.6);
        for (int i = 0; i < 3; i++) {
            server.sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0, dir.x + (server.random.nextFloat() - 0.5) * 0.2,
                    (server.random.nextFloat() - 0.5) * 0.15, dir.z + (server.random.nextFloat() - 0.5) * 0.2, 0.5);
        }
        if (vent.burst % 10 == 0) {
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, reach(pos, facing), e -> vent.isEnemy(server, e))) {
                e.igniteForSeconds(4);
                e.hurt(server.damageSources().inFire(), 2F);
            }
        }
    }
}
