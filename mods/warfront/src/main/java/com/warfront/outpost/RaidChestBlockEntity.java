package com.warfront.outpost;

import com.warfront.army.SoldierRole;
import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.registry.WFRegistry;
import com.warfront.world.WarbandSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A raid chest: the outpost's loot, and its record. It remembers every block the outpost placed, the War Standard it
 * besieges and the siege's warband, sends a small group of raiders at the standard every so often, and tears the
 * outpost down when it is broken.
 */
public class RaidChestBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final List<Long> placed = new ArrayList<>();
    @Nullable private BlockPos standard;
    @Nullable private UUID warband;
    private long nextPressure;
    private boolean tornDown;

    public RaidChestBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.RAID_CHEST_BE.get(), pos, state);
    }

    public NpcFaction faction() {
        return NpcFaction.values()[getBlockState().getValue(RaidChestBlock.FACTION) % NpcFaction.values().length];
    }

    /** Links this chest to its siege and the blocks its outpost placed. */
    public void link(BlockPos standard, UUID warband, List<BlockPos> blocks) {
        this.standard = standard.immutable();
        this.warband = warband;
        placed.clear();
        for (BlockPos p : blocks) placed.add(p.asLong());
        nextPressure = level == null ? 0 : level.getGameTime() + WFConfig.OUTPOST_PRESSURE.get() * 20L;
        setChanged();
    }

    public List<BlockPos> placedBlocks() {
        List<BlockPos> out = new ArrayList<>();
        for (long l : placed) out.add(BlockPos.of(l));
        return out;
    }

    @Nullable
    public UUID getWarband() {
        return warband;
    }

    /** Every 90 seconds (config) a few raiders set out from the outpost toward the standard. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, RaidChestBlockEntity be) {
        if (be.standard == null || be.warband == null || !(level instanceof ServerLevel server)) return;
        if (level.getGameTime() < be.nextPressure) return;
        be.nextPressure = level.getGameTime() + WFConfig.OUTPOST_PRESSURE.get() * 20L;
        List<SoldierRole> group = List.of(SoldierRole.SWORDSMAN, SoldierRole.SPEARMAN, SoldierRole.ARCHER);
        WarbandSpawner.spawnInto(server, be.warband, be.faction(), group, pos.above(), Vec3.atBottomCenterOf(be.standard), be.standard, 2);
        server.playSound(null, pos, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4.0F, 1.3F);
    }

    /** Siege engines (the human Trebuchet) batter the outpost: this much more and it falls. */
    public static final int OUTPOST_HEALTH = 60;
    private int battered;

    public int outpostHealth() {
        return Math.max(0, OUTPOST_HEALTH - battered);
    }

    /**
     * A boulder hits the outpost: knocks out a few of its blocks near {@code at}, and once it has taken
     * {@link #OUTPOST_HEALTH} the whole outpost falls (its loot spills). Returns true if it fell.
     */
    public boolean bombard(ServerLevel level, net.minecraft.world.phys.Vec3 at, int damage) {
        if (tornDown) return false;
        battered += damage;
        setChanged();
        int knocked = 0;
        List<Long> near = new ArrayList<>(placed);
        near.sort(java.util.Comparator.comparingDouble(l -> BlockPos.of(l).distToCenterSqr(at)));
        for (long l : near) {
            BlockPos p = BlockPos.of(l);
            if (p.equals(worldPosition) || level.getBlockState(p).isAir()) continue;
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            if (++knocked >= 6) break;
        }
        if (battered < OUTPOST_HEALTH) return false;
        net.minecraft.world.Containers.dropContents(level, worldPosition, this);
        clearContent();
        level.destroyBlock(worldPosition, false);   // the chest going tears the outpost down
        return true;
    }

    /** Removes every block the outpost placed and the raiders still holding it. Called when the chest goes. */
    public void tearDown(ServerLevel level) {
        if (tornDown) return;
        tornDown = true;
        for (long l : placed) {
            BlockPos p = BlockPos.of(l);
            if (!p.equals(worldPosition)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        if (warband != null) {
            UUID id = warband;
            for (SoldierEntity s : level.getEntitiesOfClass(SoldierEntity.class, new AABB(worldPosition).inflate(24),
                    s -> id.equals(s.getWarbandId()))) s.discard();
        }
        level.sendParticles(ParticleTypes.EXPLOSION, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                6, 3, 1, 3, 0.1);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                40, 4, 2, 4, 0.02);
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5F, 0.8F);
        if (standard != null && level.getBlockEntity(standard) instanceof com.warfront.block.WarStandardBlockEntity s) s.outpostFallen(level);
    }

    // ------------------------------------------------------------------ container

    @Override
    protected Component getDefaultName() {
        return Component.literal(faction().displayName + " Raid Chest");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;   // loot only comes out
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (battered > 0) tag.putInt("Battered", battered);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("Placed", new LongArrayTag(placed));
        if (standard != null) tag.putLong("Standard", standard.asLong());
        if (warband != null) tag.putUUID("Warband", warband);
        tag.putLong("NextPressure", nextPressure);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        battered = tag.getInt("Battered");
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        placed.clear();
        for (long l : tag.getLongArray("Placed")) placed.add(l);
        standard = tag.contains("Standard") ? BlockPos.of(tag.getLong("Standard")) : null;
        warband = tag.hasUUID("Warband") ? tag.getUUID("Warband") : null;
        nextPressure = tag.getLong("NextPressure");
    }
}
