package com.warfront.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.Warfront;
import com.warfront.racetower.RaceTowerBlockEntity;
import com.warfront.racetower.RaceTowerType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/** Draws a race tower's model on its plinth block, glowing only while its mana network can pay for a shot. */
public class RaceTowerRenderer implements BlockEntityRenderer<RaceTowerBlockEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private final Map<RaceTowerType, ModelPart> models = new EnumMap<>(RaceTowerType.class);

    public RaceTowerRenderer(BlockEntityRendererProvider.Context ctx) {
        for (RaceTowerType t : RaceTowerType.values()) models.put(t, ctx.bakeLayer(WFModelLayers.of("rt_" + t.id)));
    }

    @Override
    public void render(RaceTowerBlockEntity tower, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tower.getLevel() == null) return;
        RaceTowerType type = tower.type();
        ModelPart root = models.get(type);
        ModelPart body = root.getChild("body");
        float time = tower.getLevel().getGameTime() + partialTick;
        boolean on = tower.isPowered();
        switch (type) {
            case BALLISTA -> body.getChild("turret").yRot = Mth.sin(time * 0.01F) * 0.8F;
            case SOUL_PYRE -> {
                ModelPart flame = body.getChild("flame");
                flame.visible = on;
                flame.yScale = 0.9F + Mth.sin(time * 0.3F) * 0.1F;
            }
            case SUN_LANCE -> body.getChild("lens").yRot = on ? time * 0.04F : 0.785F;
            case LURKER_PIT -> body.getChild("maw").visible = tower.isRevealed();
            default -> {
            }
        }
        String id = "rt_" + type.id;
        String race = type.race.id();
        ResourceLocation tex = Warfront.id("textures/entity/soldier/tower_" + race + "/" + id + ".png");
        ResourceLocation glow = Warfront.id("textures/entity/soldier/tower_" + race + "/" + id + "_glow.png");
        int lit = LevelRenderer.getLightColor(tower.getLevel(), tower.getBlockPos().above());
        pose.pushPose();
        pose.translate(0.5, 2.5, 0.5);
        pose.scale(-1F, -1F, 1F);
        root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), lit, OverlayTexture.NO_OVERLAY);
        if (on) root.render(pose, buffers.getBuffer(RenderType.eyes(glow)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(RaceTowerBlockEntity tower) {
        return true;
    }
}
