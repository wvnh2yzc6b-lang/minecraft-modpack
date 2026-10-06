package com.warfront.mana;

import com.warfront.faction.FactionData;
import com.warfront.faction.Factions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** A block that is part of a mana network (a Mana Well or a Mana Pylon). It belongs to whoever placed it. */
public abstract class ManaNodeBlockEntity extends BlockEntity {
    @Nullable private UUID owner;

    protected ManaNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    /** The faction this node serves; only allies of it can draw its mana. */
    public String factionKey(@Nullable MinecraftServer server) {
        if (owner == null) return Factions.WILD;
        return server == null ? "p:" + owner : FactionData.get(server).keyOf(owner);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) ManaNetwork.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) ManaNetwork.remove(level, worldPosition);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) ManaNetwork.remove(level, worldPosition);
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
