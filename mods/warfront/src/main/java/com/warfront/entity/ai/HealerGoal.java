package com.warfront.entity.ai;

import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Race;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

/** Healers seek out the most wounded ally nearby and mend them. */
public class HealerGoal extends Goal {
    private final SoldierEntity soldier;
    @Nullable private LivingEntity patient;
    private int searchCooldown;
    private int healCooldown;
    private int repath;

    public HealerGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (soldier.getRole() != SoldierRole.HEALER || soldier.isRouting()) return false;
        if (--searchCooldown > 0) return false;
        searchCooldown = 10;
        patient = findPatient();
        return patient != null;
    }

    @Nullable
    private LivingEntity findPatient() {
        Vec3 slot = soldier.getSlot();
        boolean holding = soldier.getOrder() == Order.HOLD && slot != null;
        return soldier.nearbyAllies(12, LivingEntity.class).stream()
                .filter(e -> e.getHealth() < e.getMaxHealth() * 0.85F)
                .filter(e -> !holding || e.position().distanceToSqr(slot) < 10 * 10)
                .min(Comparator.comparingDouble(e -> e.getHealth() / e.getMaxHealth()))
                .orElse(null);
    }

    @Override
    public boolean canContinueToUse() {
        return patient != null && patient.isAlive() && patient.getHealth() < patient.getMaxHealth()
                && soldier.distanceToSqr(patient) < 16 * 16 && !soldier.isRouting();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        patient = null;
        soldier.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (patient == null) return;
        soldier.getLookControl().setLookAt(patient, 30.0F, 30.0F);
        if (soldier.distanceToSqr(patient) > 2.5 * 2.5) {
            if (--repath <= 0) {
                repath = 10;
                soldier.getNavigation().moveTo(patient, 1.1);
            }
            return;
        }
        soldier.getNavigation().stop();
        if (--healCooldown > 0) return;
        healCooldown = 30;
        float amount = soldier.getRace() == Race.HUMAN ? 5.0F : 4.0F;
        patient.heal(amount);
        soldier.swing(InteractionHand.MAIN_HAND);
        soldier.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
        if (soldier.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY() + patient.getBbHeight() + 0.3,
                    patient.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        }
    }
}
