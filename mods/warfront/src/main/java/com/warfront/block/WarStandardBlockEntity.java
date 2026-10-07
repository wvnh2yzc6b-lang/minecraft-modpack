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
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks a stronghold's integrity and its sieges.
 * <p>
 * A single siege comes at night on its own. Sounding a War Horn starts a <b>wave campaign</b>:
 * endless, escalating waves with a short break between them, tracked on a boss bar. From wave 4
 * attackers come from several directions, and every 5th wave is led by a Warlord.
 */
public class WarStandardBlockEntity extends BlockEntity {
    private static final double BAR_RANGE = 96;

    @Nullable private UUID owner;
    private int health = -1;
    private int wave;
    /** Siege waves beaten at this standard, ever. Counts toward the base's level; lost if the standard falls. */
    private int wavesWon;
    @Nullable private UUID warband;
    private int waveSize;
    /** Soldiers the latest wave actually spawned (not saved). */
    private int lastSpawned;
    private long siegeStart;
    private long lastSiege;
    private boolean campaign;
    private long nextWaveAt;
    @Nullable private String attackerName;

    @Nullable private ServerBossEvent bar;

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
        FactionData.Faction f = FactionData.get(level.getServer()).factionOf(owner);
        return f != null && f.members.contains(player.getUUID());
    }

    public boolean isUnderSiege() {
        return warband != null;
    }

    public boolean isCampaignActive() {
        return campaign;
    }

    @Nullable
    public UUID getWarbandId() {
        return warband;
    }

    public int getLastSpawned() {
        return lastSpawned;
    }

    public int getWave() {
        return wave;
    }

    public int getWavesWon() {
        return wavesWon;
    }

    public void setWavesWon(int wavesWon) {
        this.wavesWon = Math.max(0, wavesWon);
        setChanged();
    }

    /** The faction this standard belongs to. */
    public String factionKey(@Nullable net.minecraft.server.MinecraftServer server) {
        if (owner == null) return com.warfront.faction.Factions.WILD;
        return server == null ? "p:" + owner : FactionData.get(server).keyOf(owner);
    }

    private int maxHealth() {
        return WFConfig.STANDARD_HEALTH.get();
    }

    private int health() {
        if (health < 0) health = maxHealth();
        return health;
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, WarStandardBlockEntity be) {
        ServerLevel server = (ServerLevel) level;
        long time = level.getGameTime();

        if (be.warband != null && time % 20 == 0) {
            int left = be.countAttackers(server);
            if (left == 0 || time - be.siegeStart > 12000) {
                be.victory(server);
            } else {
                be.updateBar(server, left);
            }
        }

        if (be.warband == null && be.campaign && time % 20 == 0) {
            if (time >= be.nextWaveAt) {
                be.startSiege(server);
            } else {
                be.updateBar(server, 0);
            }
        }

        if (be.warband == null && !be.campaign && time % 600 == 0) {
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

    private int countAttackers(ServerLevel level) {
        UUID id = warband;
        if (id == null) return 0;
        return level.getEntitiesOfClass(SoldierEntity.class, new AABB(worldPosition).inflate(BAR_RANGE),
                s -> s.isAlive() && id.equals(s.getWarbandId())).size();
    }

    private boolean ownerNearby(ServerLevel level) {
        if (owner == null) return false;
        Player p = level.getPlayerByUUID(owner);
        return p != null && p.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 64 * 64;
    }

    // ------------------------------------------------------------------ campaign control

    /** Starts an endless wave campaign. Returns false if one is already running. */
    public boolean startCampaign(ServerLevel level) {
        if (campaign) return false;
        campaign = true;
        setChanged();
        announce(level, Component.literal("The war horn sounds! Hold the standard against endless waves. "
                + "Sneak and sound the horn again to stand down after the current wave.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (warband == null) startSiege(level);
        return true;
    }

    public void stopCampaign(ServerLevel level) {
        if (!campaign) return;
        campaign = false;
        setChanged();
        announce(level, Component.literal("The campaign ends after " + (warband != null ? wave - 1 : wave)
                + " waves survived.").withStyle(ChatFormatting.GOLD));
        if (warband == null) clearBar();
    }

    /** Begins the next siege wave. Returns false if a wave is already underway. */
    public boolean startSiege(ServerLevel level) {
        if (warband != null) return false;
        wave++;
        BlockPos biomePos = worldPosition;
        NpcFaction faction = NpcFaction.pick(level.getBiome(biomePos), level, level.random);
        List<SoldierRole> roles = WarbandSpawner.siegeComposition(wave, faction);
        int tier = Math.min(4, 1 + wave / 3);
        int groups = wave >= 8 ? 3 : wave >= 4 ? 2 : 1;

        UUID id = UUID.randomUUID();
        float baseAngle = level.random.nextFloat() * Mth.TWO_PI;
        List<String> fronts = new ArrayList<>();
        List<SoldierEntity> spawned = new ArrayList<>();
        for (int g = 0; g < groups; g++) {
            float angle = baseAngle + g * Mth.TWO_PI / groups + (level.random.nextFloat() - 0.5F) * 0.6F;
            double dist = 36 + level.random.nextInt(8);
            int x = worldPosition.getX() + Mth.floor(Mth.cos(angle) * dist);
            int z = worldPosition.getZ() + Mth.floor(Mth.sin(angle) * dist);
            BlockPos spawn = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            List<SoldierRole> share = new ArrayList<>();
            for (int i = g; i < roles.size(); i += groups) share.add(roles.get(i));
            spawned.addAll(WarbandSpawner.spawnInto(level, id, faction, share, spawn,
                    Vec3.atBottomCenterOf(worldPosition), worldPosition, tier));
            fronts.add(direction(angle));
        }

        boolean bossWave = wave % 5 == 0;
        if (bossWave) {
            spawned.stream().filter(s -> s.getRole() == SoldierRole.CAPTAIN).findFirst()
                    .or(() -> spawned.stream().findFirst())
                    .ifPresent(SoldierEntity::makeWarlord);
        }

        warband = id;
        lastSpawned = spawned.size();
        waveSize = Math.max(1, spawned.size());
        attackerName = faction.displayName;
        siegeStart = level.getGameTime();
        setChanged();

        level.playSound(null, worldPosition, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 64.0F, 1.0F);
        announce(level, Component.literal("Wave " + wave + (bossWave ? " (WARLORD)" : "") + ": the "
                + faction.displayName + " attack with " + spawned.size() + " soldiers from the "
                + String.join(" and ", fronts) + "!").withStyle(faction.color, ChatFormatting.BOLD));
        updateBar(level, spawned.size());
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
        wavesWon++;

        int marks = 4 + wave * 2;
        Block.popResource(level, worldPosition.above(), new ItemStack(WFRegistry.WAR_MARK.get(), marks));
        Block.popResource(level, worldPosition.above(), new ItemStack(WFRegistry.MANA_SHARD.get(), 2 + wave));
        if (wave % 5 == 0) Block.popResource(level, worldPosition.above(), new ItemStack(Items.DIAMOND, wave / 5));
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                worldPosition.getZ() + 0.5, 40, 0.5, 1.0, 0.5, 0.4);
        level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 0.8F);

        String next = "";
        if (campaign) {
            int pause = WFConfig.WAVE_INTERMISSION.get();
            nextWaveAt = level.getGameTime() + pause;
            next = " Next wave in " + pause / 20 + "s.";
        } else {
            clearBar();
        }
        setChanged();
        announce(level, Component.literal("Wave " + wave + " repelled! (+" + marks + " War Marks)" + next)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    /** Called when an attacker reaches the standard and strikes it. */
    public void takeHit(SoldierEntity attacker) {
        if (!(level instanceof ServerLevel server)) return;
        health = health() - (attacker.isWarlord() ? 3 : 1);
        setChanged();
        server.sendParticles(ParticleTypes.CRIT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 8, 0.3, 0.5, 0.3, 0.2);
        server.playSound(null, worldPosition, SoundEvents.SHIELD_BREAK, SoundSource.BLOCKS, 0.7F, 0.8F);
        if (health % 5 == 0 && health > 0) {
            announce(server, Component.literal("Your War Standard is under attack! (" + health + "/" + maxHealth()
                    + ")").withStyle(ChatFormatting.RED));
        }
        if (health <= 0) {
            announce(server, Component.literal("The War Standard has fallen on wave " + wave + ".")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            warband = null;
            campaign = false;
            clearBar();
            server.destroyBlock(worldPosition, true);
        }
    }

    // ------------------------------------------------------------------ boss bar

    private void updateBar(ServerLevel level, int attackersLeft) {
        if (bar == null) {
            bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        }
        if (warband != null) {
            bar.setName(Component.literal("Wave " + wave + " - " + attackerName + " - " + attackersLeft
                    + " remaining  |  Standard " + health() + "/" + maxHealth()));
            bar.setColor(wave % 5 == 0 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
            bar.setProgress(Mth.clamp(attackersLeft / (float) waveSize, 0F, 1F));
        } else {
            long left = Math.max(0, nextWaveAt - level.getGameTime());
            bar.setName(Component.literal("Wave " + (wave + 1) + " in " + (left / 20) + "s  |  Standard "
                    + health() + "/" + maxHealth()));
            bar.setColor(BossEvent.BossBarColor.GREEN);
            bar.setProgress(Mth.clamp(left / (float) WFConfig.WAVE_INTERMISSION.get(), 0F, 1F));
        }
        Vec3 c = Vec3.atCenterOf(worldPosition);
        for (ServerPlayer p : level.players()) {
            boolean near = p.distanceToSqr(c) < BAR_RANGE * BAR_RANGE;
            if (near && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            else if (!near && bar.getPlayers().contains(p)) bar.removePlayer(p);
        }
    }

    private void clearBar() {
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    @Override
    public void setRemoved() {
        clearBar();
        super.setRemoved();
    }

    // ------------------------------------------------------------------ misc

    public void describeTo(Player player) {
        String status = warband != null ? "UNDER SIEGE (wave " + wave + ")"
                : campaign ? "Campaign: next wave soon" : "Peaceful";
        player.displayClientMessage(Component.literal("War Standard: " + status + "  |  Integrity " + health() + "/"
                + maxHealth() + "  |  Waves won: " + wavesWon)
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
        tag.putInt("WavesWon", wavesWon);
        tag.putInt("WaveSize", waveSize);
        tag.putLong("SiegeStart", siegeStart);
        tag.putLong("LastSiege", lastSiege);
        tag.putBoolean("Campaign", campaign);
        tag.putLong("NextWaveAt", nextWaveAt);
        if (attackerName != null) tag.putString("Attacker", attackerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        warband = tag.hasUUID("Warband") ? tag.getUUID("Warband") : null;
        health = tag.contains("Health") ? tag.getInt("Health") : -1;
        wave = tag.getInt("Wave");
        wavesWon = tag.contains("WavesWon") ? tag.getInt("WavesWon") : Math.max(0, warband != null ? wave - 1 : wave);
        waveSize = tag.getInt("WaveSize");
        siegeStart = tag.getLong("SiegeStart");
        lastSiege = tag.getLong("LastSiege");
        campaign = tag.getBoolean("Campaign");
        nextWaveAt = tag.getLong("NextWaveAt");
        attackerName = tag.contains("Attacker") ? tag.getString("Attacker") : null;
    }
}
