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
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Race flight gear on a player's back: the orc rocket pack (exhaust flames while it climbs) and angel wings (folded
 * down the back on the ground, spread and beating in the air).
 */
public class FlightGearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation PACK_TEX = Warfront.id("textures/entity/soldier/glider/rocket_pack.png");
    private static final ResourceLocation PACK_GLOW = Warfront.id("textures/entity/soldier/glider/rocket_pack_glow.png");
    private static final ResourceLocation WINGS_TEX = Warfront.id("textures/entity/soldier/glider/angel_wings.png");
    private static final ResourceLocation WINGS_GLOW = Warfront.id("textures/entity/soldier/glider/angel_wings_glow.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float FOLD_Y = 0.15F, FOLD_Z = -1.35F, SPREAD_Y = 0.25F, SPREAD_Z = -0.2F;

    private final ModelPart pack;
    private final ModelPart packBody;
    private final ModelPart flameR;
    private final ModelPart flameL;
    private final ModelPart wings;
    private final ModelPart wingsBody;
    private final ModelPart wingR;
    private final ModelPart wingL;
    private final Map<AbstractClientPlayer, float[]> open = new WeakHashMap<>();

    public FlightGearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        pack = models.bakeLayer(WFModelLayers.of("rocket_pack"));
        packBody = pack.getChild("body");
        flameR = packBody.getChild("pack").getChild("flame_r");
        flameL = packBody.getChild("pack").getChild("flame_l");
        wings = models.bakeLayer(WFModelLayers.of("angel_wings"));
        wingsBody = wings.getChild("body");
        wingR = wingsBody.getChild("wing_r");
        wingL = wingsBody.getChild("wing_l");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        if (chest.is(WFRegistry.ROCKET_PACK.get())) {
            boolean burning = !player.onGround() && player.getY() - player.yo > 0.04;
            flameR.visible = burning;
            flameL.visible = burning;
            packBody.copyFrom(getParentModel().body);
            pack.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(PACK_TEX)), light, overlay);
            pack.render(poseStack, buffer.getBuffer(RenderType.eyes(PACK_GLOW)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        } else if (chest.is(WFRegistry.ANGEL_WINGS.get())) {
            float[] s = open.computeIfAbsent(player, p -> new float[]{0F, ageInTicks});
            float dt = Mth.clamp(ageInTicks - s[1], 0F, 2F);
            s[1] = ageInTicks;
            boolean aloft = !player.onGround() && !player.isInWater() && !player.isPassenger();
            s[0] += ((aloft ? 1F : 0F) - s[0]) * Math.min(1F, dt * 0.3F);
            float t = s[0] * s[0] * (3 - 2 * s[0]);
            // Beat slowly while flying free; hold still in a glide.
            float beat = player.isFallFlying() ? 0F : t * Mth.sin(ageInTicks * 0.35F) * 0.35F;
            wingsBody.copyFrom(getParentModel().body);
            wingR.yRot = Mth.lerp(t, FOLD_Y, SPREAD_Y);
            wingR.zRot = Mth.lerp(t, FOLD_Z, SPREAD_Z) + beat;
            wingL.yRot = -wingR.yRot;
            wingL.zRot = -wingR.zRot;
            wings.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(WINGS_TEX)), light, overlay);
            wings.render(poseStack, buffer.getBuffer(RenderType.eyes(WINGS_GLOW)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }
}
