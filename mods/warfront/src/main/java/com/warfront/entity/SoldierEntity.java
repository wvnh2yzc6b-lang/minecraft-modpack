package com.warfront.entity;

import com.warfront.army.Formation;
import com.warfront.army.FormationLayout;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.entity.ai.*;
import com.warfront.faction.*;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A recruitable (or hostile) soldier. All roles share this entity; the role decides loadout,
 * stats and which AI goals are active.
 */
public class SoldierEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_ROLE =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ORDER =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_FACTION =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public static final int SKIN_COUNT = 6;

    private Race race = Race.HUMAN;
    private Formation formation = Formation.LINE;
    @Nullable private UUID warbandId;
    @Nullable private Vec3 anchor;
    private float anchorYaw;
    @Nullable private BlockPos siegeTarget;
    private int tier = 1;
    private boolean configured;
    /** Bitmask of equipment slots holding gear a player handed over (dropped on death). */
    private int givenMask;

    private float morale = 100f;
    private int routTicks;
    @Nullable private Vec3 slot;
    private boolean marchLeader;

    public SoldierEntity(EntityType<? extends SoldierEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROLE, SoldierRole.SWORDSMAN.ordinal());
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_ORDER, Order.FOLLOW.ordinal());
        builder.define(DATA_FACTION, Factions.WILD);
        builder.define(DATA_OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RoutGoal(this));
        this.goalSelector.addGoal(2, new HealerGoal(this));
        this.goalSelector.addGoal(2, new ShieldGoal(this));
        this.goalSelector.addGoal(3, new ArcherGoal(this));
        this.goalSelector.addGoal(3, new SoldierMeleeGoal(this));
        this.goalSelector.addGoal(5, new FormationMoveGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new CommanderAssistGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, false,
                this::isEnemy));
    }

    // ------------------------------------------------------------------ setup

    /** Configures this soldier as a recruit serving {@code owner}. */
    public void setupAsRecruit(Player owner, SoldierRole role, Race race) {
        this.configured = true;
        this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        this.race = race;
        this.tier = 1;
        this.entityData.set(DATA_ROLE, role.ordinal());
        this.entityData.set(DATA_SKIN, race.ordinal());
        this.entityData.set(DATA_ORDER, Order.FOLLOW.ordinal());
        this.setPersistenceRequired();
        refreshFactionKey();
        applyStats();
        equipLoadout();
        refreshName();
    }

    /** Configures this soldier as a member of a hostile NPC warband. */
    public void setupAsRaider(NpcFaction faction, SoldierRole role, UUID warband, @Nullable Vec3 objective,
                              @Nullable BlockPos siegeTarget, int tier) {
        this.configured = true;
        this.entityData.set(DATA_OWNER, Optional.empty());
        this.entityData.set(DATA_FACTION, faction.key());
        this.race = faction.race;
        this.tier = tier;
        this.warbandId = warband;
        this.anchor = objective;
        this.siegeTarget = siegeTarget;
        this.entityData.set(DATA_ROLE, role.ordinal());
        this.entityData.set(DATA_SKIN, faction.skin);
        this.entityData.set(DATA_ORDER, (objective != null ? Order.MARCH : Order.CHARGE).ordinal());
        this.formation = siegeTarget != null ? Formation.WEDGE : Formation.LINE;
        if (siegeTarget != null) this.setPersistenceRequired();
        applyStats();
        equipLoadout();
        refreshName();
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data);
        if (!configured) {
            NpcFaction f = NpcFaction.values()[random.nextInt(NpcFaction.values().length)];
            SoldierRole[] roles = SoldierRole.values();
            setupAsRaider(f, roles[random.nextInt(roles.length)], UUID.randomUUID(), null, null,
                    1 + (int) difficulty.getEffectiveDifficulty() / 2);
        }
        return result;
    }

    private void applyStats() {
        SoldierRole role = getRole();
        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(role.health + tier * 2);
        Objects.requireNonNull(getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(role.damage);
        Objects.requireNonNull(getAttribute(Attributes.ARMOR)).setBaseValue(role.armor);
        Objects.requireNonNull(getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(role.speed);
        race.apply(this);
        setHealth(getMaxHealth());
    }

    private void equipLoadout() {
        boolean elite = tier >= 3;
        boolean iron = tier >= 2;
        clearLoadout();
        switch (getRole()) {
            case SHIELDBEARER -> {
                gear(EquipmentSlot.MAINHAND, iron ? Items.IRON_SWORD : Items.STONE_SWORD);
                gear(EquipmentSlot.OFFHAND, Items.SHIELD);
                gear(EquipmentSlot.HEAD, iron ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET);
                gear(EquipmentSlot.CHEST, iron ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE);
                gear(EquipmentSlot.LEGS, Items.CHAINMAIL_LEGGINGS);
            }
            case SPEARMAN -> {
                gear(EquipmentSlot.MAINHAND, Items.TRIDENT);
                gear(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET);
                gear(EquipmentSlot.CHEST, iron ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE);
            }
            case SWORDSMAN -> {
                gear(EquipmentSlot.MAINHAND, elite ? Items.DIAMOND_SWORD : Items.IRON_SWORD);
                gear(EquipmentSlot.HEAD, Items.IRON_HELMET);
                gear(EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE);
                if (iron) gear(EquipmentSlot.LEGS, Items.IRON_LEGGINGS);
            }
            case CAPTAIN -> {
                gear(EquipmentSlot.MAINHAND, elite ? Items.DIAMOND_SWORD : Items.IRON_SWORD);
                gear(EquipmentSlot.OFFHAND, bannerFor(Factions.colorOf(level().getServer(), getFactionKey())));
                gear(EquipmentSlot.HEAD, elite ? Items.DIAMOND_HELMET : Items.IRON_HELMET);
                gear(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE);
                gear(EquipmentSlot.LEGS, Items.IRON_LEGGINGS);
                gear(EquipmentSlot.FEET, Items.IRON_BOOTS);
            }
            case ARCHER -> {
                gear(EquipmentSlot.MAINHAND, Items.BOW);
                gear(EquipmentSlot.HEAD, Items.LEATHER_HELMET);
                gear(EquipmentSlot.CHEST, iron ? Items.CHAINMAIL_CHESTPLATE : Items.LEATHER_CHESTPLATE);
            }
            case HEALER -> {
                gear(EquipmentSlot.MAINHAND, WFRegistry.HEALING_STAFF.get());
                gear(EquipmentSlot.HEAD, Items.GOLDEN_HELMET);
                if (iron) gear(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
            }
        }
    }

    private void clearLoadout() {
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s == EquipmentSlot.BODY) continue;
            if ((givenMask & (1 << s.ordinal())) == 0) setItemSlot(s, ItemStack.EMPTY);
        }
    }

    private void gear(EquipmentSlot slot, Item item) {
        if ((givenMask & (1 << slot.ordinal())) != 0) return;
        setItemSlot(slot, new ItemStack(item));
        setDropChance(slot, 0.0F);
    }

    private static Item bannerFor(ChatFormatting color) {
        DyeColor dye = switch (color) {
            case RED, DARK_RED -> DyeColor.RED;
            case BLUE, DARK_BLUE -> DyeColor.BLUE;
            case GREEN -> DyeColor.LIME;
            case DARK_GREEN -> DyeColor.GREEN;
            case AQUA -> DyeColor.LIGHT_BLUE;
            case DARK_AQUA -> DyeColor.CYAN;
            case GOLD -> DyeColor.ORANGE;
            case YELLOW -> DyeColor.YELLOW;
            case LIGHT_PURPLE -> DyeColor.MAGENTA;
            case DARK_PURPLE -> DyeColor.PURPLE;
            case GRAY -> DyeColor.LIGHT_GRAY;
            case DARK_GRAY, BLACK -> DyeColor.BLACK;
            default -> DyeColor.WHITE;
        };
        return BannerBlock.byColor(dye).asItem();
    }

    // ------------------------------------------------------------------ accessors

    public SoldierRole getRole() {
        return SoldierRole.byOrdinal(entityData.get(DATA_ROLE));
    }

    public int getSkin() {
        return Mth.clamp(entityData.get(DATA_SKIN), 0, SKIN_COUNT - 1);
    }

    public Order getOrder() {
        return Order.byOrdinal(entityData.get(DATA_ORDER));
    }

    public Formation getFormation() {
        return formation;
    }

    public Race getRace() {
        return race;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    @Nullable
    public Player getOwner() {
        UUID id = getOwnerUUID();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    public boolean isOwnedBy(Player p) {
        return p.getUUID().equals(getOwnerUUID());
    }

    @Nullable
    public UUID getWarbandId() {
        return warbandId;
    }

    @Nullable
    public BlockPos getSiegeTarget() {
        return siegeTarget;
    }

    @Nullable
    public Vec3 getSlot() {
        return slot;
    }

    @Nullable
    public Vec3 getAnchor() {
        return anchor;
    }

    public float getAnchorYaw() {
        return anchorYaw;
    }

    public boolean isMarchLeader() {
        return marchLeader;
    }

    public float getMorale() {
        return morale;
    }

    public boolean isRouting() {
        return routTicks > 0;
    }

    public String getFactionKey() {
        return entityData.get(DATA_FACTION);
    }

    /** Issues an order to this soldier. Anchor/yaw are used by HOLD and CHARGE. */
    public void command(Order order, Formation formation, @Nullable Vec3 anchor, float yaw) {
        this.entityData.set(DATA_ORDER, order.ordinal());
        this.formation = formation;
        if (anchor != null) {
            this.anchor = anchor;
            this.anchorYaw = yaw;
        }
        if (order != Order.CHARGE) setTarget(null);
        this.getNavigation().stop();
        recomputeSlot();
    }

    public boolean isSameGroup(SoldierEntity other) {
        UUID owner = getOwnerUUID();
        if (owner != null) return owner.equals(other.getOwnerUUID());
        return warbandId != null && warbandId.equals(other.warbandId);
    }

    public boolean isEnemy(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return Factions.relation(this, e) == Relation.ENEMY;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == this) return true;
        if (level().isClientSide) return super.isAlliedTo(other);
        return Factions.relation(this, other) == Relation.ALLY || super.isAlliedTo(other);
    }

    /** How far from its slot this soldier may chase an enemy, given its current order. */
    public boolean withinLeash(@Nullable LivingEntity target) {
        if (target == null) return false;
        Order order = getOrder();
        if (order == Order.CHARGE || slot == null) return true;
        double leash = switch (order) {
            case HOLD -> formation == Formation.SHIELD_WALL ? 3.5 : 6.0;
            case FOLLOW -> 10.0;
            default -> 18.0;
        };
        return target.position().distanceToSqr(slot) < leash * leash;
    }

    // ------------------------------------------------------------------ formation logic

    /** Recalculates this soldier's place in its formation. */
    public void recomputeSlot() {
        Order order = getOrder();
        marchLeader = false;
        if (order == Order.CHARGE) {
            slot = anchor;
            return;
        }

        Vec3 ref;
        float yaw;
        if (order == Order.FOLLOW) {
            Player owner = getOwner();
            if (owner == null) {
                slot = anchor;
                return;
            }
            yaw = owner.getYRot();
            float rad = yaw * Mth.DEG_TO_RAD;
            ref = owner.position().add(Mth.sin(rad) * 2.5, 0, -Mth.cos(rad) * 2.5);
        } else if (order == Order.HOLD) {
            if (anchor == null) {
                anchor = position();
                anchorYaw = getYRot();
            }
            ref = anchor;
            yaw = anchorYaw;
        } else { // MARCH
            if (anchor == null) {
                slot = null;
                return;
            }
            List<SoldierEntity> group = groupMembers(position(), order);
            SoldierEntity leader = this;
            for (SoldierEntity s : group) {
                if (s.getUUID().compareTo(leader.getUUID()) < 0) leader = s;
            }
            if (leader == this) {
                marchLeader = true;
                Vec3 to = anchor.subtract(position());
                slot = to.length() > 20 ? position().add(to.normalize().scale(16)) : anchor;
                return;
            }
            Vec3 dir = anchor.subtract(leader.position());
            yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
            float rad = yaw * Mth.DEG_TO_RAD;
            ref = leader.position().add(Mth.sin(rad) * 1.5, 0, -Mth.cos(rad) * 1.5);
            SoldierEntity finalLeader = leader;
            group.removeIf(s -> s == finalLeader);
            assignSlot(group, ref, yaw);
            return;
        }
        assignSlot(groupMembers(ref, order), ref, yaw);
    }

    private void assignSlot(List<SoldierEntity> group, Vec3 ref, float yaw) {
        if (!group.contains(this)) group.add(this);
        group.sort(Comparator.<SoldierEntity>comparingInt(s -> s.getRole().rank).thenComparing(Entity::getUUID));
        int melee = 0;
        for (SoldierEntity s : group) if (s.getRole().melee) melee++;
        int index = group.indexOf(this);
        slot = FormationLayout.slot(formation, index, group.size(), melee, ref, yaw);
        anchorYaw = yaw;
    }

    private List<SoldierEntity> groupMembers(Vec3 around, Order order) {
        AABB box = new AABB(around, around).inflate(48);
        return new ArrayList<>(level().getEntitiesOfClass(SoldierEntity.class, box,
                s -> s.isAlive() && s.getOrder() == order && s.isSameGroup(this)));
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;

        if (routTicks > 0 && --routTicks == 0) {
            morale = Math.min(100f, morale + 35f);
        }

        if ((tickCount + getId()) % 20 == 0) {
            secondTick();
        }
    }

    private void secondTick() {
        refreshFactionKey();
        recomputeSlot();

        // Morale recovers over time; faster near a captain.
        float regen = (float) race.moraleRegen * 2f;
        if (getRole() != SoldierRole.CAPTAIN && nearbyAllies(10, SoldierEntity.class).stream()
                .anyMatch(s -> s.getRole() == SoldierRole.CAPTAIN)) {
            regen += 4f;
        }
        morale = Mth.clamp(morale + regen, 0f, 100f);

        if (race.routs && routTicks == 0 && morale < 20f && getHealth() < getMaxHealth() * 0.4f) {
            routTicks = 120;
            setTarget(null);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.VILLAGER_HURT, SoundSource.HOSTILE, 1f, 0.8f);
        }

        if (getRole() == SoldierRole.CAPTAIN && (tickCount / 20) % 2 == 0) {
            rally();
        }
        if (getRole() == SoldierRole.HEALER && getHealth() < getMaxHealth()) {
            heal(1f);
        }

        Order order = getOrder();
        if (order == Order.MARCH && anchor != null && position().distanceToSqr(anchor) < 14 * 14) {
            entityData.set(DATA_ORDER, Order.CHARGE.ordinal());
        }

        if (siegeTarget != null && getTarget() == null
                && position().distanceToSqr(Vec3.atCenterOf(siegeTarget)) < 3.5 * 3.5) {
            BlockEntity be = level().getBlockEntity(siegeTarget);
            if (be instanceof WarStandardBlockEntity standard) {
                swing(InteractionHand.MAIN_HAND);
                standard.takeHit(this);
            } else {
                siegeTarget = null;
            }
        }

        if (getOwnerUUID() != null && tickCount % 200 < 20) {
            refreshName();
        }
    }

    /** Captain's rallying cry: strength and morale for allies close by. */
    private void rally() {
        for (LivingEntity ally : nearbyAllies(10, LivingEntity.class)) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0, true, false));
            if (ally instanceof SoldierEntity s) s.morale = Math.min(100f, s.morale + 5f);
        }
    }

    public <T extends LivingEntity> List<T> nearbyAllies(double radius, Class<T> type) {
        return level().getEntitiesOfClass(type, getBoundingBox().inflate(radius),
                e -> e != this && e.isAlive() && Factions.relation(this, e) == Relation.ALLY);
    }

    private void refreshFactionKey() {
        UUID owner = getOwnerUUID();
        MinecraftServer server = level().getServer();
        if (owner != null && server != null) {
            String key = FactionData.get(server).keyOf(owner);
            if (!key.equals(entityData.get(DATA_FACTION))) entityData.set(DATA_FACTION, key);
        }
    }

    private void refreshName() {
        MinecraftServer server = level().getServer();
        String key = getFactionKey();
        Component name = Component.literal("[" + Factions.displayName(server, key) + "] ")
                .withStyle(Factions.colorOf(server, key))
                .append(Component.literal(race.displayName() + " " + getRole().displayName())
                        .withStyle(ChatFormatting.WHITE));
        setCustomName(name);
        setCustomNameVisible(false);
    }

    // ------------------------------------------------------------------ combat hooks

    @Override
    protected AABB getAttackBoundingBox() {
        AABB box = super.getAttackBoundingBox();
        return getRole() == SoldierRole.SPEARMAN ? box.inflate(1.25, 0.0, 1.25) : box;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            morale = Math.max(0f, morale - amount * 2.5f);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel server)) return;

        float shock = getRole() == SoldierRole.CAPTAIN ? 35f : 12f;
        for (SoldierEntity ally : nearbyAllies(12, SoldierEntity.class)) {
            ally.morale = Math.max(0f, ally.morale - shock);
        }

        Player owner = getOwner();
        if (owner != null) {
            owner.displayClientMessage(Component.literal("Your " + race.displayName() + " "
                    + getRole().displayName() + " has fallen.").withStyle(ChatFormatting.RED), true);
        } else if (source.getEntity() != null) {
            int marks = 1 + random.nextInt(2) + tier / 2 + (getRole() == SoldierRole.CAPTAIN ? 3 : 0);
            spawnAtLocation(new ItemStack(WFRegistry.WAR_MARK.get(), marks));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return getOwnerUUID() == null && siegeTarget == null;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwnedBy(player)) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            EquipmentSlot target = getEquipmentSlotForItem(held);
            boolean armor = target.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
            boolean weapon = target == EquipmentSlot.MAINHAND && getRole().melee
                    && (held.getItem() instanceof SwordItem || held.getItem() instanceof AxeItem);
            if (armor || weapon) {
                ItemStack old = getItemBySlot(target);
                if (!old.isEmpty() && (givenMask & (1 << target.ordinal())) != 0) {
                    player.getInventory().placeItemBackInInventory(old);
                }
                setItemSlot(target, held.split(1));
                setDropChance(target, 2.0F);
                givenMask |= 1 << target.ordinal();
                playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 1f);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        player.displayClientMessage(Component.empty()
                .append(Objects.requireNonNull(getCustomName()))
                .append(Component.literal(String.format("  %.0f/%.0f HP  morale %.0f  %s, %s",
                        getHealth(), getMaxHealth(), morale, getOrder().title, formation.title))
                        .withStyle(ChatFormatting.GRAY)), true);
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", entityData.get(DATA_ROLE));
        tag.putInt("Skin", entityData.get(DATA_SKIN));
        tag.putInt("Order", entityData.get(DATA_ORDER));
        tag.putString("Faction", entityData.get(DATA_FACTION));
        UUID owner = getOwnerUUID();
        if (owner != null) tag.putUUID("Owner", owner);
        if (warbandId != null) tag.putUUID("Warband", warbandId);
        tag.putString("Race", race.id());
        tag.putInt("Formation", formation.ordinal());
        tag.putInt("Tier", tier);
        tag.putFloat("Morale", morale);
        tag.putInt("Given", givenMask);
        tag.putBoolean("Configured", configured);
        if (anchor != null) {
            tag.putDouble("AnchorX", anchor.x);
            tag.putDouble("AnchorY", anchor.y);
            tag.putDouble("AnchorZ", anchor.z);
        }
        tag.putFloat("AnchorYaw", anchorYaw);
        if (siegeTarget != null) tag.putLong("SiegeTarget", siegeTarget.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_ROLE, tag.getInt("Role"));
        entityData.set(DATA_SKIN, tag.getInt("Skin"));
        entityData.set(DATA_ORDER, tag.getInt("Order"));
        if (tag.contains("Faction")) entityData.set(DATA_FACTION, tag.getString("Faction"));
        entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        warbandId = tag.hasUUID("Warband") ? tag.getUUID("Warband") : null;
        Race r = Race.byId(tag.getString("Race"));
        race = r != null ? r : Race.HUMAN;
        formation = Formation.byOrdinal(tag.getInt("Formation"));
        tier = tag.getInt("Tier");
        morale = tag.contains("Morale") ? tag.getFloat("Morale") : 100f;
        givenMask = tag.getInt("Given");
        configured = tag.getBoolean("Configured");
        anchor = tag.contains("AnchorX")
                ? new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ")) : null;
        anchorYaw = tag.getFloat("AnchorYaw");
        siegeTarget = tag.contains("SiegeTarget") ? BlockPos.of(tag.getLong("SiegeTarget")) : null;
    }
}
