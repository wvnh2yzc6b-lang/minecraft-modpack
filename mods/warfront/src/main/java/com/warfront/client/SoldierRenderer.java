package com.warfront.client;

import com.warfront.Warfront;
import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.army.UnitBody;
import com.warfront.entity.SoldierEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public class SoldierRenderer extends HumanoidMobRenderer<SoldierEntity, SoldierModel> {
    /** Race skins first, then one per NPC faction, matching {@link SoldierEntity#getSkin()}. */
    private static final ResourceLocation[] SKINS = buildSkins();

    private static ResourceLocation[] buildSkins() {
        Race[] races = Race.values();
        NpcFaction[] factions = NpcFaction.values();
        ResourceLocation[] out = new ResourceLocation[races.length + factions.length];
        for (Race r : races) out[r.ordinal()] = Warfront.id("textures/entity/soldier/" + r.id() + ".png");
        for (NpcFaction f : factions) {
            out[f.skin()] = Warfront.id("textures/entity/soldier/" + f.name().toLowerCase(Locale.ROOT) + ".png");
        }
        return out;
    }

    private final java.util.Map<String, ImpRenderer> imps = new java.util.HashMap<>();

    public SoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SoldierModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        for (String id : com.warfront.client.model.UnitGeometry.all().keySet()) {
            if (id.startsWith("imp")) imps.put(id, new ImpRenderer(ctx, WFModelLayers.of(id), id));
        }
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addLayer(new RoleGearLayer(this, ctx.getModelSet()));
    }

    private ImpRenderer impFor(SoldierEntity entity) {
        UnitBody body = entity.getBody();
        return body == UnitBody.IMP ? imps.get(body.variant(entity.getRole())) : null;
    }

    @Override
    public void render(SoldierEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ImpRenderer imp = impFor(entity);
        if (imp != null) {
            imp.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity entity) {
        ImpRenderer imp = impFor(entity);
        if (imp != null) return imp.getTextureLocation(entity);
        return SKINS[entity.getSkin()];
    }
}
