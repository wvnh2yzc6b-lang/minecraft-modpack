package com.warfront.block;

import com.warfront.config.WFConfig;
import com.warfront.mana.ManaNodeBlockEntity;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/** Stores mana for the network. Fed Mana Shards and Mana Crystals by hand or by hopper. */
public class ManaWellBlockEntity extends ManaNodeBlockEntity {
    public static final float SHARD_MANA = 10F;
    public static final float CRYSTAL_MANA = 50F;

    private float mana;

    /** Hoppers and pipes can push fuel in; nothing comes out. */
    public final IItemHandler fuelInput = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int fits = fits(stack);
            if (fits == 0) return stack;
            if (!simulate) addFuel(stack, fits);
            return stack.copyWithCount(stack.getCount() - fits);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return fuelValue(stack) > 0F;
        }
    };

    public ManaWellBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.MANA_WELL_BE.get(), pos, state);
    }

    public static float fuelValue(ItemStack stack) {
        if (stack.is(WFRegistry.MANA_SHARD.get())) return SHARD_MANA;
        if (stack.is(WFRegistry.MANA_CRYSTAL.get())) return CRYSTAL_MANA;
        return 0F;
    }

    public static float capacity() {
        return WFConfig.WELL_CAPACITY.get();
    }

    public float getMana() {
        return mana;
    }

    public void setMana(float value) {
        mana = Mth.clamp(value, 0F, capacity());
        changed();
    }

    /** How many items from this stack would fit without wasting mana. */
    public int fits(ItemStack stack) {
        float value = fuelValue(stack);
        if (value <= 0F) return 0;
        return Math.min(stack.getCount(), (int) ((capacity() - mana) / value));
    }

    /** Burns {@code count} items of this stack into mana. The caller removes them. */
    public void addFuel(ItemStack stack, int count) {
        setMana(mana + fuelValue(stack) * count);
    }

    /** Removes up to {@code amount} mana and returns how much was taken. */
    public float take(float amount) {
        float taken = Math.min(amount, mana);
        setMana(mana - taken);
        return taken;
    }

    /** 0 (empty) to 4 (full): drives the well's look and light. */
    public int fillLevel() {
        return mana <= 0F ? 0 : Mth.clamp((int) Math.ceil(mana / capacity() * 4F), 1, 4);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            int fill = fillLevel();
            if (state.hasProperty(ManaWellBlock.FILL) && state.getValue(ManaWellBlock.FILL) != fill) {
                level.setBlock(worldPosition, state.setValue(ManaWellBlock.FILL, fill), 3);
            } else {
                level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Mana", mana);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mana = tag.getFloat("Mana");
    }
}
