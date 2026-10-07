package com.warfront.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.warfront.block.ManaPylonBlock;
import com.warfront.block.ManaWellBlock;
import com.warfront.block.SummoningAltarBlock;
import com.warfront.block.TowerBlock;
import com.warfront.block.WarStandardBlock;
import com.warfront.network.ManaInfoPayload;
import com.warfront.network.ManaQueryPayload;
import com.warfront.registry.WFRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The base mana overlay. Looking at a Mana Well, Pylon, tower, altar or War Standard within 8 blocks shows a small
 * panel by the crosshair. Holding a Mana Crystal or the Commander's Baton shows the network: lines between linked
 * wells and pylons, each pylon's reach as a ring on the ground, and wells running dry pulsing red.
 */
public final class ManaOverlay {
    private static final double LOOK_RANGE = 8;
    @Nullable private static ManaInfoPayload info;
    @Nullable private static ManaInfoPayload net;
    @Nullable private static BlockPos asked;
    private static long askedAt;
    private static long netAskedAt;

    private ManaOverlay() {}

    public static void receive(ManaInfoPayload p) {
        if (p.network()) net = p;
        else info = p;
    }

    private static boolean manaBlock(Block b) {
        return b instanceof ManaWellBlock || b instanceof ManaPylonBlock || b instanceof TowerBlock
                || b instanceof SummoningAltarBlock || b instanceof WarStandardBlock;
    }

    private static boolean networkView(Minecraft mc) {
        return mc.player != null && (mc.player.getMainHandItem().is(WFRegistry.MANA_CRYSTAL.get())
                || mc.player.getMainHandItem().is(WFRegistry.COMMANDER_BATON.get()));
    }

    @Nullable
    private static BlockPos target(Minecraft mc) {
        if (mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = hit.getBlockPos();
        if (pos.distToCenterSqr(mc.player.getEyePosition()) > LOOK_RANGE * LOOK_RANGE) return null;
        return manaBlock(mc.level.getBlockState(pos).getBlock()) ? pos : null;
    }

    /** Client tick: ask the server about what we look at (a few times a second at most) and the network nearby. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        BlockPos pos = target(mc);
        if (pos != null && (!pos.equals(asked) || now - askedAt >= 20)) {
            asked = pos;
            askedAt = now;
            PacketDistributor.sendToServer(new ManaQueryPayload(pos, false));
        }
        if (networkView(mc) && now - netAskedAt >= 40) {
            netAskedAt = now;
            PacketDistributor.sendToServer(new ManaQueryPayload(mc.player.blockPosition(), true));
        }
    }

    /** The panel by the crosshair. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || info == null || info.title().isEmpty()) return;
        BlockPos pos = target(mc);
        if (pos == null || !pos.equals(info.pos())) return;
        Font font = mc.font;
        int x = g.guiWidth() / 2 + 12, y = g.guiHeight() / 2 + 6, w = 150;
        int h = 46 + (info.detail().isEmpty() ? 0 : 10);
        WFTheme.panel(g, x, y, w, h, 0x7FD8FF);
        WFTheme.text(g, font, info.title(), x + 5, y + 6, 0xFF7FD8FF);
        int ty = y + 17;
        String amount = info.capacity() > 0 ? info.mana() + " / " + info.capacity() + " mana" : info.mana() + " mana in reach";
        WFTheme.text(g, font, amount, x + 5, ty, WFTheme.TEXT);
        if (info.capacity() > 0) WFTheme.bar(g, x + 5, ty + 10, w - 10, 3, info.mana() / (float) info.capacity(), 0x7FD8FF);
        ty += 15;
        if (!info.detail().isEmpty()) {
            WFTheme.text(g, font, info.detail(), x + 5, ty, WFTheme.MUTED);
            ty += 10;
        }
        String status = info.wells() + " wells, " + info.pylons() + " pylons · " + (info.powered() ? "powered" : "STARVING");
        WFTheme.text(g, font, status, x + 5, ty, info.powered() ? WFTheme.MUTED : 0xFFFF7070);
    }

    /** The network view in the world. */
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (net == null || !networkView(mc) || mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose pose = ps.last();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        List<Long> nodes = net.nodes();
        double reach = 16;
        float pulse = 0.5F + 0.5F * Mth.sin(mc.level.getGameTime() * 0.3F);
        for (int i = 0; i < nodes.size(); i++) {
            BlockPos a = BlockPos.of(nodes.get(i) >> 1);
            Vec3 ca = Vec3.atCenterOf(a);
            for (int j = i + 1; j < nodes.size(); j++) {
                BlockPos b = BlockPos.of(nodes.get(j) >> 1);
                if (a.distSqr(b) > reach * reach) continue;
                line(lines, pose, ca, Vec3.atCenterOf(b), 0xC07FD8FF);
            }
            if ((nodes.get(i) & 1) == 1) ring(lines, pose, Vec3.atBottomCenterOf(a), reach, 0x507FD8FF);
        }
        for (long e : net.empty()) {
            BlockPos p = BlockPos.of(e);
            int alpha = (int) (80 + 175 * pulse);
            Vec3 c = Vec3.atBottomCenterOf(p);
            line(lines, pose, c, c.add(0, 2.5, 0), (alpha << 24) | 0xFF4040);
            ring(lines, pose, c.add(0, 1.02, 0), 0.7, (alpha << 24) | 0xFF4040);
        }
        buffers.endBatch(RenderType.lines());
        ps.popPose();
    }

    private static void line(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, int argb) {
        Vec3 n = b.subtract(a).normalize();
        vc.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(argb).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
        vc.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(argb).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

    private static void ring(VertexConsumer vc, PoseStack.Pose pose, Vec3 c, double r, int argb) {
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            double t0 = i * Mth.TWO_PI / seg, t1 = (i + 1) * Mth.TWO_PI / seg;
            line(vc, pose, c.add(Math.cos(t0) * r, 0.05, Math.sin(t0) * r), c.add(Math.cos(t1) * r, 0.05, Math.sin(t1) * r), argb);
        }
    }
}
