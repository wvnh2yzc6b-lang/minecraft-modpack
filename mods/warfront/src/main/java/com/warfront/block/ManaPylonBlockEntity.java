package com.warfront.block;

import com.warfront.mana.ManaNodeBlockEntity;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Relays a mana network: stores nothing, but extends its reach by another link range. */
public class ManaPylonBlockEntity extends ManaNodeBlockEntity {
    public ManaPylonBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.MANA_PYLON_BE.get(), pos, state);
    }
}
