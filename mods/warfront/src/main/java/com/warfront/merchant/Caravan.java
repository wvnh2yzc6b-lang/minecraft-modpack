package com.warfront.merchant;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.Race;
import com.warfront.registry.WFRegistry;
import com.warfront.war.RaidScheduler;
import com.warfront.war.WarState;
import com.warfront.world.BaseLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * The merchant's caravan: the merchant, a pack llama and two neutral guards. It visits each player's War Standard
 * every 3 to 5 days, stays a day, then leaves. Never during a raid.
 */
public final class Caravan {
    public static final int STAY_TICKS = RaidScheduler.DAY;
    private static final String GUARD_TAG = "warfront_caravan";

    private Caravan() {}

    /** Whether the caravan should arrive now: it's due, the player has a base, and no attack is coming or underway. */
    public static boolean shouldArrive(WarState.Clock c, long now) {
        return c.home != null && c.merchantDue > 0 && now >= c.merchantDue
                && c.pending == WarState.Pending.NONE && c.activeUntil <= now;
    }

    public static long nextVisit(long now, net.minecraft.util.RandomSource r) {
        return now + (3 + r.nextInt(3)) * (long) RaidScheduler.DAY;
    }

    /** Every 10 seconds. */
    public static void tick(MinecraftServer server) {
        WarState war = WarState.get(server);
        long now = server.overworld().getGameTime();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            WarState.Clock c = war.clock(p.getUUID());
            if (c.home == null) continue;
            if (c.merchantDue <= 0) c.merchantDue = nextVisit(now, p.getRandom());
            if (!shouldArrive(c, now)) continue;
            ServerLevel level = RaidScheduler.homeLevel(p, c);
            if (level == null || !level.isLoaded(c.home)) continue;   // wait until the base is loaded
            c.merchantDue = nextVisit(now, p.getRandom());
            if (c.merchantSkip) {
                c.merchantSkip = false;   // still offended
                continue;
            }
            arrive(level, c.home, p);
        }
        // Guards leave with their merchant.
        for (ServerLevel level : server.getAllLevels()) {
            for (SoldierEntity s : level.getEntities(WFRegistry.SOLDIER.get(), e -> e.getPersistentData().hasUUID(GUARD_TAG))) {
                if (!(level.getEntity(s.getPersistentData().getUUID(GUARD_TAG)) instanceof MerchantEntity m) || !m.isAlive()) s.discard();
            }
        }
    }

    /** The caravan arrives beside the War Standard at {@code home}. */
    public static MerchantEntity arrive(ServerLevel level, BlockPos home, ServerPlayer host) {
        BlockPos spot = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, home.offset(4, 0, 4));
        MerchantEntity m = WFRegistry.MERCHANT.get().create(level);
        if (m == null) throw new IllegalStateException("merchant entity type failed to create");
        m.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0F, 0F);
        m.setHostLevel(BaseLevel.of(level, home, Factions.keyOf(level.getServer(), host)).level());
        m.setDespawnDelay(STAY_TICKS);
        m.setWanderTarget(home);
        level.addFreshEntity(m);
        TraderLlama llama = EntityType.TRADER_LLAMA.create(level);
        if (llama != null) {
            llama.moveTo(spot.getX() + 1.5, spot.getY(), spot.getZ() + 0.5, 0F, 0F);
            level.addFreshEntity(llama);
            llama.setLeashedTo(m, true);
        }
        Race race = Race.byId(host.getData(WFRegistry.RACE));
        for (int i = 0; i < 2; i++) {
            SoldierEntity g = WFRegistry.SOLDIER.get().create(level);
            if (g == null) continue;
            g.moveTo(spot.getX() + 0.5 - 1.5 + i * 3, spot.getY(), spot.getZ() - 1.5, 0F, 0F);
            g.setupAsNeutral(race == null ? Race.HUMAN : race, SoldierRole.SPEARMAN, m.position());
            g.getPersistentData().putUUID(GUARD_TAG, m.getUUID());
            level.addFreshEntity(g);
        }
        com.warfront.alert.Alerts.toast(host, "merchant", "A merchant arrives", "At your War Standard for one day. He trades in Mana Crystals.");
        return m;
    }

    /** Struck by a player: the caravan leaves now and skips its next visit. */
    static void insulted(ServerLevel level, MerchantEntity m, ServerPlayer p) {
        WarState.get(level.getServer()).clock(p.getUUID()).merchantSkip = true;
        p.displayClientMessage(Component.literal("The merchant packs up in a hurry. He won't come next time.").withStyle(ChatFormatting.RED), true);
        for (SoldierEntity s : level.getEntitiesOfClass(SoldierEntity.class, new AABB(m.blockPosition()).inflate(32),
                s -> m.getUUID().equals(s.getPersistentData().hasUUID(GUARD_TAG) ? s.getPersistentData().getUUID(GUARD_TAG) : null))) {
            s.discard();
        }
        m.discard();
    }

    /** Test mode: a caravan beside the player now. */
    public static MerchantEntity summonNow(ServerPlayer p) {
        Vec3 at = p.position();
        return arrive(p.serverLevel(), BlockPos.containing(at), p);
    }

    public static boolean isGuard(SoldierEntity s, UUID merchant) {
        return s.getPersistentData().hasUUID(GUARD_TAG) && merchant.equals(s.getPersistentData().getUUID(GUARD_TAG));
    }
}
