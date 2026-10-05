package com.warfront.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** World-wide store of player factions, their members and their diplomacy. */
public class FactionData extends SavedData {
    private static final String NAME = "warfront_factions";
    public static final SavedData.Factory<FactionData> FACTORY =
            new SavedData.Factory<>(FactionData::new, FactionData::load, null);

    public static final class Faction {
        public final String name;
        public UUID leader;
        public ChatFormatting color = ChatFormatting.BLUE;
        public final Set<UUID> members = new HashSet<>();
        public final Set<UUID> invites = new HashSet<>();
        public final Set<String> allies = new HashSet<>();
        public final Set<String> enemies = new HashSet<>();
        /** Factions that proposed an alliance to us. */
        public final Set<String> allyRequests = new HashSet<>();

        Faction(String name, UUID leader) {
            this.name = name;
            this.leader = leader;
            this.members.add(leader);
        }

        public String key() {
            return "f:" + name;
        }
    }

    private final Map<String, Faction> factions = new LinkedHashMap<>();
    private final Map<UUID, String> memberIndex = new HashMap<>();

    public static FactionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public Collection<Faction> all() {
        return factions.values();
    }

    @Nullable
    public Faction byName(String name) {
        return factions.get(name.toLowerCase(Locale.ROOT));
    }

    @Nullable
    public Faction byKey(String key) {
        return key.startsWith("f:") ? factions.get(key.substring(2)) : null;
    }

    @Nullable
    public Faction factionOf(UUID player) {
        String n = memberIndex.get(player);
        return n == null ? null : factions.get(n);
    }

    /** The diplomatic key for a player: their faction, or a personal key when unaffiliated. */
    public String keyOf(UUID player) {
        Faction f = factionOf(player);
        return f != null ? f.key() : "p:" + player;
    }

    public Faction create(String name, UUID leader) {
        String n = name.toLowerCase(Locale.ROOT);
        Faction f = new Faction(n, leader);
        factions.put(n, f);
        memberIndex.put(leader, n);
        setDirty();
        return f;
    }

    public void join(Faction f, UUID player) {
        leave(player);
        f.members.add(player);
        f.invites.remove(player);
        memberIndex.put(player, f.name);
        setDirty();
    }

    /** Removes a player from their faction; disbands it when empty. Returns the faction left, if any. */
    @Nullable
    public Faction leave(UUID player) {
        String n = memberIndex.remove(player);
        if (n == null) return null;
        Faction f = factions.get(n);
        if (f == null) return null;
        f.members.remove(player);
        if (f.members.isEmpty()) {
            disband(f);
        } else if (f.leader.equals(player)) {
            f.leader = f.members.iterator().next();
        }
        setDirty();
        return f;
    }

    public void disband(Faction f) {
        factions.remove(f.name);
        for (UUID m : f.members) memberIndex.remove(m);
        for (Faction other : factions.values()) {
            other.allies.remove(f.key());
            other.enemies.remove(f.key());
            other.allyRequests.remove(f.key());
        }
        setDirty();
    }

    public void declareWar(Faction a, Faction b) {
        a.allies.remove(b.key());
        b.allies.remove(a.key());
        a.allyRequests.remove(b.key());
        b.allyRequests.remove(a.key());
        a.enemies.add(b.key());
        b.enemies.add(a.key());
        setDirty();
    }

    public void makePeace(Faction a, Faction b) {
        a.enemies.remove(b.key());
        b.enemies.remove(a.key());
        setDirty();
    }

    /** Returns true when the alliance is now formed (both sides agreed). */
    public boolean proposeAlliance(Faction from, Faction to) {
        if (from.allyRequests.remove(to.key())) {
            from.enemies.remove(to.key());
            to.enemies.remove(from.key());
            from.allies.add(to.key());
            to.allies.add(from.key());
            setDirty();
            return true;
        }
        to.allyRequests.add(from.key());
        setDirty();
        return false;
    }

    public Relation relation(String a, String b) {
        if (a.equals(b)) return Relation.ALLY;
        Faction fa = byKey(a);
        if (fa == null) return Relation.NEUTRAL;
        if (fa.allies.contains(b)) return Relation.ALLY;
        if (fa.enemies.contains(b)) return Relation.ENEMY;
        return Relation.NEUTRAL;
    }

    // ---- persistence ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Faction f : factions.values()) {
            CompoundTag ft = new CompoundTag();
            ft.putString("name", f.name);
            ft.putUUID("leader", f.leader);
            ft.putString("color", f.color.getName());
            ft.put("members", uuids(f.members));
            ft.put("invites", uuids(f.invites));
            ft.put("allies", strings(f.allies));
            ft.put("enemies", strings(f.enemies));
            ft.put("allyRequests", strings(f.allyRequests));
            list.add(ft);
        }
        tag.put("factions", list);
        return tag;
    }

    public static FactionData load(CompoundTag tag, HolderLookup.Provider registries) {
        FactionData data = new FactionData();
        ListTag list = tag.getList("factions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ft = list.getCompound(i);
            Faction f = new Faction(ft.getString("name"), ft.getUUID("leader"));
            ChatFormatting c = ChatFormatting.getByName(ft.getString("color"));
            if (c != null && c.isColor()) f.color = c;
            readUuids(ft.getList("members", Tag.TAG_INT_ARRAY), f.members);
            readUuids(ft.getList("invites", Tag.TAG_INT_ARRAY), f.invites);
            readStrings(ft.getList("allies", Tag.TAG_STRING), f.allies);
            readStrings(ft.getList("enemies", Tag.TAG_STRING), f.enemies);
            readStrings(ft.getList("allyRequests", Tag.TAG_STRING), f.allyRequests);
            data.factions.put(f.name, f);
            for (UUID m : f.members) data.memberIndex.put(m, f.name);
        }
        return data;
    }

    private static ListTag uuids(Set<UUID> set) {
        ListTag l = new ListTag();
        for (UUID u : set) l.add(NbtUtils.createUUID(u));
        return l;
    }

    private static void readUuids(ListTag l, Set<UUID> out) {
        for (Tag t : l) out.add(NbtUtils.loadUUID(t));
    }

    private static ListTag strings(Set<String> set) {
        ListTag l = new ListTag();
        for (String s : set) l.add(StringTag.valueOf(s));
        return l;
    }

    private static void readStrings(ListTag l, Set<String> out) {
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
    }
}
