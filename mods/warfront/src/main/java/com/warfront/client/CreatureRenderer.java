package com.warfront.client;

import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders one generated creature model (imp_impaler, hive_lancer, hive_beast...). {@link SoldierRenderer} picks
 * the model per entity. Textures live at {@code textures/entity/soldier/<skin>/<variant>.png}, with a glow layer.
 */
public class CreatureRenderer<M extends HumanoidModel<SoldierEntity>> extends HumanoidMobRenderer<SoldierEntity, M> {
    private final String variant;

    public CreatureRenderer(EntityRendererProvider.Context ctx, M model, float shadow, String variant) {
        super(ctx, model, shadow);
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
