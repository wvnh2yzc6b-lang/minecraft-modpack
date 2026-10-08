package com.warfront.network;

import com.warfront.Warfront;
import com.warfront.block.ManaPylonBlockEntity;
import com.warfront.block.ManaWellBlockEntity;
import com.warfront.block.SummoningAltarBlockEntity;
import com.warfront.block.TowerBlock;
import com.warfront.block.TowerBlockEntity;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.faction.Factions;
import com.warfront.mana.ManaNetwork;
import com.warfront.mana.ManaNodeBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Client to server: what is this mana block (network false), or show me the mana network around me (network true).
 * Answered with a {@link ManaInfoPayload}. On demand only: nothing is broadcast.
 */
public record ManaQueryPayload(BlockPos pos, boolean network) implements CustomPacketPayload {
    public static final Type<ManaQueryPayload> TYPE = new Type<>(Warfront.id("mana_query"));
    public static final StreamCodec<ByteBuf, ManaQueryPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ManaQueryPayload::pos, ByteBufCodecs.BOOL, ManaQueryPayload::network, ManaQueryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManaQueryPayload q, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer p) || p.blockPosition().distSqr(q.pos()) > 24 * 24) return;
            PacketDistributor.sendToPlayer(p, q.network() ? network(p) : describe(p, q.pos()));
        });
    }

    private static ManaInfoPayload network(ServerPlayer p) {
        List<Long> nodes = new ArrayList<>();
        List<Long> empty = new ArrayList<>();
        for (ManaNodeBlockEntity n : ManaNetwork.near(p.level(), p.blockPosition(), 48)) {
            if (Factions.relation(p.server, n.factionKey(p.server), Factions.keyOf(p.server, p)) != com.warfront.faction.Relation.ALLY) continue;
            long packed = n.getBlockPos().asLong() << 1 | (n instanceof ManaPylonBlockEntity ? 1 : 0);
            nodes.add(packed);
            if (n instanceof ManaWellBlockEntity w && w.getMana() < ManaWellBlockEntity.SHARD_MANA) empty.add(n.getBlockPos().asLong());
        }
        return new ManaInfoPayload(p.blockPosition(), true, "", "", 0, 0, 0, 0, true, nodes, empty);
    }

    private static ManaInfoPayload describe(ServerPlayer p, BlockPos pos) {
        BlockEntity be = p.level().getBlockEntity(pos);
        String key;
        String title;
        String detail = "";
        int mana, cap = 0;
        boolean powered;
        if (be instanceof ManaWellBlockEntity w) {
            key = w.factionKey(p.server);
            title = w.isSunwell() ? "Sunwell" : "Mana Well";
            if (w.isSunwell()) detail = "Gathers mana by day under open sky";
            mana = Math.round(w.getMana());
            cap = Math.round(ManaWellBlockEntity.capacity());
            powered = mana > 0;
        } else if (be instanceof ManaPylonBlockEntity pylon) {
            key = pylon.factionKey(p.server);
            title = "Mana Pylon";
            mana = Math.round(ManaNetwork.available(p.level(), pos, key));
            powered = mana > 0;
        } else if (be instanceof TowerBlockEntity tower && p.level().getBlockState(pos).getBlock() instanceof TowerBlock tb) {
            key = tower.factionKey(p.server);
            title = p.level().getBlockState(pos).getBlock().getName().getString();
            mana = Math.round(ManaNetwork.available(p.level(), pos, key));
            detail = tb.getTowerType().manaCost + " mana per shot";
            powered = mana >= tb.getTowerType().manaCost;
        } else if (be instanceof SummoningAltarBlockEntity altar) {
            key = altar.factionKey(p.server);
            title = "Summoning Altar";
            mana = Math.round(altar.availableMana());
            powered = mana > 0;
        } else if (be instanceof WarStandardBlockEntity standard) {
            key = standard.factionKey(p.server);
            title = "War Standard";
            mana = Math.round(ManaNetwork.available(p.level(), pos, key));
            detail = "Waves won: " + standard.getWavesWon();
            powered = mana > 0;
        } else {
            return new ManaInfoPayload(pos, false, "", "", 0, 0, 0, 0, false, List.of(), List.of());
        }
        int wells = 0, pylons = 0;
        for (ManaNodeBlockEntity n : ManaNetwork.nodes(p.level(), pos, key)) {
            if (n instanceof ManaWellBlockEntity) wells++;
            else pylons++;
        }
        return new ManaInfoPayload(pos, false, title, detail, mana, cap, wells, pylons, powered, List.of(), List.of());
    }
}
