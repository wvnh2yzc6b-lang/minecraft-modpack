package com.warfront.client;

import com.warfront.Warfront;
import com.warfront.client.model.ImpModel;
import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders one imp variant (imp_impaler, imp_firecaster). {@link SoldierRenderer}
 * picks the variant per entity.
 */
public class ImpRenderer extends HumanoidMobRenderer<SoldierEntity, ImpModel> {
    private final String variant;

    public ImpRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, String variant) {
        super(ctx, new ImpModel(ctx.bakeLayer(layer)), 0.35F);
        this.variant = variant;
        this.addLayer(new GlowLayer<>(this, e -> texture(e, "_glow")));
    }

    private ResourceLocation texture(SoldierEntity entity, String suffix) {
        return Warfront.id("textures/entity/soldier/" + SkinKeys.of(entity.getSkin()) + "/" + variant + suffix + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity entity) {
        return texture(entity, "");
    }
}
