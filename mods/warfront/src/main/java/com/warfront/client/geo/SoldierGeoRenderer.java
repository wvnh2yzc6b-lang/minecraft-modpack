package com.warfront.client.geo;

import com.warfront.entity.SoldierEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Draws a unit with its GeckoLib model: translucent where the texture is (wing membranes), glowing eyes. */
public class SoldierGeoRenderer extends GeoEntityRenderer<SoldierEntity> {
    public SoldierGeoRenderer(EntityRendererProvider.Context ctx, String id, float shadow) {
        super(ctx, new SoldierGeoModel(id));
        this.shadowRadius = shadow;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(SoldierEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource,
                                    float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
