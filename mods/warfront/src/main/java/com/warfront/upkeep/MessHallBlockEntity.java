package com.warfront.upkeep;

import com.warfront.entity.SoldierEntity;
import com.warfront.faction.FactionData;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 54 slots of food. Takes anything edible: vanilla food, the c:foods tag, other mods' crops. */
public class MessHallBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 54;
    /** How far from the hall units eat. */
    public static final double RANGE = 32;

    /** Loaded halls, per dimension, for the morning meal. */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> HALLS = new HashMap<>();

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    @Nullable private UUID owner;

    public MessHallBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.MESS_HALL_BE.get(), pos, state);
    }

    public static Set<BlockPos> loaded(Level level) {
        return HALLS.getOrDefault(level.dimension(), Set.of());
    }

    public static void clearAll() {
        HALLS.clear();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) HALLS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) HALLS.getOrDefault(level.dimension(), new HashSet<>()).remove(worldPosition);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) HALLS.getOrDefault(level.dimension(), new HashSet<>()).remove(worldPosition);
        super.onChunkUnloaded();
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public String factionKey(@Nullable MinecraftServer server) {
        if (owner == null) return Factions.WILD;
        return server == null ? "p:" + owner : FactionData.get(server).keyOf(owner);
    }

    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && (stack.has(DataComponents.FOOD) || stack.is(Tags.Items.FOODS));
    }

    public static int nutrition(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? 1 : Math.max(1, food.nutrition());
    }

    /** Total food points stored. */
    public int foodPoints() {
        int n = 0;
        for (ItemStack s : items) if (isFood(s)) n += nutrition(s) * s.getCount();
        return n;
    }

    /** Takes at least {@code points} food points, whole items at a time. False (and nothing taken) if short. */
    public boolean eat(int points) {
        if (foodPoints() < points) return false;
        int left = points;
        for (ItemStack s : items) {
            while (left > 0 && isFood(s) && !s.isEmpty()) {
                left -= nutrition(s);
                s.shrink(1);
            }
            if (left <= 0) break;
        }
        setChanged();
        return true;
    }

    /** Battle units allied to this hall within range: the ones it feeds. */
    public List<SoldierEntity> troops() {
        if (level == null) return List.of();
        MinecraftServer server = level.getServer();
        String key = factionKey(server);
        return level.getEntitiesOfClass(SoldierEntity.class, new AABB(worldPosition).inflate(RANGE), s -> s.isAlive()
                && !s.getRole().posted() && Factions.relation(server, key, s.getFactionKey()) == Relation.ALLY);
    }

    public int troopsInRange() {
        return troops().size();
    }

    public double daysOfFood(int troops) {
        return troops == 0 ? 0 : foodPoints() / (double) (troops * Upkeep.pointsPerUnit());
    }

    public void empty() {
        items.replaceAll(s -> ItemStack.EMPTY);
        setChanged();
    }

    // ------------------------------------------------------------------ container

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.warfront.mess_hall");
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
        return isFood(stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.sixRows(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
