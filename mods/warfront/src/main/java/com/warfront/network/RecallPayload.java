package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the recall key (H) was pressed. */
public record RecallPayload() implements CustomPacketPayload {
    public static final RecallPayload INSTANCE = new RecallPayload();
    public static final Type<RecallPayload> TYPE = new Type<>(Warfront.id("recall"));
    public static final StreamCodec<ByteBuf, RecallPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RecallPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) com.warfront.war.Recall.start(player);
        });
    }
}
