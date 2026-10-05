package com.warfront.faction;

import com.warfront.entity.SoldierEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Central diplomacy rules: who is friend and who is foe. */
public final class Factions {
    public static final String MONSTERS = "monsters";
    public static final String WILD = "wild";
    /** Persistent-data key used to tag projectiles fired by towers with their owner's faction. */
    public static final String PROJECTILE_TAG = "warfront_faction";

    private Factions() {}

    /** Diplomatic key of an entity, or {@link #WILD} for things that take no side. */
    public static String keyOf(@Nullable MinecraftServer server, Entity e) {
        if (e instanceof SoldierEntity s) return s.getFactionKey();
        if (e instanceof Player p) {
            return server == null ? "p:" + p.getUUID() : FactionData.get(server).keyOf(p.getUUID());
        }
        if (e instanceof Enemy) return MONSTERS;
        return WILD;
    }

    public static Relation relation(@Nullable MinecraftServer server, String a, String b) {
        if (a.equals(b)) return Relation.ALLY;
        if (a.equals(WILD) || b.equals(WILD)) return Relation.NEUTRAL;
        if (a.equals(MONSTERS) || b.equals(MONSTERS)) return Relation.ENEMY;
        if (a.startsWith("npc:") || b.startsWith("npc:")) return Relation.ENEMY;
        if (server == null) return Relation.NEUTRAL;
        return FactionData.get(server).relation(a, b);
    }

    public static Relation relation(Entity a, Entity b) {
        MinecraftServer server = a.level().getServer();
        return relation(server, keyOf(server, a), keyOf(server, b));
    }

    public static ChatFormatting colorOf(@Nullable MinecraftServer server, String key) {
        NpcFaction npc = NpcFaction.byKey(key);
        if (npc != null) return npc.color;
        if (server != null) {
            FactionData.Faction f = FactionData.get(server).byKey(key);
            if (f != null) return f.color;
        }
        return ChatFormatting.WHITE;
    }

    public static String displayName(@Nullable MinecraftServer server, String key) {
        NpcFaction npc = NpcFaction.byKey(key);
        if (npc != null) return npc.displayName;
        if (server != null) {
            FactionData.Faction f = FactionData.get(server).byKey(key);
            if (f != null) return f.name;
        }
        return "Freelance";
    }
}
