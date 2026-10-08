package com.warfront.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warfront.Warfront;
import com.warfront.block.TowerBlock;
import com.warfront.block.TowerBlockEntity;
import com.warfront.block.TowerType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Set;

/**
 * Draws the 3D tower on top of its block: a watchtower, a spire with a floating crystal, or a shrine with a basin
 * and a hovering light, in the owner's race's materials. The glow (crystal, runes, water, light) only shows while
 * the tower's mana network can pay for a shot.
 */
public class TowerRenderer implements BlockEntityRenderer<TowerBlockEntity> {
    private static final Set<String> RACES = Set.of("human", "elf", "dwarf", "orc", "demon", "angel", "hive");
    private static final int FULL_BRIGHT = 0xF000F0;

    private final ModelPart arrow;
    private final ModelPart arcane;
    private final ModelPart healing;

    public TowerRenderer(BlockEntityRendererProvider.Context ctx) {
        arrow = ctx.bakeLayer(WFModelLayers.of("tower_arrow"));
        arcane = ctx.bakeLayer(WFModelLayers.of("tower_arcane"));
        healing = ctx.bakeLayer(WFModelLayers.of("tower_healing"));
    }

    private static String id(TowerType type) {
        return switch (type) {
            case ARROW -> "tower_arrow";
            case ARCANE -> "tower_arcane";
            case HEALING -> "tower_healing";
        };
    }

    @Override
    public void render(TowerBlockEntity tower, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(tower.getBlockState().getBlock() instanceof TowerBlock block) || tower.getLevel() == null) return;
        TowerType type = block.getTowerType();
        ModelPart root = switch (type) {
            case ARROW -> arrow;
            case ARCANE -> arcane;
            case HEALING -> healing;
        };
        String race = RACES.contains(tower.race()) ? tower.race() : "human";
        String id = id(type);
        ResourceLocation tex = Warfront.id("textures/entity/soldier/tower_" + race + "/" + id + ".png");
        ResourceLocation glow = Warfront.id("textures/entity/soldier/tower_" + race + "/" + id + "_glow.png");
        float time = tower.getLevel().getGameTime() + partialTick;
        ModelPart body = root.getChild("body");
        if (type == TowerType.ARCANE) {
            ModelPart crystal = body.getChild("crystal");
            crystal.yRot = tower.isPowered() ? time * 0.05F : 0.785F;
            crystal.y = -15F + (tower.isPowered() ? Mth.sin(time * 0.08F) * 1.2F : 2F);
        } else if (type == TowerType.HEALING) {
            body.getChild("light").y = 8F + (tower.isPowered() ? Mth.sin(time * 0.06F) * 1.0F : 6F);
        }
        // Light from the block above the plinth, so the tower isn't drawn in the plinth's own shadow.
        int lit = net.minecraft.client.renderer.LevelRenderer.getLightColor(tower.getLevel(), tower.getBlockPos().above());
        pose.pushPose();
        pose.translate(0.5, 2.5, 0.5);
        pose.scale(-1F, -1F, 1F);
        root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), lit, OverlayTexture.NO_OVERLAY);
        if (tower.isPowered()) root.render(pose, buffers.getBuffer(RenderType.eyes(glow)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TowerBlockEntity tower) {
        return true;
    }
}
