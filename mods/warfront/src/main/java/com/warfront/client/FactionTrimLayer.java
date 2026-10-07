package com.warfront.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.Warfront;
import com.warfront.client.model.UnitGeometry;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * Enemy faction trim: war paint and trophies, tabards, chains, acid sacs, masks, face guards, torn wings. Drawn over
 * raiders of each NPC faction (never player-owned units), on top of the body and any armor. Cosmetic only.
 */
public class FactionTrimLayer extends RenderLayer<SoldierEntity, SoldierModel> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private final ModelPart[] trims = new ModelPart[NpcFaction.values().length];
    private final ResourceLocation[] tex = new ResourceLocation[NpcFaction.values().length];
    private final ResourceLocation[] glow = new ResourceLocation[NpcFaction.values().length];

    public FactionTrimLayer(RenderLayerParent<SoldierEntity, SoldierModel> parent, EntityModelSet models) {
        super(parent);
        for (NpcFaction f : NpcFaction.values()) {
            String key = f.name().toLowerCase(Locale.ROOT);
            String id = "trim_" + key;
            if (!UnitGeometry.all().containsKey(id)) continue;
            trims[f.ordinal()] = models.bakeLayer(WFModelLayers.of(id));
            tex[f.ordinal()] = Warfront.id("textures/entity/soldier/" + key + "/" + id + ".png");
            glow[f.ordinal()] = Warfront.id("textures/entity/soldier/" + key + "/" + id + "_glow.png");
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, SoldierEntity entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || entity.getOwnerUUID() != null) return;
        int f = entity.getSkin() - Race.values().length;
        if (f < 0 || f >= trims.length || trims[f] == null) return;
        ModelPart root = trims[f];
        SoldierModel model = getParentModel();
        root.getChild("head").copyFrom(model.head);
        root.getChild("hat").copyFrom(model.hat);
        root.getChild("body").copyFrom(model.body);
        root.getChild("right_arm").copyFrom(model.rightArm);
        root.getChild("left_arm").copyFrom(model.leftArm);
        root.getChild("right_leg").copyFrom(model.rightLeg);
        root.getChild("left_leg").copyFrom(model.leftLeg);
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(tex[f])), light, overlay);
        root.render(poseStack, buffer.getBuffer(RenderType.eyes(glow[f])), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
