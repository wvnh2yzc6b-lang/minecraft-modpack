package com.warfront.entity.ai;

import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** Soldiers attack whatever attacked their commander, and whatever their commander attacks. */
public class CommanderAssistGoal extends TargetGoal {
    private final SoldierEntity soldier;
    @Nullable private LivingEntity candidate;
    private int lastHurtByStamp;
    private int lastHurtStamp;

    public CommanderAssistGoal(SoldierEntity soldier) {
        super(soldier, false);
        this.soldier = soldier;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        Player owner = soldier.getOwner();
        if (owner == null) return false;

        LivingEntity attacker = owner.getLastHurtByMob();
        int stamp = owner.getLastHurtByMobTimestamp();
        if (attacker != null && stamp != lastHurtByStamp && valid(attacker)) {
            lastHurtByStamp = stamp;
            candidate = attacker;
            return true;
        }
        LivingEntity victim = owner.getLastHurtMob();
        stamp = owner.getLastHurtMobTimestamp();
        if (victim != null && stamp != lastHurtStamp && valid(victim)) {
            lastHurtStamp = stamp;
            candidate = victim;
            return true;
        }
        return false;
    }

    private boolean valid(LivingEntity e) {
        return e != soldier && e.isAlive() && soldier.distanceToSqr(e) < 32 * 32
                && Factions.relation(soldier, e) != Relation.ALLY
                && canAttack(e, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        mob.setTarget(candidate);
        super.start();
    }
}
