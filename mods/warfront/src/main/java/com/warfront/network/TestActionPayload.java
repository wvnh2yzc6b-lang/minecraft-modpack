package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.test.TestActions;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Client to server: a Test Panel button, as the same words /wftest takes. Re-checked on the server every time. */
public record TestActionPayload(List<String> args) implements CustomPacketPayload {
    public static final Type<TestActionPayload> TYPE = new Type<>(Warfront.id("test_action"));
    public static final StreamCodec<ByteBuf, TestActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(8)), TestActionPayload::args, TestActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TestActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!TestActions.allowed(player)) {
                player.displayClientMessage(Component.literal("Test mode is off (an operator turns it on with /wftest on).")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            TestActions.Result r = TestActions.run(player, payload.args());
            player.displayClientMessage(Component.literal(r.message())
                    .withStyle(r.ok() ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        });
    }
}
