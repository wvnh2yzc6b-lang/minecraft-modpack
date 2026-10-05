package com.warfront.client;

import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class SoldierRenderer extends HumanoidMobRenderer<SoldierEntity, SoldierModel> {
    private static final ResourceLocation[] SKINS = {
            Warfront.id("textures/entity/soldier/human.png"),
            Warfront.id("textures/entity/soldier/elf.png"),
            Warfront.id("textures/entity/soldier/dwarf.png"),
            Warfront.id("textures/entity/soldier/orc.png"),
            Warfront.id("textures/entity/soldier/marauder.png"),
            Warfront.id("textures/entity/soldier/black_legion.png"),
    };

    public SoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SoldierModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity entity) {
        return SKINS[entity.getSkin()];
    }
}
