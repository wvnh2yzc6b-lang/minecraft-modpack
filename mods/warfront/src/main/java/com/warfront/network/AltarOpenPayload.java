package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.army.SoldierRole;
import com.warfront.army.UnitNames;
import com.warfront.block.SummoningAltarBlockEntity;
import com.warfront.faction.Race;
import com.warfront.war.WarState;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: opens (or refreshes) the summoning menu. Carries the mana in reach, what the altar is
 * missing (empty when complete), every role's unit name, cost and description for the player's race, and the
 * player's returning heroes (name, half cost, seconds until ready).
 */
public record AltarOpenPayload(BlockPos pos, float mana, String missing, List<String> names, List<Integer> costs,
                               List<String> descriptions, List<String> heroNames, List<Integer> heroCosts,
                               List<Integer> heroWait) implements CustomPacketPayload {
    public static final Type<AltarOpenPayload> TYPE = new Type<>(Warfront.id("altar_open"));
    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, List<Integer>> INTS = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());
    public static final StreamCodec<ByteBuf, AltarOpenPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        BlockPos.STREAM_CODEC.encode(buf, p.pos());
        ByteBufCodecs.FLOAT.encode(buf, p.mana());
        ByteBufCodecs.STRING_UTF8.encode(buf, p.missing());
        STRINGS.encode(buf, p.names());
        INTS.encode(buf, p.costs());
        STRINGS.encode(buf, p.descriptions());
        STRINGS.encode(buf, p.heroNames());
        INTS.encode(buf, p.heroCosts());
        INTS.encode(buf, p.heroWait());
    }, buf -> new AltarOpenPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf), STRINGS.decode(buf), INTS.decode(buf), STRINGS.decode(buf),
            STRINGS.decode(buf), INTS.decode(buf), INTS.decode(buf)));

    public static AltarOpenPayload of(SummoningAltarBlockEntity altar, Player player) {
        Race race = SummoningAltarBlockEntity.raceOf(player);
        List<String> names = new ArrayList<>();
        List<Integer> costs = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        for (SoldierRole role : SoldierRole.values()) {
            names.add(UnitNames.of(race, role));
            // A cost of -1 hides the role: a retired role, or this race has no war beast yet.
            costs.add(role.retired() || role == SoldierRole.BEAST && !UnitNames.hasBeast(race) ? -1 : role.manaCost(race));
            descriptions.add(role.displayName() + ". " + role.description);
        }
        List<String> heroNames = new ArrayList<>();
        List<Integer> heroCosts = new ArrayList<>();
        List<Integer> heroWait = new ArrayList<>();
        if (player.getServer() != null) {
            long now = player.level().getGameTime();
            for (WarState.Returning r : WarState.get(player.getServer()).returning(player.getUUID())) {
                heroNames.add(UnitNames.of(r.race(), r.role()));
                heroCosts.add(SummoningAltarBlockEntity.returnCost(r));
                heroWait.add((int) Math.max(0, (r.readyAt() - now) / 20));
            }
        }
        List<String> missing = SummoningAltarBlockEntity.missing(altar.getLevel(), altar.getBlockPos());
        return new AltarOpenPayload(altar.getBlockPos(), altar.availableMana(),
                missing.isEmpty() ? "" : "It needs " + String.join(" and ", missing) + ".", names, costs, descriptions,
                heroNames, heroCosts, heroWait);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AltarOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.warfront.client.ClientHooks.openAltar(payload));
    }
}
