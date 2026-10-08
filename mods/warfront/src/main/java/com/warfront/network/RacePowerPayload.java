package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the race power key (R) was pressed, with or without sneaking. */
public record RacePowerPayload(boolean sneaking) implements CustomPacketPayload {
    public static final Type<RacePowerPayload> TYPE = new Type<>(Warfront.id("race_power"));
    public static final StreamCodec<ByteBuf, RacePowerPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(RacePowerPayload::new, RacePowerPayload::sneaking);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RacePowerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) com.warfront.combat.RacePower.use(player, payload.sneaking());
        });
    }
}
