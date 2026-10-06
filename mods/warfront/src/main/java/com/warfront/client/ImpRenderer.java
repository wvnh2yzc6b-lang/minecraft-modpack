package com.warfront.client;

import com.warfront.Warfront;
import com.warfront.client.model.ImpModel;
import com.warfront.entity.SoldierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders imp-bodied soldiers. Used by {@link SoldierRenderer}, which picks the body per entity. */
public class ImpRenderer extends HumanoidMobRenderer<SoldierEntity, ImpModel> {
    public ImpRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ImpModel(ctx.bakeLayer(WFModelLayers.IMP)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity entity) {
        return Warfront.id("textures/entity/soldier/" + SkinKeys.of(entity.getSkin()) + "/imp.png");
    }
}
