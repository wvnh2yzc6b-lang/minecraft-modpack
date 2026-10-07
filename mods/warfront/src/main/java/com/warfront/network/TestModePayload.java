package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: whether test mode is on for this player, so F8 knows to open the Test Panel. */
public record TestModePayload(boolean on) implements CustomPacketPayload {
    public static final Type<TestModePayload> TYPE = new Type<>(Warfront.id("test_mode"));
    public static final StreamCodec<ByteBuf, TestModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TestModePayload::on, TestModePayload::new);

    /** Plain client-side flag; safe to load on either side. */
    public static volatile boolean clientOn;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TestModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientOn = payload.on());
    }
}
