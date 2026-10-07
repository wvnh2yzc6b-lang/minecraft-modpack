package com.warfront.fortress;

import com.warfront.faction.NpcFaction;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Keeps track of loaded trophy banners, so the aura pass can find them. */
public class TrophyBannerBlockEntity extends BlockEntity {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED = new HashMap<>();

    public TrophyBannerBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.TROPHY_BANNER_BE.get(), pos, state);
    }

    public NpcFaction faction() {
        return NpcFaction.values()[getBlockState().getValue(TrophyBannerBlock.FACTION) % NpcFaction.values().length];
    }

    public static Set<BlockPos> loaded(Level level) {
        return LOADED.getOrDefault(level.dimension(), Set.of());
    }

    public static void clearAll() {
        LOADED.clear();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) LOADED.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) LOADED.getOrDefault(level.dimension(), new HashSet<>()).remove(worldPosition);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) LOADED.getOrDefault(level.dimension(), new HashSet<>()).remove(worldPosition);
        super.onChunkUnloaded();
    }
}
