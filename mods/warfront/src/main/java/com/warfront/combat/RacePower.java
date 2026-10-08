package com.warfront.combat;

import com.warfront.faction.Race;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** The race power key (R): each race's active power. */
public final class RacePower {
    private RacePower() {}

    public static void use(ServerPlayer p, boolean sneaking) {
        Race race = Race.of(p);
        if (race == null) return;
        switch (race) {
            case HUMAN -> {
                if (Valor.rally(p) < 0) say(p, "Valor " + Valor.current(p) + "/" + Valor.MAX + ": not ready to Rally yet.");
            }
            case DWARF -> {
                if (!Resolve.swear(p)) say(p, "Resolve " + Resolve.current(p) + "/" + Resolve.MAX + ": not ready to swear the Oath.");
            }
            case ANGEL -> Radiance.judgment(p);
            case HIVE -> SwarmCall.call(p, sneaking);
            case ORC -> say(p, "Rage builds as you fight; at full, you Frenzy on your own.");
            case DEMON -> say(p, "Your souls burst on your melee hits.");
            case ELF -> say(p, "Elven power is in the bow: long shots mark their targets.");
        }
    }

    static void say(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY), true);
    }
}
