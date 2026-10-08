package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server to client, once a second: what the combat HUD shows. Packed as ints: souls, rage, frenzy ticks left,
 * units following, army total, formation, order, on guard, on patrol, beasts, beast cap, siege wave (0 none),
 * enemies left, resolve, Oath of Stone ticks left, valor, radiance, swarm.
 */
public record HudPayload(List<Integer> values) implements CustomPacketPayload {
    public static final Type<HudPayload> TYPE = new Type<>(Warfront.id("hud"));
    public static final StreamCodec<ByteBuf, HudPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(32)), HudPayload::values, HudPayload::new);

    public static final int SOULS = 0, RAGE = 1, FRENZY = 2, FOLLOWING = 3, TOTAL = 4, FORMATION = 5, ORDER = 6,
            GUARD = 7, PATROL = 8, BEASTS = 9, BEAST_CAP = 10, WAVE = 11, LEFT = 12, RESOLVE = 13, OATH = 14, VALOR = 15, RADIANCE = 16, SWARM = 17, SIZE = 18;

    /** The latest values on this client (plain data; safe on either side). */
    public static volatile int[] client = new int[SIZE];

    public int get(int i) {
        return i < values.size() ? values.get(i) : 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            int[] v = new int[SIZE];
            for (int i = 0; i < SIZE; i++) v[i] = payload.get(i);
            client = v;
        });
    }
}
