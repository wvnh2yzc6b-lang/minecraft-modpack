package com.warfront.client.geo;

import com.warfront.Warfront;
import com.warfront.client.SkinKeys;
import com.warfront.entity.SoldierEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * A high-detail unit model: geo/&lt;id&gt;.geo.json, animations/&lt;id&gt;.animation.json, and a texture per skin
 * (race or enemy faction) at textures/entity/geo/&lt;skin&gt;/&lt;id&gt;.png, all written by the mod's tools/geo_*.py.
 */
public class SoldierGeoModel extends GeoModel<SoldierEntity> {
    private final String id;
    private final ResourceLocation model, animation;

    public SoldierGeoModel(String id) {
        this.id = id;
        this.model = Warfront.id("geo/" + id + ".geo.json");
        this.animation = Warfront.id("animations/" + id + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SoldierEntity animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(SoldierEntity animatable) {
        return Warfront.id("textures/entity/geo/" + SkinKeys.of(animatable.getSkin()) + "/" + id + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(SoldierEntity animatable) {
        return animation;
    }
}
