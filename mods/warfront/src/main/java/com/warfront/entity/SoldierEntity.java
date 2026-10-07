package com.warfront.entity;

import com.warfront.army.Formation;
import com.warfront.army.FormationLayout;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.entity.ai.*;
import com.warfront.entity.work.Blueprint;
import com.warfront.entity.work.WorkSites;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.SimpleContainer;
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
    private static final EntityDataAccessor<Integer> DATA_RANK =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FALLEN =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int SKIN_COUNT = Race.values().length + NpcFaction.values().length;

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
    private int xp;
    /** Game time a fallen hero gives up waiting for its commander. */
    private long fallenUntil;
    private int routTicks;
    private boolean warlord;
    /** Seconds this soldier has failed to make progress towards its objective (for block breaching). */
    private int stuckSeconds;
    @Nullable private Vec3 lastProgressPos;
    @Nullable private Vec3 slot;
    private boolean marchLeader;
    /** What a worker carries: harvest, seeds, building blocks. */
    private final SimpleContainer workItems = new SimpleContainer(18);
    @Nullable private Blueprint blueprint;
    private long alarmQuietUntil;
    /** Guard or patrol duty for a battle unit (NONE while it marches with the army). */
    private com.warfront.army.Duty duty = com.warfront.army.Duty.NONE;

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
        builder.define(DATA_RANK, 0);
        builder.define(DATA_FALLEN, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RoutGoal(this));
        // Workers run from danger instead of fighting it.
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class, 10.0F, 0.9, 1.25,
                e -> getRole().worker() && isEnemy(e)));
        this.goalSelector.addGoal(2, new BreachGoal(this));
        this.goalSelector.addGoal(2, new HealerGoal(this));
        this.goalSelector.addGoal(2, new ShieldGoal(this));
        this.goalSelector.addGoal(3, new ArcherGoal(this));
        this.goalSelector.addGoal(3, new SoldierMeleeGoal(this));
        this.goalSelector.addGoal(4, new FarmerGoal(this));
        this.goalSelector.addGoal(4, new BuilderGoal(this));
        this.goalSelector.addGoal(4, new GuardPatrolGoal(this));
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

    /** Configures this soldier as a neutral guard (the merchant's caravan) holding near {@code post}. */
    public void setupAsNeutral(Race race, SoldierRole role, Vec3 post) {
        this.configured = true;
        this.entityData.set(DATA_OWNER, Optional.empty());
        this.entityData.set(DATA_FACTION, Factions.WILD);
        this.race = race;
        this.tier = 2;
        this.entityData.set(DATA_ROLE, role.ordinal());
        this.entityData.set(DATA_SKIN, race.ordinal());
        this.setPersistenceRequired();
        applyStats();
        equipLoadout();
        refreshName();
        command(Order.HOLD, Formation.LINE, post, 0F);
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
        this.entityData.set(DATA_SKIN, faction.skin());
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
            NpcFaction f = NpcFaction.random(random);
            SoldierRole[] roles = Arrays.stream(SoldierRole.values()).filter(r -> !r.posted()).toArray(SoldierRole[]::new);
            setupAsRaider(f, roles[random.nextInt(roles.length)], UUID.randomUUID(), null, null,
                    1 + (int) difficulty.getEffectiveDifficulty() / 2);
        }
        return result;
    }

    private void applyStats() {
        SoldierRole role = getRole();
        boolean champion = role == SoldierRole.CHAMPION;
        double damage = role.damage;
        double armor = role.armor + role.gearArmor + (tier - 1) * 1.5;
        // Hive warriors fight with their own claws instead of weapons.
        if (race == Race.HIVE && role.melee) damage += 4.0;
        if (champion && race == Race.DWARF) armor += 6.0;
        com.warfront.army.UnitBody unitBody = com.warfront.army.UnitBody.of(race, role);
        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(
                Math.round((role.health + tier * 2) * unitBody.healthMultiplier));
        Objects.requireNonNull(getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(damage);
        Objects.requireNonNull(getAttribute(Attributes.ARMOR)).setBaseValue(Math.min(30.0, armor));
        Objects.requireNonNull(getAttribute(Attributes.ARMOR_TOUGHNESS)).setBaseValue(champion ? 4.0 : tier >= 3 ? 2.0 : 0.0);
        Objects.requireNonNull(getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(role.speed * unitBody.speedMultiplier);
        race.apply(this);
        setModifier(Attributes.SCALE, "unit_scale", unitBody.scale, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        // The rank ladder reads at a glance: Captains stand 15% taller than their soldiers, Champions 30% (Hive 35%).
        if (role == SoldierRole.CAPTAIN) {
            setModifier(Attributes.SCALE, "captain_scale", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        if (champion) {
            double scale = race == Race.HIVE ? 0.35 : 0.30;
            setModifier(Attributes.SCALE, "champion_scale", scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            setModifier(Attributes.KNOCKBACK_RESISTANCE, "champion_knockback", race == Race.DWARF ? 1.0 : 0.4,
                    AttributeModifier.Operation.ADD_VALUE);
            if (race == Race.ELF) {
                setModifier(Attributes.MOVEMENT_SPEED, "champion_speed", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            }
        }
        setHealth(getMaxHealth());
    }

    // ------------------------------------------------------------------ fallen heroes

    /** Player-owned Captains, Champions and war beasts fall instead of dying. */
    public boolean isHero() {
        SoldierRole r = getRole();
        return getOwnerUUID() != null && (r == SoldierRole.CAPTAIN || r == SoldierRole.CHAMPION || r == SoldierRole.BEAST);
    }

    /** Seconds a fallen hero still waits for its commander. */
    public int fallenSecondsLeft() {
        return isFallen() ? (int) Math.max(0, (fallenUntil - level().getGameTime()) / 20) : 0;
    }

    public boolean isFallen() {
        return entityData.get(DATA_FALLEN);
    }

    /**
     * Called as this unit would die. A hero falls instead (unless the world is on Warlord, or the damage bypasses
     * invulnerability): it lies at 1 health for a while, waiting for its commander. Returns true if it fell.
     */
    public boolean tryFall(DamageSource source) {
        if (!isHero() || isFallen() || level().isClientSide || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        MinecraftServer server = level().getServer();
        if (server != null && com.warfront.war.WarState.get(server).preset().heroesStayDead()) return false;
        setHealth(1F);
        entityData.set(DATA_FALLEN, true);
        fallenUntil = level().getGameTime() + com.warfront.config.WFConfig.FALL_WINDOW.get() * 20L;
        setTarget(null);
        getNavigation().stop();
        setNoAi(true);
        playSound(SoundEvents.PLAYER_HURT, 1.0F, 0.6F);
        Player owner = getOwner();
        if (owner != null) {
            com.warfront.alert.Alerts.toast(owner, "hero_fallen", "Hero fallen", "Your " + getUnitName() + " fell at " + blockPosition().toShortString()
                    + ". Right-click it within " + com.warfront.config.WFConfig.FALL_WINDOW.get() + "s to get it up.");
        }
        return true;
    }

    /** The commander helps a fallen hero up: back at half health. */
    public void revive() {
        if (!isFallen()) return;
        entityData.set(DATA_FALLEN, false);
        setNoAi(false);
        setHealth(getMaxHealth() * 0.5F);
        playSound(SoundEvents.TOTEM_USE, 0.6F, 1.4F);
        com.warfront.alert.Alerts.toast(getOwner(), "hero_revived", "Back on its feet", "Your " + getUnitName() + " fights on at half health.");
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.5), getZ(), 15, 0.4, 0.4, 0.4, 0.1);
        }
    }

    /** Nobody came: the hero leaves the field and returns to the commander's altars after a while. */
    public void giveUpWaiting() {
        UUID owner = getOwnerUUID();
        MinecraftServer server = level().getServer();
        if (owner != null && server != null) {
            long ready = level().getGameTime() + com.warfront.config.WFConfig.HERO_RETURN.get() * 20L;
            com.warfront.war.WarState.get(server).addReturning(owner,
                    new com.warfront.war.WarState.Returning(getRole(), race, xp, ready));
            Player p = getOwner();
            com.warfront.alert.Alerts.toast(p, "hero_returning", "Hero carried off", "Your " + getUnitName() + " can be summoned again at an altar "
                    + "for half its cost in " + com.warfront.config.WFConfig.HERO_RETURN.get() / 60 + " min.");
        }
        discard();
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !isFallen() && !com.warfront.war.Bounties.isCaptive(this) && super.canBeSeenAsEnemy();
    }

    // ------------------------------------------------------------------ veterancy

    public int getRank() {
        return entityData.get(DATA_RANK);
    }

    public int getXp() {
        return xp;
    }

    /** Sets XP (and with it the rank) without fanfare: test tools, loading, returning heroes. */
    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
        int rank = com.warfront.army.Veterancy.ranks(getRole()) ? com.warfront.army.Veterancy.rankFor(this.xp) : 0;
        entityData.set(DATA_RANK, rank);
        applyRank();
    }

    /** Earned XP; a new rank brings a burst, a sound and a word to the commander. */
    public void addXp(int amount) {
        if (amount <= 0 || !com.warfront.army.Veterancy.ranks(getRole()) || level().isClientSide) return;
        int before = getRank();
        xp += amount;
        int rank = com.warfront.army.Veterancy.rankFor(xp);
        if (rank == before) return;
        entityData.set(DATA_RANK, rank);
        float healthFrac = getHealth() / getMaxHealth();
        applyRank();
        setHealth(getMaxHealth() * healthFrac);
        refreshName();
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, getX(), getY(1.0), getZ(), 20, 0.3, 0.5, 0.3, 0.2);
        }
        playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.3F);
        Player owner = getOwner();
        if (owner != null) {
            com.warfront.alert.Alerts.toast(owner, "rank_up", "Promoted: " + com.warfront.army.Veterancy.TITLES[rank],
                    "Your " + getUnitName() + " is now a " + com.warfront.army.Veterancy.TITLES[rank] + ".");
        }
    }

    private void applyRank() {
        double bonus = com.warfront.army.Veterancy.bonus(getRank());
        setModifier(Attributes.MAX_HEALTH, "rank_health", bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(Attributes.ATTACK_DAMAGE, "rank_damage", bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    /** Multiplier on this unit's arrows and bolts from its rank. */
    public double rangedBonus() {
        return 1.0 + com.warfront.army.Veterancy.bonus(getRank());
    }

    /** Well Fed units hold their nerve 10% better. */
    private float moraleLossFactor() {
        return hasEffect(WFRegistry.WELL_FED) ? 0.9f : 1f;
    }

    /** Scales health and damage (difficulty presets for enemies). 1 removes the scaling. */
    public void applyStrength(double multiplier) {
        setModifier(Attributes.MAX_HEALTH, "difficulty_health", multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(Attributes.ATTACK_DAMAGE, "difficulty_damage", multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setHealth(getMaxHealth());
    }

    private void setModifier(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                             String id, double amount, AttributeModifier.Operation op) {
        var inst = getAttribute(attribute);
        if (inst == null) return;
        var rl = com.warfront.Warfront.id(id);
        inst.removeModifier(rl);
        inst.addPermanentModifier(new AttributeModifier(rl, amount, op));
    }

    /**
     * Hands out weapons. Armor is drawn into each unit's skin and granted as an attribute, so the
     * unit designs stay visible; armor a player hands over is still worn and rendered.
     */
    private void equipLoadout() {
        boolean elite = tier >= 3;
        boolean iron = tier >= 2;
        clearLoadout();
        boolean claws = race == Race.HIVE;
        boolean axes = race == Race.DWARF || race == Race.ORC;
        Item blade = elite ? Items.DIAMOND_SWORD : Items.IRON_SWORD;
        Item axe = elite ? Items.DIAMOND_AXE : Items.IRON_AXE;
        switch (getRole()) {
            case SHIELDBEARER -> {
                if (!claws) {
                    gear(EquipmentSlot.MAINHAND, axes ? (iron ? Items.IRON_AXE : Items.STONE_AXE)
                            : iron ? Items.IRON_SWORD : Items.STONE_SWORD);
                    gear(EquipmentSlot.OFFHAND, Items.SHIELD);
                }
            }
            case SPEARMAN -> {
                if (!claws) gear(EquipmentSlot.MAINHAND, Items.TRIDENT);
            }
            case SWORDSMAN -> {
                if (!claws) gear(EquipmentSlot.MAINHAND, axes ? axe : blade);
            }
            case CAPTAIN -> {
                if (!claws) gear(EquipmentSlot.MAINHAND, axes ? axe : blade);
                gear(EquipmentSlot.OFFHAND, bannerFor(Factions.colorOf(level().getServer(), getFactionKey())));
            }
            case CHAMPION -> {
                switch (race) {
                    case HUMAN -> {
                        gear(EquipmentSlot.MAINHAND, blade);
                        gear(EquipmentSlot.OFFHAND, Items.SHIELD);
                    }
                    case ELF -> {
                        gear(EquipmentSlot.MAINHAND, blade);
                        gear(EquipmentSlot.OFFHAND, Items.IRON_SWORD);
                    }
                    case DWARF -> gear(EquipmentSlot.MAINHAND, Items.MACE);
                    case ORC -> {
                        gear(EquipmentSlot.MAINHAND, axe);
                        gear(EquipmentSlot.OFFHAND, Items.IRON_AXE);
                    }
                    case DEMON -> gear(EquipmentSlot.MAINHAND, Items.NETHERITE_SWORD);
                    case ANGEL -> gear(EquipmentSlot.MAINHAND, Items.GOLDEN_SWORD);
                    case HIVE -> { }
                }
            }
            case ARCHER -> {
                // Hive spitters and demon firecasters need no bow.
                if (!claws && race != Race.DEMON) gear(EquipmentSlot.MAINHAND, Items.BOW);
            }
            case HEALER -> gear(EquipmentSlot.MAINHAND, WFRegistry.HEALING_STAFF.get());
            case FARMER -> gear(EquipmentSlot.MAINHAND, iron ? Items.IRON_HOE : Items.STONE_HOE);
            case BUILDER -> gear(EquipmentSlot.MAINHAND, WFRegistry.MASON_HAMMER.get());
            case BEAST -> { }   // beasts fight with their own claws and jaws
            case GUARD -> {
                if (race == Race.DEMON) {
                    gear(EquipmentSlot.MAINHAND, Items.TRIDENT);   // imp sentries carry a pike
                } else if (!claws) {
                    gear(EquipmentSlot.MAINHAND, axes ? (iron ? Items.IRON_AXE : Items.STONE_AXE) : Items.IRON_SWORD);
                    gear(EquipmentSlot.OFFHAND, Items.SHIELD);
                }
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

    /** The unit's race as both server and client know it (derived from the synced skin). */
    public Race getVisualRace() {
        int skin = getSkin();
        Race[] races = Race.values();
        return skin < races.length ? races[skin] : NpcFaction.values()[skin - races.length].race;
    }

    /** The Deepmaw is a lobster centaur, long and broad, so it gets a wider hitbox than a humanoid. */
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (entityData != null && getBody() == com.warfront.army.UnitBody.HIVE_BEAST) {
            return EntityDimensions.scalable(1.1F, 1.7F).withEyeHeight(1.45F);
        }
        return super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ROLE.equals(key) || DATA_SKIN.equals(key)) refreshDimensions();
    }

    public com.warfront.army.UnitBody getBody() {
        return com.warfront.army.UnitBody.of(getVisualRace(), getRole());
    }

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

    public SimpleContainer getWorkItems() {
        return workItems;
    }

    @Nullable
    public Blueprint getBlueprint() {
        return blueprint;
    }

    /** Where a posted unit (worker, or a battle unit on duty) works or stands watch. */
    @Nullable
    public BlockPos getPost() {
        return isPosted() && anchor != null ? BlockPos.containing(anchor) : null;
    }

    /** Workers, plus battle units on guard or patrol duty: they keep a post and ignore the baton. */
    public boolean isPosted() {
        return getRole().posted() || duty != com.warfront.army.Duty.NONE;
    }

    /** Units that stand watch and raise the alarm: battle units on duty (and guards from older saves). */
    public boolean onWatch() {
        return duty != com.warfront.army.Duty.NONE || getRole() == SoldierRole.GUARD;
    }

    public com.warfront.army.Duty getDuty() {
        return getRole() == SoldierRole.GUARD && duty == com.warfront.army.Duty.NONE ? com.warfront.army.Duty.GUARD : duty;
    }

    /** Puts a battle unit on guard or patrol duty at a post, or (NONE) sends it back to its commander. */
    public void setDuty(com.warfront.army.Duty duty, Vec3 post, float yaw) {
        if (getRole().worker()) return;
        this.duty = duty;
        if (duty == com.warfront.army.Duty.NONE) {
            command(Order.FOLLOW, formation, null, yaw);
        } else {
            command(Order.HOLD, formation, post, yaw);
        }
    }

    /** Replaces a builder's blueprint (used by tests to survey a small area). */
    public void setBlueprint(@Nullable Blueprint blueprint) {
        this.blueprint = blueprint;
    }

    /** Re-surveys the base for a builder, or posts a unit where it stands. */
    public void assignPost(Vec3 where, float yaw) {
        command(Order.HOLD, formation, where, yaw);
        if (getRole() == SoldierRole.BUILDER && level() instanceof ServerLevel server) {
            blueprint = Blueprint.survey(server, BlockPos.containing(where));
        }
    }

    public boolean isMarchLeader() {
        return marchLeader;
    }

    /** This unit's race-specific name, e.g. "Axe Thane" for a dwarven swordsman. */
    public String getUnitName() {
        return com.warfront.army.UnitNames.of(race, getRole());
    }

    public boolean isWarlord() {
        return warlord;
    }

    /** Turns this soldier into a boss: a towering warlord with triple health. */
    public void makeWarlord() {
        warlord = true;
        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).addPermanentModifier(new AttributeModifier(
                com.warfront.Warfront.id("warlord_health"), 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        Objects.requireNonNull(getAttribute(Attributes.SCALE)).addPermanentModifier(new AttributeModifier(
                com.warfront.Warfront.id("warlord_scale"), 0.45, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        Objects.requireNonNull(getAttribute(Attributes.ATTACK_DAMAGE)).addPermanentModifier(new AttributeModifier(
                com.warfront.Warfront.id("warlord_attack"), 4.0, AttributeModifier.Operation.ADD_VALUE));
        Objects.requireNonNull(getAttribute(Attributes.KNOCKBACK_RESISTANCE)).addPermanentModifier(new AttributeModifier(
                com.warfront.Warfront.id("warlord_knockback"), 0.8, AttributeModifier.Operation.ADD_VALUE));
        setHealth(getMaxHealth());
        refreshName();
    }

    /** Where this soldier is trying to get to: its target, or its march/charge objective. */
    @Nullable
    public Vec3 currentObjective() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) return target.position();
        Order order = getOrder();
        if ((order == Order.CHARGE || order == Order.MARCH) && anchor != null) return anchor;
        return null;
    }

    public boolean isStuck() {
        return stuckSeconds >= 2;
    }

    public void resetStuck() {
        stuckSeconds = 0;
        lastProgressPos = position();
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

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && getRole().worker()) return;   // workers don't pick fights
        super.setTarget(target);
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
        if (onWatch() && order == Order.HOLD) {
            double reach = getDuty().beat + 8;
            return target.position().distanceToSqr(slot) < reach * reach;
        }
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
        if (isPosted()) {
            // Posted units keep their own post (HOLD) or trail their commander while being moved (FOLLOW).
            Player owner = getOwner();
            if (order == Order.FOLLOW && owner != null) {
                float rad = owner.getYRot() * Mth.DEG_TO_RAD;
                slot = owner.position().add(Mth.sin(rad) * 2.0, 0, -Mth.cos(rad) * 2.0);
                anchorYaw = owner.getYRot();
            } else {
                if (anchor == null) {
                    anchor = position();
                    anchorYaw = getYRot();
                }
                slot = anchor;
            }
            return;
        }
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
        List<SoldierRole> roles = new ArrayList<>(group.size());
        for (SoldierEntity s : group) roles.add(s.getRole());
        slot = FormationLayout.slot(formation, roles, group.indexOf(this), ref, yaw);
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
        if (isFallen()) {
            if (!level().isClientSide && level().getGameTime() >= fallenUntil) giveUpWaiting();
            super.aiStep();
            return;
        }
        if (!level().isClientSide && race == Race.HIVE && tickCount % 40 == 0) {
            com.warfront.world.HiveAdaptation.apply(this, 60);
        }
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
        trackProgress();

        if (race == Race.ANGEL && getHealth() < getMaxHealth()) {
            heal(0.5f);
        }

        // Morale recovers over time; faster near a captain.
        float regen = (float) race.moraleRegen * 2f;
        if (getRole() != SoldierRole.CAPTAIN && nearbyAllies(10, SoldierEntity.class).stream()
                .anyMatch(s -> s.getRole() == SoldierRole.CAPTAIN)) {
            regen += 4f;
        }
        morale = Mth.clamp(morale + regen, 0f, 100f);
        // Well Fed: wounds slowly close out of combat.
        if (getTarget() == null && hasEffect(WFRegistry.WELL_FED) && (tickCount / 20) % 5 == 0 && getHealth() < getMaxHealth()) heal(1f);

        if (race.routs && routTicks == 0 && !com.warfront.combat.Rage.isFrenzied(this)
                && !com.warfront.army.Veterancy.neverRouts(getRank()) && morale < 20f && getHealth() < getMaxHealth() * 0.4f) {
            routTicks = 120;
            setTarget(null);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.VILLAGER_HURT, SoundSource.HOSTILE, 1f, 0.8f);
        }

        if (getRole() == SoldierRole.CAPTAIN && (tickCount / 20) % 2 == 0) {
            rally();
        }
        if (getRole() == SoldierRole.CHAMPION) {
            championPulse();
        }
        if (getRole() == SoldierRole.HEALER && getHealth() < getMaxHealth()) {
            heal(1f);
        }
        if (onWatch()) {
            soundAlarm();
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

    private void trackProgress() {
        Vec3 objective = currentObjective();
        if (objective == null || position().distanceToSqr(objective) < 3.0 * 3.0) {
            resetStuck();
            return;
        }
        if (lastProgressPos != null && position().distanceToSqr(lastProgressPos) < 0.5 * 0.5) {
            stuckSeconds++;
        } else {
            stuckSeconds = 0;
            lastProgressPos = position();
        }
    }

    /** Once-a-second abilities of each race's Champion. */
    private void championPulse() {
        if (!(level() instanceof ServerLevel server)) return;
        switch (race) {
            case HUMAN -> {
                for (LivingEntity ally : nearbyAllies(6, LivingEntity.class)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false));
                }
            }
            case DEMON -> {
                for (LivingEntity foe : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3),
                        this::isEnemy)) {
                    foe.igniteForSeconds(3.0F);
                }
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, getX(), getY() + 1, getZ(),
                        6, 0.6, 0.6, 0.6, 0.01);
            }
            case ANGEL -> {
                if ((tickCount / 20) % 6 != 0) return;
                LivingEntity foe = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12),
                                this::isEnemy).stream()
                        .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
                if (foe == null) return;
                boolean holy = foe.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD) || Race.of(foe) == Race.DEMON;
                foe.hurt(damageSources().indirectMagic(this, this), holy ? 12.0F : 6.0F);
                Vec3 from = getEyePosition();
                Vec3 to = foe.getEyePosition();
                for (int i = 0; i <= 12; i++) {
                    Vec3 p = from.lerp(to, i / 12.0);
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
                playSound(SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F);
            }
            default -> { }
        }
    }

    /** A guard who has spotted an enemy rings out and calls nearby troops to the fight. */
    private void soundAlarm() {
        LivingEntity foe = getTarget();
        if (foe == null || !foe.isAlive() || level().getGameTime() < alarmQuietUntil) return;
        int radius = com.warfront.config.WFConfig.GUARD_ALARM_RADIUS.get();
        if (radius <= 0) return;
        alarmQuietUntil = level().getGameTime() + 200;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 1.6F, 1.1F);
        for (SoldierEntity ally : nearbyAllies(radius, SoldierEntity.class)) {
            if (!ally.getRole().worker() && ally.getTarget() == null && ally.isEnemy(foe)) ally.setTarget(foe);
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
        int rank = getRank();
        Component name = Component.literal("[" + Factions.displayName(server, key) + "] ")
                .withStyle(Factions.colorOf(server, key))
                .append(Component.literal(warlord ? race.displayName() + " Warlord"
                        : (rank > 0 ? com.warfront.army.Veterancy.TITLES[rank] + " " : "") + getUnitName())
                        .withStyle(warlord ? ChatFormatting.DARK_RED : ChatFormatting.WHITE));
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
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && race == Race.DEMON) target.igniteForSeconds(3.0F);
        if (hit && race == Race.HIVE && getRole() == SoldierRole.CHAMPION && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), this);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
        }
        return hit;
    }

    /** True while this soldier is an orc Berserker below half health. */
    public boolean isBerserk() {
        return race == Race.ORC && getRole() == SoldierRole.CHAMPION && getHealth() < getMaxHealth() * 0.5F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isFallen() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (!level().isClientSide && race == Race.ELF && getRole() == SoldierRole.CHAMPION
                && source.getEntity() != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && random.nextFloat() < 0.3F) {
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.6F);
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            morale = Math.max(0f, morale - amount * 2.5f * moraleLossFactor());
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel server) || !isDeadOrDying()) return;   // a hero fell instead

        float shock = getRole() == SoldierRole.CAPTAIN ? 35f : 12f;
        for (SoldierEntity ally : nearbyAllies(12, SoldierEntity.class)) {
            ally.morale = Math.max(0f, ally.morale - shock * ally.moraleLossFactor());
        }

        for (ItemStack carried : workItems.removeAllItems()) spawnAtLocation(carried);
        Player owner = getOwner();
        if (owner != null) {
            owner.displayClientMessage(Component.literal("Your " + getUnitName() + " has fallen.").withStyle(ChatFormatting.RED), true);
        } else if (source.getEntity() != null) {
            int marks = 1 + random.nextInt(2) + tier / 2 + (getRole() == SoldierRole.CAPTAIN ? 3 : 0)
                    + (warlord ? 12 : 0);
            spawnAtLocation(new ItemStack(WFRegistry.WAR_MARK.get(), marks));
            if (random.nextFloat() < (warlord ? 1.0F : 0.35F)) {
                spawnAtLocation(new ItemStack(WFRegistry.MANA_SHARD.get(), warlord ? 8 : 1 + random.nextInt(2)));
            }
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
        if (com.warfront.war.Bounties.tryFree(player, this)) return InteractionResult.CONSUME;
        if (isFallen()) {
            revive();
            player.displayClientMessage(Component.literal("Your " + getUnitName() + " gets back on its feet.").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!getRole().posted() && player.isShiftKeyDown() && held.isEmpty()) {
            // Sneak + empty hand on a battle unit: guard here, then patrol here, then back to the army.
            com.warfront.army.Duty next = com.warfront.army.Duty.byOrdinal(getDuty().ordinal() + 1);
            setDuty(next, position(), player.getYRot());
            String what = switch (next) {
                case GUARD -> " stands guard here. Sneak + right-click again to have it patrol instead.";
                case PATROL -> " patrols around here. Sneak + right-click again to call it back to the army.";
                case NONE -> " rejoins your army and follows you.";
            };
            player.displayClientMessage(Component.literal("Your " + getUnitName() + what).withStyle(ChatFormatting.GOLD), true);
            playSound(SoundEvents.VILLAGER_YES, 0.8F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (getRole().posted() && player.isShiftKeyDown() && held.isEmpty()) {
            // Sneak + empty hand: pick a posted unit up to move it, or set it down at a new post.
            if (getOrder() == Order.HOLD) {
                command(Order.FOLLOW, formation, null, player.getYRot());
                player.displayClientMessage(Component.literal("Your " + getUnitName()
                        + " follows you. Sneak + right-click it again to post it.").withStyle(ChatFormatting.GOLD), true);
            } else {
                assignPost(position(), player.getYRot());
                player.displayClientMessage(Component.literal("Your " + getUnitName() + " takes up its post here"
                        + (blueprint != null ? " and surveys " + blueprint.size() + " blocks." : ".")).withStyle(ChatFormatting.GOLD), true);
            }
            playSound(SoundEvents.VILLAGER_YES, 0.8F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty() && getRole().worker() && getEquipmentSlotForItem(held).getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            // Hand a worker seeds or building blocks.
            ItemStack rest = WorkSites.insert(workItems, held.copy());
            int given = held.getCount() - rest.getCount();
            if (given > 0) {
                held.shrink(given);
                playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.2F);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
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
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(workStatus()).withStyle(ChatFormatting.DARK_AQUA)), true);
        return InteractionResult.CONSUME;
    }

    private String workStatus() {
        if (!getRole().posted()) return duty == com.warfront.army.Duty.NONE ? "" : "  " + duty.title;
        int carried = 0;
        for (int i = 0; i < workItems.getContainerSize(); i++) carried += workItems.getItem(i).getCount();
        String s = getOrder() == Order.HOLD ? "  on post" : "  following";
        if (getRole().worker()) s += ", carrying " + carried;
        if (blueprint != null) s += ", " + blueprint.countDamage(level()) + "/" + blueprint.size() + " blocks to rebuild";
        return s;
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
        tag.putInt("Xp", xp);
        if (isFallen()) tag.putLong("FallenUntil", fallenUntil);
        tag.putInt("Given", givenMask);
        tag.putBoolean("Configured", configured);
        tag.putBoolean("Warlord", warlord);
        if (anchor != null) {
            tag.putDouble("AnchorX", anchor.x);
            tag.putDouble("AnchorY", anchor.y);
            tag.putDouble("AnchorZ", anchor.z);
        }
        tag.putFloat("AnchorYaw", anchorYaw);
        if (duty != com.warfront.army.Duty.NONE) tag.putInt("Duty", duty.ordinal());
        if (siegeTarget != null) tag.putLong("SiegeTarget", siegeTarget.asLong());
        if (!workItems.isEmpty()) tag.put("WorkItems", workItems.createTag(registryAccess()));
        if (blueprint != null) tag.put("Blueprint", blueprint.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_ROLE, tag.getInt("Role"));
        entityData.set(DATA_SKIN, tag.getInt("Skin"));
        entityData.set(DATA_ORDER, tag.getInt("Order"));
        duty = com.warfront.army.Duty.byOrdinal(tag.getInt("Duty"));
        if (tag.contains("Faction")) entityData.set(DATA_FACTION, tag.getString("Faction"));
        entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        warbandId = tag.hasUUID("Warband") ? tag.getUUID("Warband") : null;
        Race r = Race.byId(tag.getString("Race"));
        race = r != null ? r : Race.HUMAN;
        formation = Formation.byOrdinal(tag.getInt("Formation"));
        tier = tag.getInt("Tier");
        morale = tag.contains("Morale") ? tag.getFloat("Morale") : 100f;
        xp = tag.getInt("Xp");
        if (tag.contains("FallenUntil")) {
            entityData.set(DATA_FALLEN, true);
            fallenUntil = tag.getLong("FallenUntil");
        }
        entityData.set(DATA_RANK, com.warfront.army.Veterancy.ranks(getRole()) ? com.warfront.army.Veterancy.rankFor(xp) : 0);
        givenMask = tag.getInt("Given");
        configured = tag.getBoolean("Configured");
        warlord = tag.getBoolean("Warlord");
        anchor = tag.contains("AnchorX")
                ? new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ")) : null;
        anchorYaw = tag.getFloat("AnchorYaw");
        siegeTarget = tag.contains("SiegeTarget") ? BlockPos.of(tag.getLong("SiegeTarget")) : null;
        workItems.clearContent();
        if (tag.contains("WorkItems")) workItems.fromTag(tag.getList("WorkItems", net.minecraft.nbt.Tag.TAG_COMPOUND), registryAccess());
        blueprint = tag.contains("Blueprint")
                ? Blueprint.load(tag.getCompound("Blueprint"), level().holderLookup(net.minecraft.core.registries.Registries.BLOCK)) : null;
    }
}
