package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Server to client: a player's race, so every client can draw demon players (and future races) correctly. */
public record RacePayload(UUID player, String race) implements CustomPacketPayload {
    public static final Type<RacePayload> TYPE = new Type<>(Warfront.id("race"));
    public static final StreamCodec<ByteBuf, RacePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RacePayload::player,
            ByteBufCodecs.STRING_UTF8, RacePayload::race,
            RacePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRaceState.set(payload.player(), payload.race()));
    }
}
