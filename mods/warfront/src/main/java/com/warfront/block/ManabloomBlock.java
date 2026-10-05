package com.warfront.block;

import com.mojang.serialization.MapCodec;
import com.warfront.registry.WFRegistry;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** A glowing crop that ripens into Mana Shards. Plant on farmland; seeds drop from grass. */
public class ManabloomBlock extends CropBlock {
    public static final MapCodec<ManabloomBlock> CODEC = simpleCodec(ManabloomBlock::new);

    public ManabloomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return WFRegistry.MANABLOOM_SEEDS.get();
    }
}
