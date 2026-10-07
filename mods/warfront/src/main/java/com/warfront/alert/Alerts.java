package com.warfront.alert;

import com.warfront.faction.Race;
import com.warfront.network.AlertPayload;
import com.warfront.registry.WFRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Alerts for players: themed toasts for things that happened to them, and the siege banner for the big moments of a
 * siege. Chat stays for command output (a client option turns the old chat lines back on).
 */
public final class Alerts {
    public enum Kind { TOAST, BANNER }

    /** What was sent, for game tests. */
    public record Sent(UUID player, Kind kind, String key, String title, String text) {}

    /** Game tests set this to a list to capture alerts. */
    @Nullable public static List<Sent> capture;

    private Alerts() {}

    /** One accent color per race, used to tint panels. */
    public static int accent(@Nullable Race race) {
        if (race == null) return 0xC9A45C;
        return switch (race) {
            case HUMAN -> 0x4A7FB5;
            case ELF -> 0x5FA83A;
            case DWARF -> 0xB0793A;
            case ORC -> 0x9E1B1B;
            case DEMON -> 0xE0612A;
            case ANGEL -> 0xE8C547;
            case HIVE -> 0x3FD8D0;
        };
    }

    public static int accentOf(Player player) {
        return accent(Race.byId(player.getData(WFRegistry.RACE)));
    }

    public static void toast(@Nullable Player player, String key, String title, String text) {
        if (player != null) send(player, Kind.TOAST, key, title, text, accentOf(player));
    }

    public static void toast(@Nullable Player player, String key, String title, String text, int accent) {
        if (player != null) send(player, Kind.TOAST, key, title, text, accent);
    }

    /**
     * A toast for a unit's owner. Owners who are offline get nothing (game tests capture it by id instead).
     */
    public static void toastOwner(@Nullable Player online, @Nullable UUID ownerId, String key, String title, String text) {
        if (online != null) toast(online, key, title, text);
        else if (capture != null && ownerId != null) capture.add(new Sent(ownerId, Kind.TOAST, key, title, text));
    }

    public static void banner(@Nullable Player player, String key, String title, String text, int accent) {
        if (player != null) send(player, Kind.BANNER, key, title, text, accent);
    }

    private static void send(Player player, Kind kind, String key, String title, String text, int accent) {
        if (capture != null) capture.add(new Sent(player.getUUID(), kind, key, title, text));
        if (player instanceof ServerPlayer sp && sp.connection != null) {
            PacketDistributor.sendToPlayer(sp, new AlertPayload(kind.ordinal(), title, text, accent));
        }
    }
}
