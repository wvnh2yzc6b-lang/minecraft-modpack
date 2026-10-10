package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.skill.ClientSkills;
import com.warfront.skill.Renown;
import com.warfront.skill.SkillNode;
import com.warfront.skill.SkillRegistry;
import com.warfront.skill.SkillState;
import com.warfront.skill.Skills;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the player's skill state. Numbers are renown, level, renown into the level, renown for the next
 * level, points, respec cost, then per slot: ticks to the next charge, charges ready, charges in all.
 */
public record SkillStatePayload(List<Integer> numbers, List<String> unlocked, List<String> slots) implements CustomPacketPayload {
    public static final Type<SkillStatePayload> TYPE = new Type<>(Warfront.id("skill_state"));
    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(512));
    public static final StreamCodec<ByteBuf, SkillStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), SkillStatePayload::numbers, STRINGS, SkillStatePayload::unlocked,
            STRINGS, SkillStatePayload::slots, SkillStatePayload::new);

    public static SkillStatePayload of(ServerPlayer p) {
        SkillState s = Skills.state(p);
        int level = s.level();
        List<Integer> n = new ArrayList<>(List.of(s.renown, level, s.renown - Renown.total(level),
                level >= Renown.MAX_LEVEL ? 0 : Renown.toNext(level), s.points(), Skills.respecCost(s)));
        for (String id : s.slots) {
            SkillNode node = id.isEmpty() ? null : SkillRegistry.node(id);
            n.add(node == null ? 0 : (int) Skills.ticksLeft(p, node));
            n.add(node == null ? 0 : Skills.chargesReady(p, node));
            n.add(node == null ? 0 : node.charges());
        }
        return new SkillStatePayload(n, List.copyOf(s.unlocked), List.copyOf(s.slots));
    }

    public int get(int i) {
        return i < numbers.size() ? numbers.get(i) : 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSkills.update(payload, SkillState.SLOTS));
    }
}
