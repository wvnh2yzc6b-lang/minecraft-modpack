package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.block.SummoningAltarBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: summon returning hero number {@code index} at this altar. */
public record AltarReturnPayload(BlockPos pos, int index) implements CustomPacketPayload {
    public static final Type<AltarReturnPayload> TYPE = new Type<>(Warfront.id("altar_return"));
    public static final StreamCodec<ByteBuf, AltarReturnPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AltarReturnPayload::pos,
            ByteBufCodecs.VAR_INT, AltarReturnPayload::index,
            AltarReturnPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AltarReturnPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(payload.pos().getCenter()) > 64.0) return;
            if (!(player.level().getBlockEntity(payload.pos()) instanceof SummoningAltarBlockEntity altar)) return;
            SummoningAltarBlockEntity.Result result = altar.summonReturning(player, payload.index());
            player.displayClientMessage(Component.literal(result.message())
                    .withStyle(result.ok() ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            PacketDistributor.sendToPlayer(player, AltarOpenPayload.of(altar, player));
        });
    }
}
