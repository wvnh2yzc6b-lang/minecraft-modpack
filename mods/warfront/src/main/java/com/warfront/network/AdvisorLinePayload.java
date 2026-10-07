package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: one of the advisor's lines, shown as a toast (the full line also goes to chat). */
public record AdvisorLinePayload(String title, String line) implements CustomPacketPayload {
    public static final Type<AdvisorLinePayload> TYPE = new Type<>(Warfront.id("advisor_line"));
    public static final StreamCodec<ByteBuf, AdvisorLinePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AdvisorLinePayload::title,
            ByteBufCodecs.STRING_UTF8, AdvisorLinePayload::line,
            AdvisorLinePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdvisorLinePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.warfront.client.ClientHooks.advisorToast(payload.title(), payload.line()));
    }
}
