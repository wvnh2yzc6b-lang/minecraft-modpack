package com.warfront.war;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.network.AdvisorLinePayload;
import com.warfront.registry.WFRegistry;
import com.warfront.world.WarbandSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The advisor's daily bounties and the raider camps. Each morning the advisor posts up to three bounties: clear a
 * raider camp, scout a marked spot, deliver materials, or rescue a captured unit. They pay in mana and veterancy XP
 * for the units that took part, and expire after two days. Raider camps sit 100 to 300 blocks from the base, tied to
 * that faction's next raid: take the camp and the raid comes a third smaller, with part of its loot left behind.
 * Camps not taken in three days break up and join their raid.
 */
public final class Bounties {
    public static final int MAX_OFFERED = 3;
    public static final int BOUNTY_DAYS = 2;
    public static final int CAMP_DAYS = 3;
    /** XP for each unit near the player when a bounty is done. */
    public static final int BOUNTY_XP = 30;
    private static final String CAPTIVE = "warfront_captive";

    private Bounties() {}

    public enum Type { CAMP, SCOUT, DELIVER, RESCUE }

    /** One bounty. {@code camp} links CAMP and RESCUE bounties to their camp. */
    public record Bounty(UUID id, Type type, BlockPos pos, String item, int count, @Nullable UUID camp, long expiresAt,
                         int shards, int crystals) {
        public String describe() {
            return switch (type) {
                case CAMP -> "Clear the raider camp at " + pos.getX() + ", " + pos.getZ();
                case SCOUT -> "Scout the spot at " + pos.getX() + ", " + pos.getZ();
                case DELIVER -> "Bring me " + count + " " + itemName();
                case RESCUE -> "Rescue the captive held at " + pos.getX() + ", " + pos.getZ();
            };
        }

        public String itemName() {
            Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(item));
            return new ItemStack(it).getHoverName().getString();
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putString("Type", type.name());
            t.putLong("Pos", pos.asLong());
            t.putString("Item", item);
            t.putInt("Count", count);
            if (camp != null) t.putUUID("Camp", camp);
            t.putLong("Expires", expiresAt);
            t.putInt("Shards", shards);
            t.putInt("Crystals", crystals);
            return t;
        }

        @Nullable
        static Bounty load(CompoundTag t) {
            try {
                return new Bounty(t.getUUID("Id"), Type.valueOf(t.getString("Type")), BlockPos.of(t.getLong("Pos")),
                        t.getString("Item"), t.getInt("Count"), t.hasUUID("Camp") ? t.getUUID("Camp") : null,
                        t.getLong("Expires"), t.getInt("Shards"), t.getInt("Crystals"));
            } catch (RuntimeException e) {
                return null;
            }
        }
    }

    /** A raider camp, built once someone comes near it. */
    public static class Camp {
        public UUID id;
        public UUID owner;
        public NpcFaction faction;
        public BlockPos center;
        public String dim;
        public long expiresAt;
        public boolean built;
        public boolean rescue;
        public int raiders;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putUUID("Owner", owner);
            t.putString("Faction", faction.name());
            t.putLong("Center", center.asLong());
            t.putString("Dim", dim);
            t.putLong("Expires", expiresAt);
            t.putBoolean("Built", built);
            t.putBoolean("Rescue", rescue);
            t.putInt("Raiders", raiders);
            return t;
        }

        @Nullable
        static Camp load(CompoundTag t) {
            try {
                Camp c = new Camp();
                c.id = t.getUUID("Id");
                c.owner = t.getUUID("Owner");
                c.faction = NpcFaction.valueOf(t.getString("Faction"));
                c.center = BlockPos.of(t.getLong("Center"));
                c.dim = t.getString("Dim");
                c.expiresAt = t.getLong("Expires");
                c.built = t.getBoolean("Built");
                c.rescue = t.getBoolean("Rescue");
                c.raiders = t.getInt("Raiders");
                return c;
            } catch (RuntimeException e) {
                return null;
            }
        }
    }

    private static final Item[] DELIVERIES = {Items.OAK_LOG, Items.IRON_INGOT, Items.BREAD, Items.COBBLESTONE,
            Items.LEATHER, Items.WHEAT, Items.STRING, Items.COPPER_INGOT};

    // ------------------------------------------------------------------ offering

    /** Tops the player's bounties up to three (drops expired ones first). Returns how many are new. */
    public static int offer(ServerPlayer player) {
        WarState war = WarState.get(player.server);
        long now = player.server.overworld().getGameTime();
        expire(war, player.getUUID(), now);
        List<Bounty> list = war.bounties(player.getUUID());
        int added = 0;
        RandomSource r = player.getRandom();
        WarState.Clock clock = war.clock(player.getUUID());
        BlockPos home = clock.home != null ? clock.home : player.blockPosition();
        while (list.size() < MAX_OFFERED) {
            Type type = Type.values()[r.nextInt(Type.values().length)];
            if ((type == Type.CAMP || type == Type.RESCUE) && clock.home == null) type = r.nextBoolean() ? Type.SCOUT : Type.DELIVER;
            long expires = now + BOUNTY_DAYS * (long) RaidScheduler.DAY;
            Bounty b = switch (type) {
                case CAMP, RESCUE -> {
                    Camp camp = planCamp(player, home, type == Type.RESCUE, null, r);
                    war.addCamp(camp);
                    yield new Bounty(UUID.randomUUID(), type, camp.center, "", 0, camp.id, expires, 12 + r.nextInt(8), 1 + r.nextInt(2));
                }
                case SCOUT -> new Bounty(UUID.randomUUID(), type, around(home, 120, 260, r), "", 0, null, expires, 6 + r.nextInt(6), 0);
                case DELIVER -> {
                    Item item = DELIVERIES[r.nextInt(DELIVERIES.length)];
                    int count = item == Items.IRON_INGOT || item == Items.COPPER_INGOT ? 16 : 64;
                    yield new Bounty(UUID.randomUUID(), type, home, BuiltInRegistries.ITEM.getKey(item).toString(), count,
                            null, expires, 8 + r.nextInt(6), r.nextInt(2));
                }
            };
            list.add(b);
            added++;
        }
        war.setDirty();
        return added;
    }

    private static BlockPos around(BlockPos home, int min, int max, RandomSource r) {
        float angle = r.nextFloat() * Mth.TWO_PI;
        int dist = min + r.nextInt(Math.max(1, max - min));
        return new BlockPos(home.getX() + Mth.floor(Mth.cos(angle) * dist), home.getY(), home.getZ() + Mth.floor(Mth.sin(angle) * dist));
    }

    /** Plans a camp 100 to 300 blocks from home (or at {@code at}); it is built when someone comes near. */
    public static Camp planCamp(ServerPlayer player, BlockPos home, boolean rescue, @Nullable BlockPos at, RandomSource r) {
        WarState.Clock clock = WarState.get(player.server).clock(player.getUUID());
        ServerLevel level = clock.homeDim != null && player.server.getLevel(clock.homeDim) != null
                ? player.server.getLevel(clock.homeDim) : player.serverLevel();
        Camp c = new Camp();
        c.id = UUID.randomUUID();
        c.owner = player.getUUID();
        c.center = at != null ? at : around(home, 100, 300, r);
        c.dim = level.dimension().location().toString();
        c.faction = NpcFaction.pick(level.getBiome(c.center), level, r);
        c.expiresAt = player.server.overworld().getGameTime() + CAMP_DAYS * (long) RaidScheduler.DAY;
        c.rescue = rescue;
        // The camp is the vanguard of that faction's next raid.
        clock.nextFaction = c.faction.name();
        return c;
    }

    // ------------------------------------------------------------------ the camps

    /** Builds a camp: two small tents, a campfire, 4 to 8 raiders and maybe a bound captive. */
    public static void build(ServerLevel level, Camp camp, @Nullable Player owner) {
        BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, camp.center);
        camp.center = g;
        level.setBlock(g, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);
        for (int side = -1; side <= 1; side += 2) {
            BlockPos t = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, g.offset(side * 4, 0, 2));
            var wool = (side < 0 ? Blocks.BROWN_WOOL : Blocks.WHITE_WOOL).defaultBlockState();
            for (int dz = 0; dz < 3; dz++) {
                level.setBlock(t.offset(-1, 0, dz), wool, 3);
                level.setBlock(t.offset(1, 0, dz), wool, 3);
                level.setBlock(t.offset(0, 1, dz), wool, 3);
            }
        }
        RandomSource r = level.random;
        int n = 4 + r.nextInt(5);
        List<SoldierRole> roles = new ArrayList<>(WarbandSpawner.raidComposition(r, camp.faction, 1, WarState.get(level.getServer()).preset()));
        while (roles.size() > n) roles.remove(roles.size() - 1);
        while (roles.size() < n) roles.add(SoldierRole.SWORDSMAN);
        for (SoldierEntity s : WarbandSpawner.spawnInto(level, camp.id, camp.faction, roles, g, Vec3.atBottomCenterOf(g), null, 1)) {
            s.setPersistenceRequired();
        }
        camp.raiders = roles.size();
        if (camp.rescue && owner != null) {
            SoldierEntity captive = WFRegistry.SOLDIER.get().create(level);
            if (captive != null) {
                BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, g.offset(0, 0, -3));
                captive.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
                Race race = Race.byId(owner.getData(WFRegistry.RACE));
                captive.setupAsRecruit(owner, SoldierRole.SWORDSMAN, race == null ? Race.HUMAN : race);
                bind(captive, camp.id);
                level.addFreshEntity(captive);
            }
        }
        camp.built = true;
    }

    public static void bind(SoldierEntity s, UUID camp) {
        s.getPersistentData().putUUID(CAPTIVE, camp);
        s.setNoAi(true);
        s.setInvulnerable(true);
    }

    public static boolean isCaptive(SoldierEntity s) {
        return s.getPersistentData().hasUUID(CAPTIVE);
    }

    /** Right-clicking a captive: freed if the camp's guards are beaten. Returns true if handled. */
    public static boolean tryFree(Player player, SoldierEntity captive) {
        if (!isCaptive(captive) || !(player.level() instanceof ServerLevel level)) return false;
        UUID campId = captive.getPersistentData().getUUID(CAPTIVE);
        if (guardsLeft(level, campId, captive.position()) > 0) {
            player.displayClientMessage(Component.literal("Beat the camp's guards first.").withStyle(ChatFormatting.RED), true);
            return true;
        }
        captive.getPersistentData().remove(CAPTIVE);
        captive.setNoAi(false);
        captive.setInvulnerable(false);
        player.displayClientMessage(Component.literal("Freed! Your " + captive.getUnitName() + " joins you.").withStyle(ChatFormatting.GREEN), true);
        if (player instanceof ServerPlayer sp) completeLinked(sp, campId, Type.RESCUE);
        return true;
    }

    public static int guardsLeft(Level level, UUID campId, Vec3 near) {
        return level.getEntitiesOfClass(SoldierEntity.class, new AABB(BlockPos.containing(near)).inflate(48),
                s -> s.isAlive() && campId.equals(s.getWarbandId())).size();
    }

    /** A camp was taken: loot, the linked raid shrinks, and its bounty is done. */
    public static void campTaken(ServerLevel level, Camp camp) {
        WarState war = WarState.get(level.getServer());
        WarState.Clock clock = war.clock(camp.owner);
        clock.shrinkFaction = camp.faction.name();
        level.setBlock(camp.center.above(), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(camp.center.above()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(WFRegistry.WAR_MARK.get(), 4 + camp.raiders));
            chest.setItem(1, new ItemStack(WFRegistry.MANA_SHARD.get(), 6 + camp.raiders));
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(camp.owner);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("The " + camp.faction.displayName + " camp is taken. Their next raid "
                    + "will come a third smaller, and they left supplies behind.").withStyle(ChatFormatting.GOLD));
            if (!camp.rescue) completeLinked(owner, camp.id, Type.CAMP);
        }
    }

    // ------------------------------------------------------------------ completing

    private static void completeLinked(ServerPlayer player, UUID campId, Type type) {
        WarState war = WarState.get(player.server);
        for (Bounty b : new ArrayList<>(war.bounties(player.getUUID()))) {
            if (b.type() == type && campId.equals(b.camp())) complete(player, b);
        }
    }

    /** Pays a bounty out and removes it: mana, and XP for the units that took part. */
    public static void complete(Player player, Bounty b) {
        if (player.getServer() == null) return;
        WarState war = WarState.get(player.getServer());
        if (!war.bounties(player.getUUID()).remove(b)) return;
        war.setDirty();
        give(player, new ItemStack(WFRegistry.MANA_SHARD.get(), b.shards()));
        if (b.crystals() > 0) give(player, new ItemStack(WFRegistry.MANA_CRYSTAL.get(), b.crystals()));
        for (SoldierEntity s : player.level().getEntitiesOfClass(SoldierEntity.class, player.getBoundingBox().inflate(32),
                s -> s.isAlive() && s.isOwnedBy(player))) {
            s.addXp(BOUNTY_XP);
        }
        player.sendSystemMessage(Component.literal("Bounty done: " + b.describe() + ". (+" + b.shards() + " shards"
                + (b.crystals() > 0 ? ", +" + b.crystals() + " crystals" : "") + ")").withStyle(ChatFormatting.GOLD));
        if (player instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, new AdvisorLinePayload("Bounty done", b.describe()));
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /** Talking to the advisor with a delivery in hand. Returns true if it was handed over. */
    public static boolean deliver(Player player, ItemStack held) {
        if (player.getServer() == null || held.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        for (Bounty b : new ArrayList<>(WarState.get(player.getServer()).bounties(player.getUUID()))) {
            if (b.type() == Type.DELIVER && b.item().equals(id) && player.getInventory().countItem(held.getItem()) >= b.count()) {
                int left = b.count();
                for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
                    ItemStack s = player.getInventory().getItem(i);
                    if (!s.is(held.getItem())) continue;
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
                complete(player, b);
                return true;
            }
        }
        return false;
    }

    /** Drops expired bounties and their camps. */
    public static void expire(WarState war, UUID player, long now) {
        List<Bounty> list = war.bounties(player);
        if (list.removeIf(b -> b.expiresAt() <= now)) war.setDirty();
    }

    /** The bounties as lines for the advisor's counsel. */
    public static List<String> lines(Player player) {
        List<String> out = new ArrayList<>();
        if (player.getServer() == null) return out;
        long now = player.getServer().overworld().getGameTime();
        for (Bounty b : WarState.get(player.getServer()).bounties(player.getUUID())) {
            long hours = Math.max(0, (b.expiresAt() - now) / 1000);
            out.add(b.describe() + " (" + hours + "h left; " + b.shards() + " shards" + (b.crystals() > 0 ? ", " + b.crystals() + " crystals" : "") + ")");
        }
        return out;
    }

    // ------------------------------------------------------------------ the clock

    private static long lastDay = -1;

    /** Every 10 seconds: morning bounties, scouting, building and checking camps, expiry. */
    public static void tick(MinecraftServer server) {
        WarState war = WarState.get(server);
        long now = server.overworld().getGameTime();
        long day = server.overworld().getDayTime() / RaidScheduler.DAY;
        boolean morning = lastDay >= 0 && day != lastDay;
        lastDay = day;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Race.byId(p.getData(WFRegistry.RACE)) == null || p.getData(WFRegistry.QUEST_STEP)
                    < com.warfront.advisor.Advisor.Step.STANDARD.ordinal()) continue;
            if (morning && offer(p) > 0) {
                PacketDistributor.sendToPlayer(p, new AdvisorLinePayload("New bounties", "Your advisor has work for you."));
            }
            expire(war, p.getUUID(), now);
            for (Bounty b : new ArrayList<>(war.bounties(p.getUUID()))) {
                if (b.type() == Type.SCOUT && p.blockPosition().distSqr(new BlockPos(b.pos().getX(), p.getBlockY(), b.pos().getZ())) < 12 * 12) {
                    complete(p, b);
                }
            }
        }
        for (Camp camp : new ArrayList<>(war.camps())) {
            ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.parse(camp.dim)));
            if (level == null) continue;
            if (now >= camp.expiresAt) {
                disband(level, camp);
                war.removeCamp(camp);
                continue;
            }
            if (!level.isLoaded(camp.center)) continue;
            if (!camp.built) {
                boolean near = level.players().stream().anyMatch(pl -> pl.blockPosition().distSqr(camp.center) < 96 * 96);
                if (near) build(level, camp, server.getPlayerList().getPlayer(camp.owner));
                war.setDirty();
                continue;
            }
            if (guardsLeft(level, camp.id, Vec3.atCenterOf(camp.center)) == 0) {
                campTaken(level, camp);
                war.removeCamp(camp);
            }
        }
    }

    /** A camp nobody took breaks up: its raiders leave to join their raid. */
    private static void disband(ServerLevel level, Camp camp) {
        if (!camp.built || !level.isLoaded(camp.center)) return;
        for (SoldierEntity s : level.getEntitiesOfClass(SoldierEntity.class, new AABB(camp.center).inflate(48),
                s -> camp.id.equals(s.getWarbandId()) || isCaptive(s) && camp.id.equals(s.getPersistentData().getUUID(CAPTIVE)))) {
            s.discard();
        }
    }

    public static String typeId(Type t) {
        return t.name().toLowerCase(Locale.ROOT);
    }
}
