package com.warfront.war;

import com.warfront.army.Order;
import com.warfront.entity.SoldierEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Recall home: during a raid warning or the attack itself, a player may channel for 5 seconds (broken by taking
 * damage) to teleport to their War Standard, with the units following them. Once per attack, free.
 */
public final class Recall {
    public static final int CHANNEL_TICKS = 100;
    /** How far away following units still come along. */
    private static final double FOLLOWER_RANGE = 32;

    private record Channel(long start, float health) {}

    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();

    private Recall() {}

    /** Why the player can't recall now, or null if they can. */
    @Nullable
    public static String refusal(WarState.Clock c, long now) {
        if (c.home == null) return "You have no War Standard to recall to.";
        if (c.pending == WarState.Pending.NONE && c.activeUntil <= now) return "You can only recall home when an attack is coming or underway.";
        if (c.recallUsed) return "You've already recalled home for this attack.";
        return null;
    }

    /** Starts channeling. */
    public static void start(ServerPlayer player) {
        WarState.Clock c = WarState.get(player.server).clock(player.getUUID());
        long now = player.server.overworld().getGameTime();
        String no = refusal(c, now);
        if (no != null) {
            player.displayClientMessage(Component.literal(no).withStyle(ChatFormatting.RED), true);
            return;
        }
        CHANNELS.put(player.getUUID(), new Channel(now, player.getHealth()));
        player.displayClientMessage(Component.literal("Recalling home... hold still for 5 seconds.").withStyle(ChatFormatting.AQUA), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    /** Runs every tick: channels finish, or break when the player is hurt. */
    public static void tick(MinecraftServer server) {
        if (CHANNELS.isEmpty()) return;
        long now = server.overworld().getGameTime();
        CHANNELS.entrySet().removeIf(e -> {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) return true;
            Channel ch = e.getValue();
            if (p.getHealth() < ch.health() - 0.01F) {
                p.displayClientMessage(Component.literal("Recall broken: you took damage.").withStyle(ChatFormatting.RED), true);
                return true;
            }
            p.serverLevel().sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, 0.8, 0.4, 0.2);
            if (now - ch.start() < CHANNEL_TICKS) return false;
            WarState.Clock c = WarState.get(server).clock(p.getUUID());
            ServerLevel home = RaidScheduler.homeLevel(p, c);
            if (home == null || c.home == null || refusal(c, now) != null) return true;
            int units = perform(p, home, c.home, c);
            p.displayClientMessage(Component.literal("Recalled home" + (units > 0 ? " with " + units + " units." : "."))
                    .withStyle(ChatFormatting.AQUA), true);
            return true;
        });
    }

    /** Moves the player, and the units following them, to beside {@code standard}. Uses the attack's recall. */
    public static int perform(Player player, ServerLevel level, BlockPos standard, WarState.Clock c) {
        List<SoldierEntity> followers = new ArrayList<>(player.level().getEntitiesOfClass(SoldierEntity.class,
                player.getBoundingBox().inflate(FOLLOWER_RANGE),
                s -> s.isAlive() && s.isOwnedBy(player) && !s.isPosted() && s.getOrder() == Order.FOLLOW));
        double x = standard.getX() + 1.5, y = standard.getY(), z = standard.getZ() + 0.5;
        player.teleportTo(level, x, y, z, Set.of(), player.getYRot(), player.getXRot());
        int i = 0;
        for (SoldierEntity s : followers) {
            double ox = (i % 4) - 1.5, oz = 1.5 + i / 4;
            s.teleportTo(level, x + ox, y, z + oz, Set.of(), s.getYRot(), s.getXRot());
            i++;
        }
        c.recallUsed = true;
        level.playSound(null, standard, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.9F);
        return followers.size();
    }
}
