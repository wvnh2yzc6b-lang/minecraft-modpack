package com.warfront.client;

import com.warfront.Warfront;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class WFModelLayers {
    public static final ModelLayerLocation IMP = layer("imp");
    public static final ModelLayerLocation IMP_BULWARK = layer("imp_bulwark");
    public static final ModelLayerLocation IMP_IMPALER = layer("imp_impaler");
    public static final ModelLayerLocation IMP_FIRECASTER = layer("imp_firecaster");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Warfront.id("soldier"), name);
    }

    private WFModelLayers() {}
}
