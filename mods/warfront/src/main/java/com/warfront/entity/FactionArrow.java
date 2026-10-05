package com.warfront.entity;

import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A plain arrow that flies through allies of whoever fired it, so archers can shoot over
 * their own front ranks. On the client and after a reload it is an ordinary arrow.
 */
public class FactionArrow extends Arrow {
    @Nullable private final String factionKey;

    public FactionArrow(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(level, owner, ammo, weapon);
        this.factionKey = Factions.keyOf(level.getServer(), owner);
        this.getPersistentData().putString(Factions.PROJECTILE_TAG, factionKey);
    }

    public FactionArrow(Level level, double x, double y, double z, ItemStack ammo, String factionKey) {
        super(level, x, y, z, ammo, null);
        this.factionKey = factionKey;
        this.getPersistentData().putString(Factions.PROJECTILE_TAG, factionKey);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) return false;
        if (factionKey == null || level().isClientSide) return true;
        String other = Factions.keyOf(level().getServer(), target);
        return Factions.relation(level().getServer(), factionKey, other) != Relation.ALLY;
    }
}
