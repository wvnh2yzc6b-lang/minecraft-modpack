package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.army.Duty;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.army.UnitNames;
import com.warfront.army.Veterancy;
import com.warfront.block.ManaWellBlockEntity;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.mana.ManaNetwork;
import com.warfront.mana.ManaNodeBlockEntity;
import com.warfront.test.TestActions;
import com.warfront.war.WarState;
import com.warfront.world.BaseLevel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the War Table. Units as "entityId|name|role|rank|health%|duty|distance", heroes as
 * "name|status|seconds|place", and the base as plain lines.
 */
public record WarTablePayload(List<String> units, List<String> heroes, List<String> base) implements CustomPacketPayload {
    public static final Type<WarTablePayload> TYPE = new Type<>(Warfront.id("war_table"));
    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
    public static final StreamCodec<ByteBuf, WarTablePayload> STREAM_CODEC = StreamCodec.composite(
            STRINGS, WarTablePayload::units, STRINGS, WarTablePayload::heroes, STRINGS, WarTablePayload::base,
            WarTablePayload::new);

    public static WarTablePayload of(ServerPlayer p) {
        List<String> units = new ArrayList<>();
        List<String> heroes = new ArrayList<>();
        for (SoldierEntity s : TestActions.owned(p)) {
            String duty = s.getDuty() != Duty.NONE ? s.getDuty().title.toLowerCase()
                    : s.getRole().posted() ? "working" : s.getOrder() == Order.FOLLOW ? "following" : "holding";
            int hp = Math.round(100 * s.getHealth() / s.getMaxHealth());
            units.add(s.getId() + "|" + s.getUnitName() + "|" + s.getRole().displayName() + "|" + s.getRank() + "|" + hp + "|"
                    + duty + "|" + Math.round(s.distanceTo(p)));
            if (s.isHero()) {
                String name = (s.getRank() > 0 ? Veterancy.TITLES[s.getRank()] + " " : "") + s.getUnitName();
                heroes.add(name + "|" + (s.isFallen() ? "fallen" : "ready") + "|" + s.fallenSecondsLeft() + "|"
                        + s.blockPosition().toShortString());
            }
        }
        long now = p.level().getGameTime();
        for (WarState.Returning r : WarState.get(p.server).returning(p.getUUID())) {
            String name = (Veterancy.rankFor(r.xp()) > 0 ? Veterancy.TITLES[Veterancy.rankFor(r.xp())] + " " : "") + UnitNames.of(r.race(), r.role());
            heroes.add(name + "|returning|" + Math.max(0, (r.readyAt() - now) / 20) + "|at any altar");
        }

        List<String> base = new ArrayList<>();
        String key = Factions.keyOf(p.server, p);
        BaseLevel.Status st = BaseLevel.of(p.level(), p.blockPosition(), key);
        if (st.buildings() == 0 && !st.isTest()) {
            base.add("No base here: stand within reach of your Mana Wells and Pylons.");
        } else {
            int lvl = st.level();
            base.add("Base level " + lvl + (st.isTest() ? " (TEST)" : ""));
            if (lvl < BaseLevel.MAX_LEVEL) {
                base.add("bar|Buildings|" + st.buildings() + "|" + BaseLevel.buildingsFor(lvl + 1));
                base.add("bar|Waves won|" + st.waves() + "|" + BaseLevel.wavesFor(lvl + 1));
            }
            float mana = 0, cap = 0;
            for (ManaNodeBlockEntity n : ManaNetwork.nodes(p.level(), p.blockPosition(), key)) {
                if (n instanceof ManaWellBlockEntity w) {
                    mana += w.getMana();
                    cap += ManaWellBlockEntity.capacity();
                }
            }
            base.add("bar|Mana|" + Math.round(mana) + "|" + Math.round(cap));
            base.add("War beasts allowed: " + BaseLevel.beastCap(lvl));
            if (lvl < BaseLevel.MAX_LEVEL) {
                List<String> unlocks = new ArrayList<>();
                for (SoldierRole r : SoldierRole.values()) if (BaseLevel.requiredLevel(r) == lvl + 1) unlocks.add(r.displayName() + "s");
                unlocks.add(BaseLevel.beastCap(lvl + 1) + " war beasts");
                base.add("Level " + (lvl + 1) + " unlocks: " + String.join(", ", unlocks));
            }
        }
        return new WarTablePayload(units, heroes, base);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WarTablePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.warfront.client.ClientHooks.openWarTable(payload));
    }
}
