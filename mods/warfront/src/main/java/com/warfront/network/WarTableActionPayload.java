package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: open the War Table (entity -1), or make one of your units glow for a few seconds. */
public record WarTableActionPayload(int entity) implements CustomPacketPayload {
    public static final Type<WarTableActionPayload> TYPE = new Type<>(Warfront.id("war_table_action"));
    public static final StreamCodec<ByteBuf, WarTableActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WarTableActionPayload::entity, WarTableActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WarTableActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer p)) return;
            if (payload.entity() >= 0 && p.level().getEntity(payload.entity()) instanceof SoldierEntity s && s.isOwnedBy(p)) {
                s.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                return;
            }
            PacketDistributor.sendToPlayer(p, WarTablePayload.of(p));
        });
    }
}
