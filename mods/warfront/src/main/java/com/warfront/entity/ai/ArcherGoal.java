package com.warfront.entity.ai;

import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.entity.FactionArrow;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Race;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Archers loose arrows in synchronized volleys: every archer in the world draws on the same
 * 2-second rhythm, so a company of archers fires together. Arrows fly through allies.
 */
public class ArcherGoal extends Goal {
    private static final int VOLLEY_PERIOD = 40;
    private final SoldierEntity soldier;

    public ArcherGoal(SoldierEntity soldier) {
        this.soldier = soldier;
    }

    @Override
    public boolean canUse() {
        LivingEntity t = soldier.getTarget();
        return soldier.getRole() == SoldierRole.ARCHER && !soldier.isRouting() && t != null && t.isAlive()
                && soldier.getMainHandItem().getItem() instanceof BowItem;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        if (soldier.isUsingItem()) soldier.stopUsingItem();
    }

    @Override
    public void tick() {
        LivingEntity target = soldier.getTarget();
        if (target == null) return;
        double d2 = soldier.distanceToSqr(target);
        boolean see = soldier.getSensing().hasLineOfSight(target);
        soldier.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (soldier.getOrder() == Order.CHARGE) {
            if (d2 > 18 * 18 || !see) soldier.getNavigation().moveTo(target, 1.0);
            else soldier.getNavigation().stop();
        }

        if (!see || d2 > 30 * 30) {
            if (soldier.isUsingItem()) soldier.stopUsingItem();
            return;
        }

        long phase = soldier.level().getGameTime() % VOLLEY_PERIOD;
        if (!soldier.isUsingItem()) {
            if (phase >= VOLLEY_PERIOD - 22) soldier.startUsingItem(InteractionHand.MAIN_HAND);
        } else if (phase < 3 && soldier.getTicksUsingItem() >= 15) {
            soldier.stopUsingItem();
            shoot(target);
        }
    }

    private void shoot(LivingEntity target) {
        ItemStack bow = soldier.getMainHandItem();
        FactionArrow arrow = new FactionArrow(soldier.level(), soldier, new ItemStack(Items.ARROW), bow.copy());
        double dx = target.getX() - soldier.getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - soldier.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        boolean elf = soldier.getRace() == Race.ELF;
        arrow.shoot(dx, dy + h * 0.2, dz, 1.7F, elf ? 2.0F : 6.0F);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        if (elf) arrow.setBaseDamage(arrow.getBaseDamage() + 1.0);
        soldier.level().addFreshEntity(arrow);
        soldier.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (soldier.getRandom().nextFloat() * 0.4F + 0.8F));
    }
}
