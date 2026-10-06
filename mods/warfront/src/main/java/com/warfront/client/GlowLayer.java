package com.warfront.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.warfront.entity.SoldierEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Full-bright pass for glowing eyes, runes and embers, drawn from a separate "_glow" texture. */
public class GlowLayer<M extends EntityModel<SoldierEntity>> extends RenderLayer<SoldierEntity, M> {
    private final Function<SoldierEntity, ResourceLocation> texture;

    public GlowLayer(RenderLayerParent<SoldierEntity, M> parent, Function<SoldierEntity, ResourceLocation> texture) {
        super(parent);
        this.texture = texture;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SoldierEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(texture.apply(entity)));
        getParentModel().renderToBuffer(poseStack, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY);
    }
}
