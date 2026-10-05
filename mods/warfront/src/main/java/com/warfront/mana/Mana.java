package com.warfront.mana;

import com.warfront.network.ManaPayload;
import com.warfront.registry.WFRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** A player's mana pool: spent to summon troops, refilled with Mana Shards. */
public final class Mana {
    public static final float CAP = 1000F;
    /** Passive regeneration only refills up to this much, so shards stay the real source. */
    public static final float PASSIVE_LIMIT = 20F;

    private Mana() {}

    public static float get(Player p) {
        return p.getData(WFRegistry.MANA);
    }

    public static float max(Player p) {
        return p.getData(WFRegistry.MAX_MANA);
    }

    public static void set(Player p, float value) {
        p.setData(WFRegistry.MANA, Mth.clamp(value, 0F, max(p)));
        sync(p);
    }

    /** Adds mana up to the maximum; returns how much was actually added. */
    public static float add(Player p, float amount) {
        float before = get(p);
        set(p, before + amount);
        return get(p) - before;
    }

    /** Spends mana if the player has enough. Creative players summon for free. */
    public static boolean trySpend(Player p, float amount) {
        if (p.getAbilities().instabuild) return true;
        float have = get(p);
        if (have < amount) return false;
        set(p, have - amount);
        return true;
    }

    public static void raiseMax(Player p, float amount) {
        p.setData(WFRegistry.MAX_MANA, Math.min(CAP, max(p) + amount));
        sync(p);
    }

    public static void sync(Player p) {
        if (p instanceof ServerPlayer sp && sp.connection != null) {
            PacketDistributor.sendToPlayer(sp, new ManaPayload(get(p), max(p)));
        }
    }
}
