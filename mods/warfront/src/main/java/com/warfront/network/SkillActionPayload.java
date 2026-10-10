package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.skill.SkillRegistry;
import com.warfront.skill.Skills;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Client to server, skill tree actions: "open", "learn &lt;node&gt;", "slot &lt;i&gt; &lt;node or empty&gt;", "respec",
 * "use &lt;slot&gt; &lt;forward&gt; &lt;strafe&gt;" (a skill key; the movement keys aim a Dash).
 */
public record SkillActionPayload(List<String> args) implements CustomPacketPayload {
    public static final Type<SkillActionPayload> TYPE = new Type<>(Warfront.id("skill_action"));
    public static final StreamCodec<ByteBuf, SkillActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(6)), SkillActionPayload::args, SkillActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static String arg(List<String> a, int i) {
        return i < a.size() ? a.get(i) : "";
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static float flt(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return 0F;
        }
    }

    public static void handle(SkillActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer p)) return;
            List<String> a = payload.args();
            Skills.Result r = switch (arg(a, 0)) {
                case "open" -> {
                    PacketDistributor.sendToPlayer(p, new SkillTreePayload(SkillRegistry.encode(), true));
                    Skills.sync(p);
                    yield null;
                }
                case "learn" -> Skills.learn(p, arg(a, 1));
                case "slot" -> Skills.setSlot(p, num(arg(a, 1)), arg(a, 2));
                case "respec" -> Skills.respec(p, false);
                case "use" -> Skills.use(p, num(arg(a, 1)), flt(arg(a, 2)), flt(arg(a, 3)));
                default -> null;
            };
            if (r != null && (!r.ok() || !arg(a, 0).equals("use"))) {
                p.displayClientMessage(Component.literal(r.message()).withStyle(r.ok() ? ChatFormatting.GREEN : ChatFormatting.RED), true);
            }
        });
    }
}
