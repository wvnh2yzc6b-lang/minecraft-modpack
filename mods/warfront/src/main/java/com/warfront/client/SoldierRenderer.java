package com.warfront.client;

import com.warfront.Warfront;
import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.army.UnitBody;
import com.warfront.client.model.HiveBeastModel;
import com.warfront.client.model.ImpModel;
import com.warfront.client.model.LancerModel;
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

    /** Generated creature models (imps, Hive units), by variant name. */
    private final java.util.Map<String, CreatureRenderer<?>> creatures = new java.util.HashMap<>();

    public SoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SoldierModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        for (String id : com.warfront.client.model.UnitGeometry.all().keySet()) {
            var part = id.startsWith("imp") || id.startsWith("hive_") ? ctx.bakeLayer(WFModelLayers.of(id)) : null;
            if (id.startsWith("imp")) creatures.put(id, new CreatureRenderer<>(ctx, new ImpModel(part), 0.35F, id));
            else if (id.equals("hive_lancer")) creatures.put(id, new CreatureRenderer<>(ctx, new LancerModel(part), 0.5F, id));
            else if (id.equals("hive_beast")) creatures.put(id, new CreatureRenderer<>(ctx, new HiveBeastModel(part), 1.0F, id));
        }
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addLayer(new RoleGearLayer(this, ctx.getModelSet()));
    }

    private CreatureRenderer<?> creatureFor(SoldierEntity entity) {
        UnitBody body = entity.getBody();
        return body == UnitBody.HUMANOID ? null : creatures.get(body.variant(entity.getRole()));
    }

    @Override
    public void render(SoldierEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        CreatureRenderer<?> creature = creatureFor(entity);
        if (creature != null) {
            creature.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SoldierEntity entity) {
        CreatureRenderer<?> creature = creatureFor(entity);
        if (creature != null) return creature.getTextureLocation(entity);
        return SKINS[entity.getSkin()];
    }
}
