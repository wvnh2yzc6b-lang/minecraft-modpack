package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.army.SoldierRole;
import com.warfront.army.UnitNames;
import com.warfront.block.SummoningAltarBlockEntity;
import com.warfront.faction.Race;
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
 * missing (empty when complete), and every role's unit name, cost and description for the player's race.
 */
public record AltarOpenPayload(BlockPos pos, float mana, String missing, List<String> names, List<Integer> costs,
                               List<String> descriptions) implements CustomPacketPayload {
    public static final Type<AltarOpenPayload> TYPE = new Type<>(Warfront.id("altar_open"));
    public static final StreamCodec<ByteBuf, AltarOpenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AltarOpenPayload::pos,
            ByteBufCodecs.FLOAT, AltarOpenPayload::mana,
            ByteBufCodecs.STRING_UTF8, AltarOpenPayload::missing,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AltarOpenPayload::names,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), AltarOpenPayload::costs,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AltarOpenPayload::descriptions,
            AltarOpenPayload::new);

    public static AltarOpenPayload of(SummoningAltarBlockEntity altar, Player player) {
        Race race = SummoningAltarBlockEntity.raceOf(player);
        List<String> names = new ArrayList<>();
        List<Integer> costs = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        for (SoldierRole role : SoldierRole.values()) {
            names.add(UnitNames.of(race, role));
            // A cost of -1 hides the role: this race has no war beast yet.
            costs.add(role == SoldierRole.BEAST && !UnitNames.hasBeast(race) ? -1 : role.manaCost(race));
            descriptions.add(role.displayName() + ". " + role.description);
        }
        List<String> missing = SummoningAltarBlockEntity.missing(altar.getLevel(), altar.getBlockPos());
        return new AltarOpenPayload(altar.getBlockPos(), altar.availableMana(),
                missing.isEmpty() ? "" : "It needs " + String.join(" and ", missing) + ".", names, costs, descriptions);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AltarOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.warfront.client.ClientHooks.openAltar(payload));
    }
}
