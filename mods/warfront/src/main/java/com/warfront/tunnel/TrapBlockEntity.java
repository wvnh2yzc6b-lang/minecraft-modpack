package com.warfront.tunnel;

import com.warfront.block.TowerBlockEntity;
import com.warfront.faction.FactionData;
import com.warfront.faction.Factions;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** A dwarf tunnel trap (spike floor, rune mine, flame vent): who set it, and how often it has gone off. */
public class TrapBlockEntity extends BlockEntity {
    @Nullable private UUID owner;
    /** Times this trap has struck since it loaded; for tests and the Test Panel. */
    private int hits;
    /** Flame vent: ticks left in the current burst, and ticks until the next check. */
    int burst;
    int cooldown;

    public TrapBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.TRAP_BE.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int hits() {
        return hits;
    }

    void hit() {
        hits++;
    }

    public String factionKey(@Nullable MinecraftServer server) {
        if (owner == null) return Factions.WILD;
        return server == null ? "p:" + owner : FactionData.get(server).keyOf(owner);
    }

    /** The tower friend-or-foe rule; a trap nobody owns only goes off on monsters. */
    public boolean isEnemy(ServerLevel level, LivingEntity e) {
        if (owner == null) return e.isAlive() && e instanceof Enemy;
        return TowerBlockEntity.isEnemy(level, factionKey(level.getServer()), e);
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
