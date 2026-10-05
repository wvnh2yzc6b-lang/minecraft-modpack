package com.warfront.block;

import com.warfront.army.SoldierRole;
import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.FactionData;
import com.warfront.faction.NpcFaction;
import com.warfront.registry.WFRegistry;
import com.warfront.world.WarbandSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Tracks a stronghold's health and its sieges: wave number, the attacking warband, rewards. */
public class WarStandardBlockEntity extends BlockEntity {
    @Nullable private UUID owner;
    private int health = -1;
    private int wave;
    @Nullable private UUID warband;
    private long siegeStart;
    private long lastSiege;

    public WarStandardBlockEntity(BlockPos pos, BlockState state) {
        super(WFRegistry.WAR_STANDARD_BE.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean isDefender(Player player) {
        if (owner == null || owner.equals(player.getUUID())) return true;
        if (level == null || level.getServer() == null) return false;
        FactionData data = FactionData.get(level.getServer());
        FactionData.Faction f = data.factionOf(owner);
        return f != null && f.members.contains(player.getUUID());
    }

    public boolean isUnderSiege() {
        return warband != null;
    }

    private int maxHealth() {
        return WFConfig.STANDARD_HEALTH.get();
    }

    private int health() {
        if (health < 0) health = maxHealth();
        return health;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WarStandardBlockEntity be) {
        ServerLevel server = (ServerLevel) level;
        long time = level.getGameTime();

        if (be.warband != null && time % 20 == 0) {
            UUID id = be.warband;
            List<SoldierEntity> left = level.getEntitiesOfClass(SoldierEntity.class, new AABB(pos).inflate(96),
                    s -> s.isAlive() && id.equals(s.getWarbandId()));
            if (left.isEmpty() || time - be.siegeStart > 12000) {
                be.victory(server);
            }
        }

        if (be.warband == null && time % 600 == 0) {
            if (be.health() < be.maxHealth()) {
                be.health++;
                be.setChanged();
            }
            if (WFConfig.NATURAL_SIEGES.get() && server.isNight() && be.ownerNearby(server)
                    && time - be.lastSiege > WFConfig.SIEGE_COOLDOWN.get()
                    && server.random.nextDouble() < WFConfig.SIEGE_CHANCE.get()) {
                be.startSiege(server);
            }
        }
    }

    private boolean ownerNearby(ServerLevel level) {
        if (owner == null) return false;
        Player p = level.getPlayerByUUID(owner);
        return p != null && p.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 64 * 64;
    }

    /** Begins the next siege wave. Returns false if a siege is already underway. */
    public boolean startSiege(ServerLevel level) {
        if (warband != null) return false;
        wave++;
        NpcFaction faction = NpcFaction.values()[level.random.nextInt(NpcFaction.values().length)];
        List<SoldierRole> roles = WarbandSpawner.siegeComposition(wave);
        int tier = Math.min(4, 1 + wave / 3);

        float angle = level.random.nextFloat() * Mth.TWO_PI;
        double dist = 36 + level.random.nextInt(8);
        int x = worldPosition.getX() + Mth.floor(Mth.cos(angle) * dist);
        int z = worldPosition.getZ() + Mth.floor(Mth.sin(angle) * dist);
        BlockPos spawn = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));

        warband = WarbandSpawner.spawn(level, faction, roles, spawn, Vec3.atBottomCenterOf(worldPosition),
                worldPosition, tier);
        siegeStart = level.getGameTime();
        setChanged();

        level.playSound(null, worldPosition, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 64.0F, 1.0F);
        announce(level, Component.literal("Siege! Wave " + wave + ": the " + faction.displayName + " march on your "
                        + "War Standard with " + roles.size() + " soldiers from the " + direction(angle) + "!")
                .withStyle(faction.color, ChatFormatting.BOLD));
        return true;
    }

    private static String direction(float angle) {
        String[] dirs = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
        int i = Math.floorMod(Math.round(angle / (Mth.TWO_PI / 8)), 8);
        return dirs[i];
    }

    private void victory(ServerLevel level) {
        warband = null;
        lastSiege = level.getGameTime();
        health = maxHealth();
        setChanged();

        int marks = 4 + wave * 2;
        Block.popResource(level, worldPosition.above(), new ItemStack(WFRegistry.WAR_MARK.get(), marks));
        if (wave % 5 == 0) Block.popResource(level, worldPosition.above(), new ItemStack(Items.DIAMOND, wave / 5));
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                worldPosition.getZ() + 0.5, 40, 0.5, 1.0, 0.5, 0.4);
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 0.8F);
        announce(level, Component.literal("Wave " + wave + " repelled! The standard still flies. (+" + marks
                + " War Marks)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    /** Called when an attacker reaches the standard and strikes it. */
    public void takeHit(SoldierEntity attacker) {
        if (!(level instanceof ServerLevel server)) return;
        health = health() - 1;
        setChanged();
        server.sendParticles(ParticleTypes.CRIT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 8, 0.3, 0.5, 0.3, 0.2);
        server.playSound(null, worldPosition, SoundEvents.SHIELD_BREAK, SoundSource.BLOCKS, 0.7F, 0.8F);
        if (health % 5 == 0 && health > 0) {
            announce(server, Component.literal("Your War Standard is under attack! (" + health + "/" + maxHealth()
                    + ")").withStyle(ChatFormatting.RED));
        }
        if (health <= 0) {
            announce(server, Component.literal("The War Standard has fallen after " + (wave - 1)
                    + " successful defences.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            warband = null;
            server.destroyBlock(worldPosition, true);
        }
    }

    public void describeTo(Player player) {
        String status = warband != null ? "UNDER SIEGE (wave " + wave + ")" : "Peaceful";
        player.displayClientMessage(Component.literal("War Standard: " + status + "  |  Integrity " + health() + "/"
                + maxHealth() + "  |  Waves survived: " + Math.max(0, warband != null ? wave - 1 : wave))
                .withStyle(warband != null ? ChatFormatting.RED : ChatFormatting.GOLD), true);
    }

    private void announce(ServerLevel level, Component message) {
        Vec3 c = Vec3.atCenterOf(worldPosition);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(c) < 128 * 128 || Objects.equals(p.getUUID(), owner)) {
                p.sendSystemMessage(message);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        if (warband != null) tag.putUUID("Warband", warband);
        tag.putInt("Health", health);
        tag.putInt("Wave", wave);
        tag.putLong("SiegeStart", siegeStart);
        tag.putLong("LastSiege", lastSiege);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        warband = tag.hasUUID("Warband") ? tag.getUUID("Warband") : null;
        health = tag.contains("Health") ? tag.getInt("Health") : -1;
        wave = tag.getInt("Wave");
        siegeStart = tag.getLong("SiegeStart");
        lastSiege = tag.getLong("LastSiege");
    }
}
