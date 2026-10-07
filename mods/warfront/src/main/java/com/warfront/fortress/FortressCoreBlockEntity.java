package com.warfront.fortress;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.registry.WFRegistry;
import com.warfront.world.WarbandSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A fortress's heart. When a player comes within 40 blocks the garrison takes its posts and the warlord rises from
 * the seat; defenders who fall are slowly replaced while the warlord lives. When the warlord falls the garrison
 * routs (see {@link Warlords}).
 */
public class FortressCoreBlockEntity extends BlockEntity {
    public static final double WAKE_RANGE = 40;
    public static final int GARRISON = 10;
    /** Ticks between reinforcements while the warlord lives. */
    public static final int RESPAWN_TICKS = 1200;
    public static final String FORTRESS_TAG = "warfront_fortress";

    private boolean active;
    private boolean defeated;
    @Nullable private UUID warband;
    @Nullable private UUID warlord;
    private long nextRespawn;

    public FortressCoreBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.FORTRESS_CORE_BE.get(), pos, state);
    }

    public NpcFaction faction() {
        return NpcFaction.values()[getBlockState().getValue(FortressCoreBlock.FACTION) % NpcFaction.values().length];
    }

    public boolean isActive() {
        return active;
    }

    public boolean isDefeated() {
        return defeated;
    }

    @Nullable
    public UUID getWarband() {
        return warband;
    }

    @Nullable
    public UUID getWarlord() {
        return warlord;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FortressCoreBlockEntity be) {
        if (be.defeated || !(level instanceof ServerLevel server) || level.getGameTime() % 20 != 0) return;
        if (!be.active) {
            Player near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, WAKE_RANGE,
                    p -> !p.isSpectator());
            if (near != null) be.activate(server);
            return;
        }
        if (level.getGameTime() >= be.nextRespawn && be.warband != null) {
            be.nextRespawn = level.getGameTime() + RESPAWN_TICKS;
            int alive = be.garrison(server).size();
            if (alive < GARRISON) be.spawnGroup(server, List.of(SoldierRole.SWORDSMAN, SoldierRole.ARCHER), 0);
            be.setChanged();
        }
        Warlords.tick(server, be);
    }

    /** The garrison takes its posts and the warlord rises. */
    public void activate(ServerLevel level) {
        if (active || defeated) return;
        active = true;
        warband = UUID.randomUUID();
        nextRespawn = level.getGameTime() + RESPAWN_TICKS;
        List<SoldierRole> roles = new ArrayList<>(List.of(SoldierRole.SHIELDBEARER, SoldierRole.SHIELDBEARER, SoldierRole.SPEARMAN,
                SoldierRole.SWORDSMAN, SoldierRole.SWORDSMAN, SoldierRole.ARCHER, SoldierRole.ARCHER, SoldierRole.HEALER,
                SoldierRole.CAPTAIN, SoldierRole.SPEARMAN));
        for (int i = 0; i < 4; i++) spawnGroup(level, roles.subList(i * roles.size() / 4, (i + 1) * roles.size() / 4), i);
        warlord = Warlords.raise(level, this, warband).getUUID();
        setChanged();
    }

    /** A group of defenders at one of the four courtyard posts. */
    private void spawnGroup(ServerLevel level, List<SoldierRole> roles, int post) {
        int[][] posts = {{-9, 9}, {9, 9}, {-9, -9}, {9, -9}};
        int[] p = posts[post % posts.length];
        BlockPos at = worldPosition.offset(p[0], 0, p[1]);
        for (SoldierEntity s : WarbandSpawner.spawnInto(level, warband, faction(), roles, at, Vec3.atBottomCenterOf(at), null, 3)) {
            s.setPersistenceRequired();
            s.getPersistentData().putLong(FORTRESS_TAG, worldPosition.asLong());
        }
    }

    public List<SoldierEntity> garrison(ServerLevel level) {
        UUID id = warband;
        if (id == null) return List.of();
        return level.getEntitiesOfClass(SoldierEntity.class, new AABB(worldPosition).inflate(48),
                s -> s.isAlive() && id.equals(s.getWarbandId()) && !s.isWarlord());
    }

    /** The warlord fell: the garrison routs and the fortress goes quiet for good. */
    public void defeat(ServerLevel level, @Nullable ServerPlayer victor) {
        if (defeated) return;
        defeated = true;
        for (SoldierEntity s : garrison(level)) s.discard();
        Warlords.rewards(level, this, victor);
        setChanged();
    }

    /** Test mode: sends everyone home and resets the fortress. */
    public void reset(ServerLevel level) {
        UUID id = warband;
        if (id != null) {
            for (SoldierEntity s : level.getEntitiesOfClass(SoldierEntity.class, new AABB(worldPosition).inflate(64),
                    s -> id.equals(s.getWarbandId()))) s.discard();
        }
        active = false;
        defeated = false;
        warband = null;
        warlord = null;
        Warlords.clearBar(this);
        setChanged();
    }

    @Override
    public void setRemoved() {
        Warlords.clearBar(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Active", active);
        tag.putBoolean("Defeated", defeated);
        if (warband != null) tag.putUUID("Warband", warband);
        if (warlord != null) tag.putUUID("Warlord", warlord);
        tag.putLong("NextRespawn", nextRespawn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        active = tag.getBoolean("Active");
        defeated = tag.getBoolean("Defeated");
        warband = tag.hasUUID("Warband") ? tag.getUUID("Warband") : null;
        warlord = tag.hasUUID("Warlord") ? tag.getUUID("Warlord") : null;
        nextRespawn = tag.getLong("NextRespawn");
    }
}
