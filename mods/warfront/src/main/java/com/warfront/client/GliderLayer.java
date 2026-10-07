package com.warfront.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.Warfront;
import com.warfront.registry.WFRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Mana Glider on a player's back: folded flat down the back on the ground, spread wide while gliding, blending
 * between the two over a few frames.
 */
public class GliderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEX = Warfront.id("textures/entity/soldier/glider/mana_glider.png");
    private static final ResourceLocation GLOW = Warfront.id("textures/entity/soldier/glider/mana_glider_glow.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    // Folded and spread poses of the right wing (the left mirrors it).
    private static final float FOLD_Y = 0.12F, FOLD_Z = -1.42F, SPREAD_Y = 0.3F, SPREAD_Z = -0.35F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart wingR;
    private final ModelPart wingL;
    private final Map<AbstractClientPlayer, float[]> open = new WeakHashMap<>();

    public GliderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        root = models.bakeLayer(WFModelLayers.of("mana_glider"));
        body = root.getChild("body");
        wingR = body.getChild("wing_r");
        wingL = body.getChild("wing_l");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !player.getItemBySlot(EquipmentSlot.CHEST).is(WFRegistry.MANA_GLIDER.get())) return;
        // Ease toward spread while gliding, folded otherwise: [openness, last age].
        float[] s = open.computeIfAbsent(player, p -> new float[]{0F, ageInTicks});
        float dt = Mth.clamp(ageInTicks - s[1], 0F, 2F);
        s[1] = ageInTicks;
        float target = player.isFallFlying() ? 1F : 0F;
        s[0] += (target - s[0]) * Math.min(1F, dt * 0.35F);
        float t = s[0] * s[0] * (3 - 2 * s[0]);

        body.copyFrom(getParentModel().body);
        wingR.yRot = Mth.lerp(t, FOLD_Y, SPREAD_Y);
        wingR.zRot = Mth.lerp(t, FOLD_Z, SPREAD_Z);
        wingL.yRot = -wingR.yRot;
        wingL.zRot = -wingR.zRot;
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        root.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, overlay);
        root.render(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
