package com.warfront.network;

import com.warfront.registry.WFRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Keeps every client informed of every player's race. */
public final class RaceSync {
    private RaceSync() {}

    /** Tells everyone about {@code player}'s race. */
    public static void broadcast(ServerPlayer player) {
        PacketDistributor.sendToAllPlayers(new RacePayload(player.getUUID(), player.getData(WFRegistry.RACE)));
    }

    /** Tells {@code joiner} the races of everyone already online. */
    public static void sendAllTo(ServerPlayer joiner) {
        if (joiner.getServer() == null) return;
        for (ServerPlayer other : joiner.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(joiner, new RacePayload(other.getUUID(), other.getData(WFRegistry.RACE)));
        }
    }
}
