package com.warfront.war;

import com.warfront.army.Duty;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.combat.Rage;
import com.warfront.combat.Souls;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.network.HudPayload;
import com.warfront.registry.WFRegistry;
import com.warfront.test.TestActions;
import com.warfront.world.BaseLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Gathers what each player's combat HUD shows and sends it once a second. */
public final class HudSync {
    private HudSync() {}

    public static List<Integer> values(ServerPlayer p) {
        int[] v = new int[HudPayload.SIZE];
        v[HudPayload.SOULS] = Souls.count(p);
        v[HudPayload.RAGE] = Rage.current(p);
        MobEffectInstance frenzy = p.getEffect(WFRegistry.FRENZY);
        v[HudPayload.FRENZY] = frenzy == null ? 0 : frenzy.getDuration();
        v[HudPayload.RESOLVE] = com.warfront.combat.Resolve.current(p);
        v[HudPayload.VALOR] = com.warfront.combat.Valor.current(p);
        v[HudPayload.RADIANCE] = com.warfront.combat.Radiance.current(p);
        v[HudPayload.SWARM] = com.warfront.combat.SwarmCall.current(p);
        MobEffectInstance oath = p.getEffect(WFRegistry.OATH_OF_STONE);
        v[HudPayload.OATH] = oath == null ? 0 : oath.getDuration();
        List<SoldierEntity> units = TestActions.owned(p);
        for (SoldierEntity s : units) {
            if (s.getRole().worker()) continue;
            v[HudPayload.TOTAL]++;
            if (s.getDuty() == Duty.GUARD) v[HudPayload.GUARD]++;
            else if (s.getDuty() == Duty.PATROL) v[HudPayload.PATROL]++;
            else if (s.getOrder() == Order.FOLLOW) v[HudPayload.FOLLOWING]++;
            if (s.getRole() == SoldierRole.BEAST) v[HudPayload.BEASTS]++;
        }
        v[HudPayload.FORMATION] = p.getData(WFRegistry.ARMY_FORMATION);
        v[HudPayload.ORDER] = p.getData(WFRegistry.ARMY_ORDER);
        BaseLevel.Status base = BaseLevel.of(p.level(), p.blockPosition(), Factions.keyOf(p.server, p));
        v[HudPayload.BEAST_CAP] = BaseLevel.beastCap(base.buildings() > 0 || base.isTest() ? base.level() : 1);
        WarStandardBlockEntity standard = TestActions.nearestStandard(p);
        if (standard != null && standard.isUnderSiege() && standard.isDefender(p)) {
            v[HudPayload.WAVE] = standard.getWave();
            v[HudPayload.LEFT] = standard.getAttackersLeft();
        }
        List<Integer> out = new ArrayList<>(v.length);
        for (int x : v) out.add(x);
        return out;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getData(WFRegistry.RACE).isEmpty()) continue;
            PacketDistributor.sendToPlayer(p, new HudPayload(values(p)));
        }
    }
}
