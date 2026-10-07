package com.warfront.advisor;

import com.warfront.faction.Race;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A player's advisor: a robed elder in the colors of the player's race who teaches the first hour and gives counsel
 * after. He can't be hurt, pushed or led, and stays where he is put.
 */
public class AdvisorEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_RACE =
            SynchedEntityData.defineId(AdvisorEntity.class, EntityDataSerializers.INT);

    @Nullable private UUID owner;

    public AdvisorEntity(EntityType<? extends AdvisorEntity> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RACE, Race.HUMAN.ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 1.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public Race getDisguise() {
        return Race.byOrdinal(entityData.get(DATA_RACE));
    }

    public void setDisguise(Race race) {
        entityData.set(DATA_RACE, race.ordinal());
        setCustomName(Component.literal(Advisor.title(race)).withStyle(race.color));
        setCustomNameVisible(true);
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) Advisor.talk(player, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isLeashed()) dropLeash(true, true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Disguise", entityData.get(DATA_RACE));
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_RACE, tag.getInt("Disguise"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
