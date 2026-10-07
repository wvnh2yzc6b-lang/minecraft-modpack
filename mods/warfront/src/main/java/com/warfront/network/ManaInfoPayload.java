package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server to client: one mana block's details (title, mana, capacity or 0, wells and pylons on its network, powered),
 * or, for a network query, the nearby nodes (position << 1 | isPylon) and the wells running dry.
 */
public record ManaInfoPayload(BlockPos pos, boolean network, String title, String detail, int mana, int capacity, int wells,
                              int pylons, boolean powered, List<Long> nodes, List<Long> empty) implements CustomPacketPayload {
    public static final Type<ManaInfoPayload> TYPE = new Type<>(Warfront.id("mana_info"));
    private static final StreamCodec<ByteBuf, List<Long>> LONGS = ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list());
    public static final StreamCodec<ByteBuf, ManaInfoPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        BlockPos.STREAM_CODEC.encode(buf, p.pos());
        ByteBufCodecs.BOOL.encode(buf, p.network());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.title());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.detail());
        ByteBufCodecs.VAR_INT.encode(buf, p.mana());
        ByteBufCodecs.VAR_INT.encode(buf, p.capacity());
        ByteBufCodecs.VAR_INT.encode(buf, p.wells());
        ByteBufCodecs.VAR_INT.encode(buf, p.pylons());
        ByteBufCodecs.BOOL.encode(buf, p.powered());
        LONGS.encode(buf, p.nodes());
        LONGS.encode(buf, p.empty());
    }, buf -> new ManaInfoPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.BOOL.decode(buf), LONGS.decode(buf), LONGS.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManaInfoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.warfront.client.ui.ManaOverlay.receive(payload));
    }
}
