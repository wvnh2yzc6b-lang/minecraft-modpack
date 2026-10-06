package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.army.SoldierRole;
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

/** Client to server: summon this role at the altar the player is using. */
public record AltarSummonPayload(BlockPos pos, int role) implements CustomPacketPayload {
    public static final Type<AltarSummonPayload> TYPE = new Type<>(Warfront.id("altar_summon"));
    public static final StreamCodec<ByteBuf, AltarSummonPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AltarSummonPayload::pos,
            ByteBufCodecs.VAR_INT, AltarSummonPayload::role,
            AltarSummonPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AltarSummonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.distanceToSqr(payload.pos().getCenter()) > 64.0) return;
            if (payload.role() < 0 || payload.role() >= SoldierRole.values().length) return;
            if (!(player.level().getBlockEntity(payload.pos()) instanceof SummoningAltarBlockEntity altar)) return;
            SummoningAltarBlockEntity.Result result = altar.summon(player, SoldierRole.values()[payload.role()]);
            player.displayClientMessage(Component.literal(result.message())
                    .withStyle(result.ok() ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            PacketDistributor.sendToPlayer(player, AltarOpenPayload.of(altar, player));
        });
    }
}
