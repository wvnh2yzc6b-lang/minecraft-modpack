package com.warfront.upkeep;

import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import com.warfront.network.AdvisorLinePayload;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Food upkeep. Each morning every battle unit near a Mess Hall eats its share and is Well Fed until the next
 * morning. Units that were away eat when they come back. Hungry units just miss the bonus: no damage, no desertion.
 * Workers don't eat.
 */
public final class Upkeep {
    private static final String FED_DAY = "warfront_fed_day";
    /** A Well Fed effect lasts until a little past the next morning. */
    public static final int WELL_FED_TICKS = 24000 + 1200;

    private Upkeep() {}

    public static int pointsPerUnit() {
        return WFConfig.FOOD_PER_UNIT.get();
    }

    public static long today(ServerLevel level) {
        return level.getServer().overworld().getDayTime() / 24000L;
    }

    public static boolean fedToday(SoldierEntity s, long day) {
        return s.getPersistentData().contains(FED_DAY) && s.getPersistentData().getLong(FED_DAY) >= day;
    }

    /**
     * Feeds every unit in range of {@code hall} that hasn't eaten today. Returns the commanders whose troops went
     * hungry because the hall ran out.
     */
    public static Set<UUID> feed(MessHallBlockEntity hall, long day) {
        Set<UUID> hungry = new HashSet<>();
        for (SoldierEntity s : hall.troops()) {
            if (fedToday(s, day)) continue;
            if (hall.eat(pointsPerUnit())) {
                s.getPersistentData().putLong(FED_DAY, day);
                s.addEffect(new MobEffectInstance(WFRegistry.WELL_FED, WELL_FED_TICKS, 0, true, true));
            } else if (s.getOwnerUUID() != null) {
                hungry.add(s.getOwnerUUID());
            }
        }
        return hungry;
    }

    private static long lastDay = -1;

    /** Every 10 seconds: the morning meal for anyone who hasn't eaten today. */
    public static void tick(MinecraftServer server) {
        long day = server.overworld().getDayTime() / 24000L;
        boolean morning = day != lastDay;
        lastDay = day;
        Set<UUID> hungry = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (BlockPos pos : new ArrayList<>(MessHallBlockEntity.loaded(level))) {
                if (level.getBlockEntity(pos) instanceof MessHallBlockEntity hall) hungry.addAll(feed(hall, day));
            }
        }
        if (!morning) return;
        for (UUID id : hungry) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) PacketDistributor.sendToPlayer(p, new AdvisorLinePayload("Your troops went hungry",
                    "The Mess Hall ran out. Fed troops hold their nerve better."));
        }
    }

    /** Test mode: fills (with bread) or empties every Mess Hall within {@code radius}. */
    public static int fillOrEmpty(Player player, boolean fill, double radius) {
        int n = 0;
        if (!(player.level() instanceof ServerLevel level)) return 0;
        for (BlockPos pos : new ArrayList<>(MessHallBlockEntity.loaded(level))) {
            if (pos.distToCenterSqr(player.position()) > radius * radius) continue;
            if (!(level.getBlockEntity(pos) instanceof MessHallBlockEntity hall)) continue;
            if (fill) {
                for (int i = 0; i < hall.getContainerSize(); i++) {
                    hall.setItem(i, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 64));
                }
            } else {
                hall.empty();
            }
            n++;
        }
        return n;
    }
}
