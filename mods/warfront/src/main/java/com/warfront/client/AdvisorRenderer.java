package com.warfront.client;

import com.warfront.Warfront;
import com.warfront.advisor.AdvisorEntity;
import com.warfront.faction.Race;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The advisor. A placeholder look until his seven guises are designed: the race skin of his current guise. */
public class AdvisorRenderer extends HumanoidMobRenderer<AdvisorEntity, PlayerModel<AdvisorEntity>> {
    public AdvisorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AdvisorEntity entity) {
        Race race = entity.getDisguise();
        return Warfront.id("textures/entity/soldier/" + race.id() + ".png");
    }
}
