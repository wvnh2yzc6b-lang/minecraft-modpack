package com.warfront.war;

import com.warfront.config.WFConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Per-world war pacing: the difficulty preset and each player's raid clock, warning and home. */
public class WarState extends SavedData {
    private static final String NAME = "warfront_war";
    public static final SavedData.Factory<WarState> FACTORY = new SavedData.Factory<>(WarState::new, WarState::load, null);

    /** Difficulty presets. Their numbers live in the config (pacing.easy, .normal, ...). */
    public enum Preset {
        EASY, NORMAL, HARD, WARLORD;

        public double raidSize() {
            return WFConfig.PRESET_RAID_SIZE[ordinal()].get();
        }

        public double strength() {
            return WFConfig.PRESET_STRENGTH[ordinal()].get();
        }

        public double raidsPerDay() {
            return WFConfig.PRESET_RAIDS_PER_DAY[ordinal()].get();
        }

        /** On Warlord, fallen heroes stay dead. */
        public boolean heroesStayDead() {
            return this == WARLORD;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String title() {
            return name().charAt(0) + id().substring(1);
        }

        @Nullable
        public static Preset byId(String id) {
            for (Preset p : values()) if (p.id().equalsIgnoreCase(id)) return p;
            return null;
        }
    }

    /** What a player's coming attack is. */
    public enum Pending { NONE, RAID, SIEGE }

    /** One player's raid clock. Progress is in ticks of clock time, which runs slower away from home. */
    public static class Clock {
        public double raidProgress;
        public double raidDue = -1;
        public double siegeProgress;
        public double siegeDue = -1;
        /** Raids since the last quiet day; every second raid adds a quiet day. */
        public int raidsInARow;
        public Pending pending = Pending.NONE;
        /** Game time the warned attack hits. */
        public long hitsAt;
        /** Faction (NpcFaction name) of the warned attack. */
        public String faction = "";
        /** Game time until which the attack counts as underway (for the recall). */
        public long activeUntil;
        public boolean recallUsed;
        /** The faction a raider camp says is coming next (empty: any). */
        public String nextFaction = "";
        /** A camp of this faction was taken: its next raid comes a third smaller. */
        public String shrinkFaction = "";
        /** When the merchant's caravan comes next (0: not scheduled), and whether it skips that visit. */
        public long merchantDue;
        public boolean merchantSkip;
        @Nullable public BlockPos home;
        @Nullable public ResourceKey<Level> homeDim;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putDouble("RaidProgress", raidProgress);
            t.putDouble("RaidDue", raidDue);
            t.putDouble("SiegeProgress", siegeProgress);
            t.putDouble("SiegeDue", siegeDue);
            t.putInt("InARow", raidsInARow);
            t.putString("Pending", pending.name());
            t.putLong("HitsAt", hitsAt);
            t.putString("Faction", faction);
            t.putLong("ActiveUntil", activeUntil);
            t.putBoolean("RecallUsed", recallUsed);
            t.putString("NextFaction", nextFaction);
            t.putString("ShrinkFaction", shrinkFaction);
            t.putLong("MerchantDue", merchantDue);
            t.putBoolean("MerchantSkip", merchantSkip);
            if (home != null && homeDim != null) {
                t.putLong("Home", home.asLong());
                t.putString("HomeDim", homeDim.location().toString());
            }
            return t;
        }

        static Clock load(CompoundTag t) {
            Clock c = new Clock();
            c.raidProgress = t.getDouble("RaidProgress");
            c.raidDue = t.contains("RaidDue") ? t.getDouble("RaidDue") : -1;
            c.siegeProgress = t.getDouble("SiegeProgress");
            c.siegeDue = t.contains("SiegeDue") ? t.getDouble("SiegeDue") : -1;
            c.raidsInARow = t.getInt("InARow");
            try {
                c.pending = Pending.valueOf(t.getString("Pending"));
            } catch (IllegalArgumentException e) {
                c.pending = Pending.NONE;
            }
            c.hitsAt = t.getLong("HitsAt");
            c.faction = t.getString("Faction");
            c.activeUntil = t.getLong("ActiveUntil");
            c.recallUsed = t.getBoolean("RecallUsed");
            c.nextFaction = t.getString("NextFaction");
            c.shrinkFaction = t.getString("ShrinkFaction");
            c.merchantDue = t.getLong("MerchantDue");
            c.merchantSkip = t.getBoolean("MerchantSkip");
            if (t.contains("Home")) {
                c.home = BlockPos.of(t.getLong("Home"));
                c.homeDim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("HomeDim")));
            }
            return c;
        }
    }

    /** A hero carried from the field, waiting to be summoned again at half cost. */
    public record Returning(com.warfront.army.SoldierRole role, com.warfront.faction.Race race, int xp, long readyAt) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("Role", role.name());
            t.putString("Race", race.id());
            t.putInt("Xp", xp);
            t.putLong("Ready", readyAt);
            return t;
        }

        @Nullable
        static Returning load(CompoundTag t) {
            try {
                com.warfront.faction.Race r = com.warfront.faction.Race.byId(t.getString("Race"));
                return new Returning(com.warfront.army.SoldierRole.valueOf(t.getString("Role")),
                        r == null ? com.warfront.faction.Race.HUMAN : r, t.getInt("Xp"), t.getLong("Ready"));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private final Map<UUID, java.util.List<Returning>> returning = new HashMap<>();
    private final Map<UUID, java.util.List<Bounties.Bounty>> bounties = new HashMap<>();
    private final java.util.List<Bounties.Camp> camps = new java.util.ArrayList<>();

    /** The player's open bounties (a live list: change it, then setDirty). */
    public java.util.List<Bounties.Bounty> bounties(UUID player) {
        return bounties.computeIfAbsent(player, k -> new java.util.ArrayList<>());
    }

    public java.util.List<Bounties.Camp> camps() {
        return camps;
    }

    public void addCamp(Bounties.Camp camp) {
        camps.add(camp);
        setDirty();
    }

    public void removeCamp(Bounties.Camp camp) {
        camps.remove(camp);
        setDirty();
    }

    public java.util.List<Returning> returning(UUID player) {
        return returning.getOrDefault(player, java.util.List.of());
    }

    public void addReturning(UUID player, Returning hero) {
        returning.computeIfAbsent(player, k -> new java.util.ArrayList<>()).add(hero);
        setDirty();
    }

    @Nullable
    public Returning takeReturning(UUID player, int index) {
        java.util.List<Returning> list = returning.get(player);
        if (list == null || index < 0 || index >= list.size()) return null;
        setDirty();
        return list.remove(index);
    }

    private Preset preset = Preset.NORMAL;
    private boolean presetChosen;
    /** Test mode: the grace period is over. */
    private boolean graceSkipped;
    private final Map<UUID, Clock> clocks = new HashMap<>();

    public static WarState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public Preset preset() {
        return preset;
    }

    public boolean presetChosen() {
        return presetChosen;
    }

    public void setPreset(Preset preset) {
        this.preset = preset;
        this.presetChosen = true;
        setDirty();
    }

    public boolean graceSkipped() {
        return graceSkipped;
    }

    public void skipGrace() {
        graceSkipped = true;
        setDirty();
    }

    public Clock clock(UUID player) {
        setDirty();
        return clocks.computeIfAbsent(player, k -> new Clock());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Preset", preset.name());
        tag.putBoolean("Chosen", presetChosen);
        tag.putBoolean("GraceSkipped", graceSkipped);
        ListTag list = new ListTag();
        clocks.forEach((id, c) -> {
            CompoundTag t = c.save();
            t.putUUID("Player", id);
            list.add(t);
        });
        tag.put("Clocks", list);
        ListTag heroes = new ListTag();
        returning.forEach((id, l) -> l.forEach(r -> {
            CompoundTag t = r.save();
            t.putUUID("Player", id);
            heroes.add(t);
        }));
        tag.put("Returning", heroes);
        ListTag offers = new ListTag();
        bounties.forEach((id, l) -> l.forEach(b -> {
            CompoundTag t = b.save();
            t.putUUID("Player", id);
            offers.add(t);
        }));
        tag.put("Bounties", offers);
        ListTag campTags = new ListTag();
        camps.forEach(c -> campTags.add(c.save()));
        tag.put("Camps", campTags);
        return tag;
    }

    public static WarState load(CompoundTag tag, HolderLookup.Provider registries) {
        WarState s = new WarState();
        Preset p = Preset.byId(tag.getString("Preset"));
        s.preset = p == null ? Preset.NORMAL : p;
        s.presetChosen = tag.getBoolean("Chosen");
        s.graceSkipped = tag.getBoolean("GraceSkipped");
        for (Tag t : tag.getList("Clocks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.hasUUID("Player")) s.clocks.put(c.getUUID("Player"), Clock.load(c));
        }
        for (Tag t : tag.getList("Returning", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Returning r = Returning.load(c);
            if (r != null && c.hasUUID("Player")) s.returning.computeIfAbsent(c.getUUID("Player"), k -> new java.util.ArrayList<>()).add(r);
        }
        for (Tag t : tag.getList("Bounties", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Bounties.Bounty b = Bounties.Bounty.load(c);
            if (b != null && c.hasUUID("Player")) s.bounties(c.getUUID("Player")).add(b);
        }
        for (Tag t : tag.getList("Camps", Tag.TAG_COMPOUND)) {
            Bounties.Camp c = Bounties.Camp.load((CompoundTag) t);
            if (c != null) s.camps.add(c);
        }
        return s;
    }
}
