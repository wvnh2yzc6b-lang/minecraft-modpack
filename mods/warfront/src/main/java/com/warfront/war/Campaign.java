package com.warfront.war;

import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.registry.WFRegistry;
import com.warfront.world.BaseLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The campaign against the seven factions. Raids come from factions picked at random but kept mixed; beating a
 * faction's raids fills its war meter, and once it is full (at base level 3+) the last raider of the next raid you
 * beat drops that faction's War Map. Your own race's renegades only raid once the other six warlords have fallen.
 */
public final class Campaign {
    /** Live raids, by warband: who they were sent against and from which faction. */
    private record Raid(UUID player, NpcFaction faction) {}

    private static final Map<UUID, Raid> RAIDS = new HashMap<>();
    private static final String MAP_FACTION = "warfront_war_map";

    private Campaign() {}

    public static void clearAll() {
        RAIDS.clear();
    }

    // ------------------------------------------------------------------ meters and warlords (on the player)

    public static int meter(Player p, NpcFaction f) {
        return p.getData(WFRegistry.WAR_METERS).getInt(f.name());
    }

    public static void setMeter(Player p, NpcFaction f, int value) {
        CompoundTag t = p.getData(WFRegistry.WAR_METERS).copy();
        t.putInt(f.name(), Math.max(0, value));
        p.setData(WFRegistry.WAR_METERS, t);
    }

    public static boolean warlordBeaten(Player p, NpcFaction f) {
        return (p.getData(WFRegistry.WARLORDS_BEATEN) & (1 << f.ordinal())) != 0;
    }

    public static void setWarlordBeaten(Player p, NpcFaction f, boolean beaten) {
        int mask = p.getData(WFRegistry.WARLORDS_BEATEN);
        p.setData(WFRegistry.WARLORDS_BEATEN, beaten ? mask | (1 << f.ordinal()) : mask & ~(1 << f.ordinal()));
    }

    public static int warlordsBeaten(Player p) {
        return Integer.bitCount(p.getData(WFRegistry.WARLORDS_BEATEN));
    }

    /** The faction made of the player's own race's renegades, or null (none for a race without one). */
    @Nullable
    public static NpcFaction renegades(@Nullable Race race) {
        if (race == null) return null;
        for (NpcFaction f : NpcFaction.values()) if (f.race == race) return f;
        return null;
    }

    /** Whether this faction may raid the player yet: renegades wait until the other six warlords are down. */
    public static boolean mayRaid(Player p, NpcFaction f) {
        NpcFaction own = renegades(Race.byId(p.getData(WFRegistry.RACE)));
        if (f != own) return true;
        int others = 0;
        for (NpcFaction o : NpcFaction.values()) if (o != own && warlordBeaten(p, o)) others++;
        return others >= NpcFaction.values().length - 1;
    }

    // ------------------------------------------------------------------ picking the next raider

    /**
     * Picks the next raiding faction for this player: random, but no faction more than three times in a row, and
     * factions not seen for a while grow more likely.
     */
    public static NpcFaction pick(Player p, WarState.Clock c, RandomSource r) {
        List<String> recent = c.recentFactions;
        double[] weights = new double[NpcFaction.values().length];
        double total = 0;
        for (NpcFaction f : NpcFaction.values()) {
            if (!mayRaid(p, f)) continue;
            int since = recent.lastIndexOf(f.name());
            int gap = since < 0 ? recent.size() + 2 : recent.size() - 1 - since;
            double w = 1.0 + 0.5 * Math.min(8, gap);
            if (recent.size() >= 3 && recent.subList(recent.size() - 3, recent.size()).stream().allMatch(f.name()::equals)) w = 0;
            weights[f.ordinal()] = w;
            total += w;
        }
        NpcFaction picked = NpcFaction.MARAUDERS;
        if (total > 0) {
            double roll = r.nextDouble() * total;
            for (NpcFaction f : NpcFaction.values()) {
                roll -= weights[f.ordinal()];
                if (weights[f.ordinal()] > 0 && roll < 0) {
                    picked = f;
                    break;
                }
            }
        }
        remember(c, picked);
        return picked;
    }

    public static void remember(WarState.Clock c, NpcFaction f) {
        c.recentFactions.add(f.name());
        while (c.recentFactions.size() > 12) c.recentFactions.remove(0);
    }

    // ------------------------------------------------------------------ raids beaten

    /** A raid was sent at this player: track it so beating it counts. */
    public static void track(UUID warband, UUID player, NpcFaction faction) {
        RAIDS.put(warband, new Raid(player, faction));
    }

    /** A raider died. If it was its raid's last, the raid is beaten: the meter rises, and maybe a War Map drops. */
    public static void raiderDied(SoldierEntity raider) {
        UUID band = raider.getWarbandId();
        if (band == null || !(raider.level() instanceof ServerLevel level)) return;
        Raid raid = RAIDS.get(band);
        if (raid == null) return;
        int left = level.getEntitiesOfClass(SoldierEntity.class, raider.getBoundingBox().inflate(160),
                s -> s != raider && s.isAlive() && band.equals(s.getWarbandId())).size();
        if (left > 0) return;
        RAIDS.remove(band);
        Player p = level.getServer().getPlayerList().getPlayer(raid.player());
        if (p == null) return;
        raidBeaten(p, raid.faction(), raider);
    }

    /** Counts a beaten raid for the player; drops the War Map from {@code last} when the meter is full. */
    public static boolean raidBeaten(Player p, NpcFaction f, @Nullable SoldierEntity last) {
        int before = meter(p, f);
        setMeter(p, f, before + 1);
        boolean drop = before >= WFConfig.WAR_MAP_RAIDS.get() && !warlordBeaten(p, f) && baseLevel(p) >= 3;
        if (drop && last != null) {
            last.spawnAtLocation(warMap(f));
            com.warfront.alert.Alerts.toast(p, "war_map", "A War Map!", "It leads to the " + f.displayName + "' warlord.");
        } else {
            com.warfront.alert.Alerts.toast(p, "raid_beaten", "Raid beaten", f.displayName + " war meter " + Math.min(before + 1,
                    WFConfig.WAR_MAP_RAIDS.get()) + "/" + WFConfig.WAR_MAP_RAIDS.get());
        }
        return drop;
    }

    private static int baseLevel(Player p) {
        WarState.Clock c = WarState.get(p.getServer()).clock(p.getUUID());
        if (c.home == null) return BaseLevel.of(p.level(), p.blockPosition(), Factions.keyOf(p.getServer(), p)).level();
        ServerLevel home = p.getServer().getLevel(c.homeDim);
        return home == null ? 1 : BaseLevel.of(home, c.home, Factions.keyOf(p.getServer(), p)).level();
    }

    // ------------------------------------------------------------------ War Maps

    public static ItemStack warMap(NpcFaction f) {
        ItemStack map = new ItemStack(WFRegistry.WAR_MAP.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(MAP_FACTION, f.name());
        map.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        map.set(DataComponents.CUSTOM_NAME, Component.literal("War Map: " + f.displayName).withStyle(f.color));
        return map;
    }

    @Nullable
    public static NpcFaction mapFaction(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        String name = data.copyTag().getString(MAP_FACTION);
        for (NpcFaction f : NpcFaction.values()) if (f.name().equals(name)) return f;
        return null;
    }

    /** Lines for /wftest war: each faction's meter and warlord. */
    public static List<Component> status(Player p) {
        List<Component> out = new ArrayList<>();
        for (NpcFaction f : NpcFaction.values()) {
            out.add(Component.literal(f.displayName + ": " + meter(p, f) + "/" + WFConfig.WAR_MAP_RAIDS.get()
                    + (warlordBeaten(p, f) ? "  (warlord beaten)" : "") + (mayRaid(p, f) ? "" : "  (renegades: waiting)"))
                    .withStyle(f.color));
        }
        return out;
    }

    /** Raiders of a warband near {@code pos} (for tests). */
    public static int alive(ServerLevel level, UUID band, AABB box) {
        return level.getEntitiesOfClass(SoldierEntity.class, box, s -> s.isAlive() && band.equals(s.getWarbandId())).size();
    }
}
