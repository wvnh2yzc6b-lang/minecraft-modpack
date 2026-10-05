package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: the player's current and maximum mana, for the HUD. */
public record ManaPayload(float mana, float max) implements CustomPacketPayload {
    public static final Type<ManaPayload> TYPE = new Type<>(Warfront.id("mana"));
    public static final StreamCodec<ByteBuf, ManaPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ManaPayload::mana,
            ByteBufCodecs.FLOAT, ManaPayload::max,
            ManaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientManaState.mana = payload.mana();
            ClientManaState.max = payload.max();
        });
    }
}
