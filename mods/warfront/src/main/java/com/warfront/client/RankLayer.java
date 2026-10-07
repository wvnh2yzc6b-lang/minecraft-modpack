package com.warfront.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Veterancy on show, for humanoid units: armor that grows with rank (Regular padded leather in the race's color,
 * Veteran chain, Elite full plate, Legend gilded plate), and one race-colored chevron per rank on the left shoulder.
 * Armor a player handed over takes the slot's place.
 */
public class RankLayer extends RenderLayer<SoldierEntity, SoldierModel> {
    private static final ResourceLocation INSIGNIA = Warfront.id("textures/entity/insignia.png");
    private static final String[] MATERIAL = {"", "leather", "chainmail", "iron", "gold"};

    private final HumanoidModel<SoldierEntity> inner;
    private final HumanoidModel<SoldierEntity> outer;
    private final ModelPart chevron;

    public RankLayer(RenderLayerParent<SoldierEntity, SoldierModel> parent, EntityModelSet models) {
        super(parent);
        inner = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        outer = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("chevron", CubeListBuilder.create().texOffs(0, 0)
                .addBox(3.05F, -1.0F, -1.5F, 0.4F, 0.8F, 3.0F), PartPose.ZERO);
        chevron = LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("chevron");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, SoldierEntity entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int rank = entity.getRank();
        if (rank <= 0 || entity.isInvisible() || entity.getRole().posted()) return;
        int color = 0xFF000000 | (entity.getVisualRace().color.getColor() == null ? 0xFFFFFF : entity.getVisualRace().color.getColor());
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        SoldierModel model = getParentModel();

        // Armor by rank: Regular chest and boots; Veteran adds legs; Elite and Legend add the helmet.
        String material = MATERIAL[Math.min(rank, MATERIAL.length - 1)];
        int tint = rank == 1 ? color : 0xFFFFFFFF;
        piece(poseStack, buffer, light, overlay, entity, model, EquipmentSlot.CHEST, material, tint);
        piece(poseStack, buffer, light, overlay, entity, model, EquipmentSlot.FEET, material, tint);
        if (rank >= 2) piece(poseStack, buffer, light, overlay, entity, model, EquipmentSlot.LEGS, material, tint);
        if (rank >= 3) piece(poseStack, buffer, light, overlay, entity, model, EquipmentSlot.HEAD, material, tint);

        // Chevrons on the left shoulder.
        poseStack.pushPose();
        model.leftArm.translateAndRotate(poseStack);
        var vc = buffer.getBuffer(RenderType.entityCutoutNoCull(INSIGNIA));
        for (int i = 0; i < rank; i++) {
            chevron.y = i * 1.1F;
            chevron.render(poseStack, vc, light, overlay, color);
        }
        poseStack.popPose();
    }

    private void piece(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, SoldierEntity entity,
                       SoldierModel parent, EquipmentSlot slot, String material, int tint) {
        if (!entity.getItemBySlot(slot).isEmpty()) return;   // real armor wins
        HumanoidModel<SoldierEntity> m = slot == EquipmentSlot.LEGS ? inner : outer;
        parent.copyPropertiesTo(m);
        m.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                m.head.visible = true;
                m.hat.visible = true;
            }
            case CHEST -> {
                m.body.visible = true;
                m.rightArm.visible = true;
                m.leftArm.visible = true;
            }
            case LEGS -> {
                m.body.visible = true;
                m.rightLeg.visible = true;
                m.leftLeg.visible = true;
            }
            default -> {
                m.rightLeg.visible = true;
                m.leftLeg.visible = true;
            }
        }
        String layer = slot == EquipmentSlot.LEGS ? "2" : "1";
        ResourceLocation tex = ResourceLocation.withDefaultNamespace("textures/models/armor/" + material + "_layer_" + layer + ".png");
        m.renderToBuffer(poseStack, buffer.getBuffer(RenderType.armorCutoutNoCull(tex)), light, overlay, tint);
    }
}
