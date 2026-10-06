package com.warfront.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.Warfront;
import com.warfront.army.SoldierRole;
import com.warfront.client.model.UnitGeometry;
import com.warfront.entity.SoldierEntity;
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
import net.minecraft.world.entity.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

/**
 * Work clothes and kit for humanoid farmers, builders and guards: hats and helmets, aprons, tool
 * belts, baskets, tabards, lanterns. Each race has its own set; NPC factions recolor their race's.
 */
public class RoleGearLayer extends RenderLayer<SoldierEntity, SoldierModel> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private final Map<String, ModelPart> gear = new HashMap<>();

    public RoleGearLayer(RenderLayerParent<SoldierEntity, SoldierModel> parent, EntityModelSet models) {
        super(parent);
        for (String id : UnitGeometry.all().keySet()) {
            if (id.startsWith("gear_")) gear.put(id, models.bakeLayer(WFModelLayers.of(id)));
        }
    }

    /** The gear model for a unit: by role and the body-shape race (demons never get here: theirs are imps). */
    static String modelId(SoldierRole role, Race race) {
        return "gear_" + role.id() + "_" + race.id();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SoldierEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || !entity.getRole().posted()) return;
        String id = modelId(entity.getRole(), entity.getVisualRace());
        ModelPart root = gear.get(id);
        if (root == null) return;
        SoldierModel model = getParentModel();
        root.getChild("head").copyFrom(model.head);
        root.getChild("hat").copyFrom(model.hat);
        root.getChild("body").copyFrom(model.body);
        root.getChild("right_arm").copyFrom(model.rightArm);
        root.getChild("left_arm").copyFrom(model.leftArm);
        root.getChild("right_leg").copyFrom(model.rightLeg);
        root.getChild("left_leg").copyFrom(model.leftLeg);
        // A worn helmet replaces the unit's own hat or helm.
        root.getChild("head").visible = entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty();

        String key = SkinKeys.of(entity.getSkin());
        ResourceLocation tex = Warfront.id("textures/entity/soldier/" + key + "/" + id + ".png");
        ResourceLocation glow = Warfront.id("textures/entity/soldier/" + key + "/" + id + "_glow.png");
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(tex)), packedLight, overlay);
        root.render(poseStack, buffer.getBuffer(RenderType.eyes(glow)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
