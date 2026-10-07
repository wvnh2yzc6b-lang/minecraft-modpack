package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the orc rocket pack started or stopped thrusting. */
public record RocketPayload(boolean thrusting) implements CustomPacketPayload {
    public static final Type<RocketPayload> TYPE = new Type<>(Warfront.id("rocket"));
    public static final StreamCodec<ByteBuf, RocketPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(RocketPayload::new, RocketPayload::thrusting);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RocketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) com.warfront.flight.Rocketry.setThrusting(player, payload.thrusting());
        });
    }
}
