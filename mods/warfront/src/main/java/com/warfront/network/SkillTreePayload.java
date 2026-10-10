package com.warfront.network;

import com.warfront.Warfront;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server to client: the skill paths and nodes (see SkillRegistry.encode); opens the Skill Tree when {@code open}. */
public record SkillTreePayload(List<String> lines, boolean open) implements CustomPacketPayload {
    public static final Type<SkillTreePayload> TYPE = new Type<>(Warfront.id("skill_tree"));
    public static final StreamCodec<ByteBuf, SkillTreePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(1024).apply(ByteBufCodecs.list(1024)), SkillTreePayload::lines,
            ByteBufCodecs.BOOL, SkillTreePayload::open, SkillTreePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillTreePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            com.warfront.skill.ClientSkills.setPaths(com.warfront.skill.SkillRegistry.decode(payload.lines()));
            if (payload.open()) com.warfront.client.ClientHooks.openSkillTree();
        });
    }
}
