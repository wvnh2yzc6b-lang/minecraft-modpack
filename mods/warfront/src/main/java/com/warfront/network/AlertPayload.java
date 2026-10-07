package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: a themed toast (kind 0) or the siege banner (kind 1). */
public record AlertPayload(int kind, String title, String text, int accent) implements CustomPacketPayload {
    public static final Type<AlertPayload> TYPE = new Type<>(Warfront.id("alert"));
    public static final StreamCodec<ByteBuf, AlertPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AlertPayload::kind,
            ByteBufCodecs.STRING_UTF8, AlertPayload::title,
            ByteBufCodecs.STRING_UTF8, AlertPayload::text,
            ByteBufCodecs.INT, AlertPayload::accent,
            AlertPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AlertPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.warfront.client.ClientHooks.alert(payload));
    }
}
