package com.warfront.test;

import com.warfront.army.Formation;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.block.ManaWellBlockEntity;
import com.warfront.block.SummoningAltarBlockEntity;
import com.warfront.block.TowerBlockEntity;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.combat.Rage;
import com.warfront.combat.Souls;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.mana.ManaNetwork;
import com.warfront.mana.ManaNodeBlockEntity;
import com.warfront.network.TestModePayload;
import com.warfront.registry.WFRegistry;
import com.warfront.world.BaseLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Test mode. Every Test Panel button and every {@code /wftest} subcommand lands in {@link #run}, so the two always do
 * the same thing. Only players with test mode on and operator rights (cheats on in singleplayer) get through.
 */
public final class TestActions {
    /** How far the base tools reach from the player. */
    public static final double BASE_RADIUS = 32;
    /** How far the army and siege tools reach. */
    private static final double REACH = 128;

    private TestActions() {}

    /** The outcome of an action: whether it worked and a line for the player. */
    public record Result(boolean ok, String message) {
        static Result ok(String m) {
            return new Result(true, m);
        }

        static Result fail(String m) {
            return new Result(false, m);
        }
    }

    public static boolean isOn(Player player) {
        return player.getData(WFRegistry.TEST_MODE);
    }

    /** Whether this player may use the test tools right now. */
    public static boolean allowed(Player player) {
        return isOn(player) && player.hasPermissions(2);
    }

    public static Result setTestMode(ServerPlayer player, boolean on) {
        player.setData(WFRegistry.TEST_MODE, on);
        if (!on && player.getData(WFRegistry.GOD_MODE)) setGod(player, false);
        sync(player);
        return Result.ok(on ? "Test mode ON. Press F8 for the Test Panel, or use /wftest." : "Test mode OFF.");
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new TestModePayload(isOn(player)));
    }

    /** Runs one action from its words, e.g. {@code spawn orc swordsman 5 friendly}. */
    public static Result run(ServerPlayer player, List<String> args) {
        if (args.isEmpty()) return Result.fail("No action.");
        String a = args.get(0).toLowerCase(Locale.ROOT);
        String b = arg(args, 1);
        try {
            return switch (a) {
                case "spawn" -> spawn(player, Race.byId(b), role(arg(args, 2)), parseInt(arg(args, 3), 1), arg(args, 4),
                        parseInt(arg(args, 5), 0));
                case "beast" -> spawn(player, Race.byId(b), SoldierRole.BEAST, parseInt(arg(args, 2), 1), arg(args, 3));
                case "army" -> army(player, b);
                case "base" -> switch (b) {
                    case "level" -> setBaseLevel(player, parseInt(arg(args, 2), 0));
                    case "fill" -> fillMana(player);
                    case "infinite" -> setInfinite(player, !"off".equals(arg(args, 2)));
                    case "starter" -> starterBase(player);
                    case "bounties" -> newBounties(player);
                    case "merchant" -> {
                        com.warfront.merchant.Caravan.summonNow(player);
                        yield Result.ok("The merchant's caravan is here.");
                    }
                    case "camp" -> raiderCamp(player, "rescue".equals(arg(args, 2)));
                    case "food" -> messHall(player, !"empty".equals(arg(args, 2)));
                    case "traps" -> {
                        com.warfront.tunnel.Tunnels.kit(player);
                        yield Result.ok("Dwarf tunnel kit given: Rune Drill, rune stone, spike floors, rune mines, flame vents.");
                    }
                    default -> Result.fail("base level|fill|infinite|starter|bounties|merchant|camp|food|traps");
                };
                case "siege" -> switch (b) {
                    case "start" -> startWave(player, faction(arg(args, 2)));
                    case "wave" -> jumpToWave(player, parseInt(arg(args, 2), 1));
                    case "pause" -> pause(player, true);
                    case "resume" -> pause(player, false);
                    case "end" -> endSiege(player);
                    case "outpost" -> outpost(player, !"destroy".equals(arg(args, 2)));
                    default -> Result.fail("siege start|wave|pause|resume|end|outpost [destroy]");
                };
                case "player" -> switch (b) {
                    case "race" -> setRace(player, Race.byId(arg(args, 2)));
                    case "fill" -> fillPlayer(player);
                    case "day" -> time(player, 1000);
                    case "night" -> time(player, 13000);
                    case "clear" -> clearWeather(player);
                    case "god" -> setGod(player, !player.getData(WFRegistry.GOD_MODE));
                    case "kit" -> kit(player);
                    case "flight" -> flightGear(player);
                    case "resolve" -> switch (arg(args, 2)) {
                        case "empty" -> {
                            com.warfront.combat.Resolve.set(player, 0);
                            yield Result.ok("Resolve empty.");
                        }
                        case "oath" -> {
                            com.warfront.combat.Resolve.set(player, com.warfront.combat.Resolve.MAX);
                            yield com.warfront.combat.Resolve.swear(player) ? Result.ok("Oath of Stone sworn.") : Result.fail("Already under the Oath.");
                        }
                        default -> {
                            com.warfront.combat.Resolve.set(player, com.warfront.combat.Resolve.MAX);
                            com.warfront.combat.Resolve.markCombat(player);
                            yield Result.ok("Resolve full: crouch to swear the Oath (dwarves).");
                        }
                    };
                    case "valor" -> switch (arg(args, 2)) {
                        case "empty" -> {
                            com.warfront.combat.Valor.set(player, 0);
                            yield Result.ok("Valor empty.");
                        }
                        case "rally" -> {
                            com.warfront.combat.Valor.set(player, com.warfront.combat.Valor.MAX);
                            int n = com.warfront.combat.Valor.rally(player);
                            yield Result.ok("Rallied " + n + " (you included).");
                        }
                        default -> {
                            com.warfront.combat.Valor.set(player, com.warfront.combat.Valor.MAX);
                            yield Result.ok("Valor full: press R to Rally (humans).");
                        }
                    };
                    case "radiance" -> switch (arg(args, 2)) {
                        case "empty" -> {
                            com.warfront.combat.Radiance.set(player, 0);
                            yield Result.ok("Radiance empty.");
                        }
                        case "judgment" -> {
                            com.warfront.combat.Radiance.set(player, com.warfront.combat.Radiance.MAX);
                            com.warfront.combat.Radiance.judgment(player);
                            yield Result.ok("Judgment called at your crosshair (angels).");
                        }
                        default -> {
                            com.warfront.combat.Radiance.set(player, com.warfront.combat.Radiance.MAX);
                            yield Result.ok("Radiance full: press R to call Judgment (angels).");
                        }
                    };
                    case "shrine" -> {
                        com.warfront.world.SkyShrine.build(player.serverLevel(), player.blockPosition().relative(player.getDirection(), 5));
                        yield Result.ok("Sky shrine built ahead of you.");
                    }
                    case "charter" -> {
                        if ("claim".equals(arg(args, 2))) {
                            if (com.warfront.item.VillageCharterItem.sendToVillage(player)) {
                                com.warfront.item.VillageCharterItem.claimAt(player, player.blockPosition());
                                yield Result.ok("Moved to the nearest village and claimed it.");
                            }
                            yield Result.fail("No village found within 100 chunks (a charter was given).");
                        }
                        player.getInventory().add(new ItemStack(WFRegistry.VILLAGE_CHARTER.get()));
                        yield Result.ok("Village Charter given.");
                    }
                    case "hold" -> {
                        com.warfront.world.HoldGate.testBuild(player);
                        yield Result.ok("Hold gate built ahead of you.");
                    }
                    default -> Result.fail("player race|fill|day|night|clear|god|kit|flight|resolve [fill|empty|oath]|hold|valor [fill|empty|rally]|charter [claim]|radiance [fill|empty|judgment]|shrine");
                };
                case "difficulty" -> setPreset(player, com.warfront.war.WarState.Preset.byId(b));
                case "raid" -> switch (b) {
                    case "warn" -> forceWarning(player, com.warfront.war.WarState.Pending.RAID);
                    case "siege" -> forceWarning(player, com.warfront.war.WarState.Pending.SIEGE);
                    case "grace" -> skipGrace(player);
                    case "recall" -> testRecall(player);
                    default -> Result.fail("raid warn|siege|grace|recall");
                };
                case "war" -> switch (b) {
                    case "meter" -> {
                        NpcFaction f = faction(arg(args, 2));
                        if (f == null) yield Result.fail("Unknown faction.");
                        com.warfront.war.Campaign.setMeter(player, f, parseInt(arg(args, 3), 0));
                        yield Result.ok(f.displayName + " war meter set to " + com.warfront.war.Campaign.meter(player, f) + ".");
                    }
                    case "map" -> {
                        NpcFaction f = faction(arg(args, 2));
                        if (f == null) yield Result.fail("Unknown faction.");
                        ItemStack map = com.warfront.war.Campaign.warMap(f);
                        if (!player.getInventory().add(map)) player.drop(map, false);
                        yield Result.ok("War Map: " + f.displayName + ".");
                    }
                    case "beaten" -> {
                        String which = arg(args, 2);
                        boolean on = !"no".equals(arg(args, 3));
                        for (NpcFaction f : NpcFaction.values()) {
                            if (which.equals("all") || which.equals(f.name().toLowerCase(Locale.ROOT))) com.warfront.war.Campaign.setWarlordBeaten(player, f, on);
                            if (which.equals("none")) com.warfront.war.Campaign.setWarlordBeaten(player, f, false);
                        }
                        yield Result.ok(com.warfront.war.Campaign.warlordsBeaten(player) + " warlords marked beaten.");
                    }
                    case "status" -> {
                        com.warfront.war.Campaign.status(player).forEach(player::sendSystemMessage);
                        yield Result.ok("War meters listed in chat.");
                    }
                    case "tp" -> fortressTp(player, faction(arg(args, 2)));
                    case "place" -> fortressPlace(player, faction(arg(args, 2)));
                    case "reset" -> fortressReset(player);
                    default -> Result.fail("war meter <faction> <n>|map <faction>|beaten <faction|all|none> [no]|status|tp|place|reset");
                };
                case "advisor" -> switch (b) {
                    case "step" -> advisorStep(player, parseInt(arg(args, 2), 0));
                    case "respawn" -> advisorRespawn(player);
                    case "disguise" -> advisorDisguise(player, Race.byId(arg(args, 2)));
                    default -> Result.fail("advisor step <n>|respawn|disguise <race>");
                };
                default -> Result.fail("Unknown test action: " + a);
            };
        } catch (NumberFormatException e) {
            return Result.fail("Not a number: " + e.getMessage());
        }
    }

    private static String arg(List<String> args, int i) {
        return i < args.size() ? args.get(i).toLowerCase(Locale.ROOT) : "";
    }

    private static int parseInt(String s, int fallback) {
        return s.isEmpty() ? fallback : Integer.parseInt(s);
    }

    @Nullable
    private static SoldierRole role(String id) {
        for (SoldierRole r : SoldierRole.values()) if (r.id().equals(id)) return r;
        return null;
    }

    /** A faction by its short name (marauders, the_swarm...), or null for "any" / unknown. */
    @Nullable
    public static NpcFaction faction(String id) {
        for (NpcFaction f : NpcFaction.values()) if (f.name().toLowerCase(Locale.ROOT).equals(id)) return f;
        return null;
    }

    // ------------------------------------------------------------------ spawn

    /**
     * Spawns {@code count} units in front of the player. {@code side} "friendly" (or empty) makes them yours; a
     * faction name makes them that faction's raiders. War beasts ignore the beast limit here.
     */
    public static Result spawn(Player player, @Nullable Race race, @Nullable SoldierRole role, int count, String side) {
        return spawn(player, race, role, count, side, 0);
    }

    /** As above, at a veterancy rank (0 Recruit to 4 Legend). */
    public static Result spawn(Player player, @Nullable Race race, @Nullable SoldierRole role, int count, String side, int rank) {
        if (role == null || role.retired()) return Result.fail("Unknown role.");
        count = Mth.clamp(count, 1, 10);
        ServerLevel level = (ServerLevel) player.level();
        boolean friendly = side.isEmpty() || side.equals("friendly");
        NpcFaction enemy = friendly ? null : faction(side);
        if (!friendly && enemy == null) return Result.fail("Unknown faction: " + side);
        Race r = race != null ? race : enemy != null ? enemy.race : SummoningAltarBlockEntity.raceOf(player);
        Vec3 front = ahead(player, 4);
        UUID warband = UUID.randomUUID();
        int made = 0;
        for (int i = 0; i < count; i++) {
            SoldierEntity s = WFRegistry.SOLDIER.get().create(level);
            if (s == null) continue;
            double x = front.x + (i % 5 - 2) * 1.2, z = front.z + (i / 5) * 1.2;
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
            s.moveTo(x, ground.getY(), z, player.getYRot() + 180F, 0F);
            if (friendly) {
                s.setupAsRecruit(player, role, r);
                if (role.posted()) {
                    level.addFreshEntity(s);
                    s.assignPost(s.position(), s.getYRot());
                } else {
                    s.command(Order.FOLLOW, Formation.byOrdinal(player.getData(WFRegistry.ARMY_FORMATION)), null, player.getYRot());
                    level.addFreshEntity(s);
                }
            } else {
                s.setupAsRaider(enemy, role, warband, null, null, 1);
                level.addFreshEntity(s);
            }
            if (rank > 0) s.setXp(com.warfront.army.Veterancy.threshold(Math.min(rank, com.warfront.army.Veterancy.MAX_RANK)));
            made++;
        }
        return Result.ok("Spawned " + made + " " + (friendly ? r.displayName() + " " : enemy.displayName + " ")
                + role.displayName().toLowerCase(Locale.ROOT) + (made == 1 ? "" : "s") + ".");
    }

    private static Vec3 ahead(Player player, double dist) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        return player.position().add(-Mth.sin(yaw) * dist, 0, Mth.cos(yaw) * dist);
    }

    /** Every soldier the player owns nearby, workers and posted units included. */
    public static List<SoldierEntity> owned(Player player) {
        return player.level().getEntitiesOfClass(SoldierEntity.class, player.getBoundingBox().inflate(REACH),
                s -> s.isAlive() && s.isOwnedBy(player));
    }

    public static Result army(Player player, String what) {
        List<SoldierEntity> army = owned(player);
        switch (what) {
            case "heal" -> army.forEach(s -> s.setHealth(s.getMaxHealth()));
            case "kill" -> army.forEach(s -> s.hurt(player.damageSources().genericKill(), Float.MAX_VALUE));
            case "dismiss" -> army.forEach(SoldierEntity::discard);
            case "revive" -> {
                army.forEach(SoldierEntity::revive);
                if (player.getServer() != null) {
                    var war = com.warfront.war.WarState.get(player.getServer());
                    var list = new ArrayList<>(war.returning(player.getUUID()));
                    for (int i = list.size() - 1; i >= 0; i--) war.takeReturning(player.getUUID(), i);
                    for (var r : list) war.addReturning(player.getUUID(), new com.warfront.war.WarState.Returning(r.role(), r.race(), r.xp(), 0));
                }
                return Result.ok("Fallen heroes revived; returning heroes are ready at the altar.");
            }
            default -> {
                return Result.fail("army heal|kill|dismiss|revive");
            }
        }
        return Result.ok(switch (what) {
            case "heal" -> "Healed ";
            case "kill" -> "Killed ";
            default -> "Dismissed ";
        } + army.size() + " of your units.");
    }

    // ------------------------------------------------------------------ base

    /** Sets (or with 0 clears) a TEST base level on every mana network within reach. */
    public static Result setBaseLevel(Player player, int level) {
        return setBaseLevel(player, level, BASE_RADIUS);
    }

    public static Result setBaseLevel(Player player, int level, double radius) {
        level = Mth.clamp(level, 0, BaseLevel.MAX_LEVEL);
        List<ManaNodeBlockEntity> nodes = ManaNetwork.near(player.level(), player.blockPosition(), radius);
        if (nodes.isEmpty()) return Result.fail("No Mana Well or Pylon within " + (int) BASE_RADIUS + " blocks.");
        for (ManaNodeBlockEntity n : nodes) n.setTestLevel(level);
        return Result.ok(level == 0 ? "Base level back to normal." : "Base level set to " + level + " (TEST).");
    }

    public static Result fillMana(Player player) {
        return fillMana(player, BASE_RADIUS);
    }

    public static Result fillMana(Player player, double radius) {
        int wells = 0;
        for (ManaNodeBlockEntity n : ManaNetwork.near(player.level(), player.blockPosition(), radius)) {
            if (n instanceof ManaWellBlockEntity w) {
                w.setMana(ManaWellBlockEntity.capacity());
                wells++;
            }
        }
        return wells == 0 ? Result.fail("No Mana Well within " + (int) BASE_RADIUS + " blocks.")
                : Result.ok("Filled " + wells + " well" + (wells == 1 ? "" : "s") + ".");
    }

    public static Result setInfinite(Player player, boolean on) {
        return setInfinite(player, on, BASE_RADIUS);
    }

    public static Result setInfinite(Player player, boolean on, double radius) {
        int wells = 0;
        for (ManaNodeBlockEntity n : ManaNetwork.near(player.level(), player.blockPosition(), radius)) {
            if (n instanceof ManaWellBlockEntity w) {
                w.setInfinite(on);
                wells++;
            }
        }
        return wells == 0 ? Result.fail("No Mana Well within " + (int) BASE_RADIUS + " blocks.")
                : Result.ok("Infinite mana " + (on ? "ON" : "OFF") + " for " + wells + " well" + (wells == 1 ? "" : "s") + ".");
    }

    public static Result newBounties(ServerPlayer player) {
        var war = com.warfront.war.WarState.get(player.server);
        war.bounties(player.getUUID()).clear();
        int n = com.warfront.war.Bounties.offer(player);
        com.warfront.war.Bounties.lines(player).forEach(l -> player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("Bounty: " + l).withStyle(net.minecraft.ChatFormatting.GOLD)));
        return Result.ok(n + " new bounties.");
    }

    /** Builds a raider camp 30 blocks ahead, with a bounty for it. */
    public static Result raiderCamp(ServerPlayer player, boolean rescue) {
        var war = com.warfront.war.WarState.get(player.server);
        Vec3 at = ahead(player, 30);
        var camp = com.warfront.war.Bounties.planCamp(player, player.blockPosition(), rescue, BlockPos.containing(at), player.getRandom());
        camp.dim = player.level().dimension().location().toString();
        com.warfront.war.Bounties.build(player.serverLevel(), camp, player);
        war.addCamp(camp);
        war.bounties(player.getUUID()).add(new com.warfront.war.Bounties.Bounty(UUID.randomUUID(),
                rescue ? com.warfront.war.Bounties.Type.RESCUE : com.warfront.war.Bounties.Type.CAMP, camp.center, "", 0, camp.id,
                player.server.overworld().getGameTime() + 2L * com.warfront.war.RaidScheduler.DAY, 12, 1));
        war.setDirty();
        return Result.ok("A " + camp.faction.displayName + " camp of " + camp.raiders + " raiders is up ahead.");
    }

    public static Result messHall(Player player, boolean fill) {
        int n = com.warfront.upkeep.Upkeep.fillOrEmpty(player, fill, BASE_RADIUS);
        return n == 0 ? Result.fail("No Mess Hall within " + (int) BASE_RADIUS + " blocks.")
                : Result.ok((fill ? "Filled " : "Emptied ") + n + " Mess Hall" + (n == 1 ? "" : "s") + ".");
    }

    /**
     * Clears a flat 13×13 spot in front of the player and builds a working base on it: a full Mana Well, two Pylons,
     * a complete Summoning Altar, a War Standard and an Arrow Tower, all owned by the player.
     */
    public static Result starterBase(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 c = ahead(player, 9);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(c.x, 0, c.z));
        return Result.ok(buildStarterBase(level, player, ground));
    }

    /** Builds the starter base with its floor at {@code center.below()}. Shared with the game test. */
    public static String buildStarterBase(ServerLevel level, Player owner, BlockPos center) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                level.setBlock(center.offset(dx, -1, dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                for (int dy = 0; dy <= 4; dy++) level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        UUID id = owner.getUUID();
        place(level, center, WFRegistry.SUMMONING_ALTAR.get(), id);
        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dz = -2; dz <= 2; dz += 4) level.setBlock(center.offset(dx, 0, dz), WFRegistry.MANA_BRAZIER.get().defaultBlockState(), 3);
        }
        BlockEntity well = place(level, center.offset(5, 0, 0), WFRegistry.MANA_WELL.get(), id);
        if (well instanceof ManaWellBlockEntity w) w.setMana(ManaWellBlockEntity.capacity());
        place(level, center.offset(-5, 0, 0), WFRegistry.MANA_PYLON.get(), id);
        place(level, center.offset(0, 0, 5), WFRegistry.MANA_PYLON.get(), id);
        place(level, center.offset(0, 0, -5), WFRegistry.WAR_STANDARD.get(), id);
        place(level, center.offset(5, 0, -5), WFRegistry.ARROW_TOWER.get(), id);
        return "Starter base built: a full Mana Well, two Pylons, a Summoning Altar, a War Standard and an Arrow Tower.";
    }

    private static BlockEntity place(ServerLevel level, BlockPos pos, Block block, UUID owner) {
        level.setBlock(pos, block.defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ManaNodeBlockEntity n) n.setOwner(owner);
        else if (be instanceof SummoningAltarBlockEntity a) a.setOwner(owner);
        else if (be instanceof WarStandardBlockEntity s) s.setOwner(owner);
        else if (be instanceof TowerBlockEntity t) t.setOwner(owner);
        return be;
    }

    // ------------------------------------------------------------------ siege

    /** The nearest War Standard within reach of the player, or null. */
    @Nullable
    public static WarStandardBlockEntity nearestStandard(Player player) {
        ServerLevel level = (ServerLevel) player.level();
        ChunkPos c = player.chunkPosition();
        int r = (int) REACH >> 4;
        WarStandardBlockEntity best = null;
        double bestD = REACH * REACH;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!level.hasChunk(c.x + dx, c.z + dz)) continue;
                for (BlockEntity be : level.getChunk(c.x + dx, c.z + dz).getBlockEntities().values()) {
                    if (!(be instanceof WarStandardBlockEntity s)) continue;
                    double d = be.getBlockPos().distToCenterSqr(player.position());
                    if (d < bestD) {
                        bestD = d;
                        best = s;
                    }
                }
            }
        }
        return best;
    }

    private static final String NO_STANDARD = "No War Standard within " + (int) REACH + " blocks.";

    public static Result startWave(Player player, @Nullable NpcFaction faction) {
        WarStandardBlockEntity s = nearestStandard(player);
        if (s == null) return Result.fail(NO_STANDARD);
        if (s.isUnderSiege()) return Result.fail("A wave is already underway.");
        s.startSiege((ServerLevel) player.level(), faction);
        return Result.ok("Wave " + s.getWave() + " started.");
    }

    public static Result jumpToWave(Player player, int wave) {
        WarStandardBlockEntity s = nearestStandard(player);
        if (s == null) return Result.fail(NO_STANDARD);
        if (s.isUnderSiege()) s.endSiege((ServerLevel) player.level());
        s.setNextWave(Math.max(1, wave));
        s.startSiege((ServerLevel) player.level(), null);
        return Result.ok("Jumped to wave " + s.getWave() + ".");
    }

    public static Result pause(Player player, boolean pause) {
        WarStandardBlockEntity s = nearestStandard(player);
        if (s == null) return Result.fail(NO_STANDARD);
        s.setPaused((ServerLevel) player.level(), pause);
        return Result.ok(pause ? "Siege clock paused." : "Siege clock running.");
    }

    /** Raises an outpost at the nearest War Standard now, or takes its outpost down. */
    public static Result outpost(Player player, boolean raise) {
        WarStandardBlockEntity s = nearestStandard(player);
        if (s == null) return Result.fail(NO_STANDARD);
        ServerLevel level = (ServerLevel) player.level();
        if (!raise) {
            BlockPos chest = s.getOutpostChest();
            if (chest == null) return Result.fail("No outpost stands at that War Standard.");
            com.warfront.outpost.Outposts.scheduleRemoval(level, chest, 1);
            return Result.ok("Outpost taken down.");
        }
        UUID warband = s.getWarbandId() != null ? s.getWarbandId() : UUID.randomUUID();
        NpcFaction f = NpcFaction.values()[level.random.nextInt(NpcFaction.values().length)];
        BlockPos chest = com.warfront.outpost.Outposts.raise(level, s, f, Math.max(5, s.getWave()), warband);
        if (chest == null) return Result.fail("No solid ground for an outpost 30-50 blocks from the standard.");
        s.setOutpostChest(chest);
        return Result.ok("A " + f.displayName + " outpost rises at " + chest.toShortString() + ".");
    }

    public static Result endSiege(Player player) {
        WarStandardBlockEntity s = nearestStandard(player);
        if (s == null) return Result.fail(NO_STANDARD);
        s.endSiege((ServerLevel) player.level());
        return Result.ok("Siege ended.");
    }

    // ------------------------------------------------------------------ player

    public static Result setRace(ServerPlayer player, @Nullable Race race) {
        if (race == null) return Result.fail("Unknown race.");
        player.setData(WFRegistry.RACE, race.id());
        race.apply(player);
        com.warfront.network.RaceSync.broadcast(player);
        player.setHealth(player.getMaxHealth());
        com.warfront.advisor.Advisor.onRaceChosen(player);
        return Result.ok("You are now " + race.displayName() + ".");
    }

    /** Souls, rage and every Mana Glider's durability, full. */
    public static Result fillPlayer(ServerPlayer player) {
        Souls.fill(player);
        Rage.fill(player);
        for (ItemStack stack : player.getInventory().items) if (stack.is(WFRegistry.MANA_GLIDER.get())) stack.setDamageValue(0);
        for (ItemStack stack : player.getInventory().armor) if (stack.is(WFRegistry.MANA_GLIDER.get())) stack.setDamageValue(0);
        for (List<ItemStack> list : List.of(player.getInventory().items, player.getInventory().armor)) {
            for (ItemStack stack : list) {
                if (stack.is(WFRegistry.ANGEL_WINGS.get())) stack.setDamageValue(0);
                if (stack.is(WFRegistry.ROCKET_PACK.get())) com.warfront.flight.RocketPackItem.setFuel(stack,
                        com.warfront.flight.RocketPackItem.MAX_FUEL_SECONDS * 20);
            }
        }
        return Result.ok("Souls, rage, glider, wings and rocket fuel filled.");
    }

    /** Your race's flight gear, equipped and ready: glider, rocket pack (fuelled), wings, or the Wind Charm with shards. */
    public static Result flightGear(ServerPlayer player) {
        Race race = Race.byId(player.getData(WFRegistry.RACE));
        if (race == null) return Result.fail("Pick a race first.");
        ItemStack chest = switch (race) {
            case HUMAN -> new ItemStack(WFRegistry.MANA_GLIDER.get());
            case ORC -> {
                ItemStack pack = new ItemStack(WFRegistry.ROCKET_PACK.get());
                com.warfront.flight.RocketPackItem.setFuel(pack, com.warfront.flight.RocketPackItem.MAX_FUEL_SECONDS * 20);
                yield pack;
            }
            case ANGEL -> new ItemStack(WFRegistry.ANGEL_WINGS.get());
            default -> ItemStack.EMPTY;
        };
        if (!chest.isEmpty()) {
            ItemStack old = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            if (!old.isEmpty() && !player.getInventory().add(old.copy())) player.drop(old.copy(), false);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
            return Result.ok("Equipped: " + chest.getHoverName().getString() + ".");
        }
        if (race == Race.ELF) {
            player.getInventory().add(new ItemStack(WFRegistry.WIND_CHARM.get()));
            player.getInventory().add(new ItemStack(WFRegistry.MANA_SHARD.get(), 32));
            return Result.ok("Wind Charm and 32 Mana Shards given.");
        }
        return Result.ok(race.displayName() + " flight needs no gear" + (race == Race.DEMON ? ": press jump while falling." : "."));
    }

    private static Result time(ServerPlayer player, long time) {
        ((ServerLevel) player.level()).setDayTime(time);
        return Result.ok(time < 12000 ? "Day." : "Night.");
    }

    private static Result clearWeather(ServerPlayer player) {
        ((ServerLevel) player.level()).setWeatherParameters(12000, 0, false, false);
        return Result.ok("Clear weather.");
    }

    public static Result setGod(ServerPlayer player, boolean on) {
        player.setData(WFRegistry.GOD_MODE, on);
        var abilities = player.getAbilities();
        boolean creative = player.isCreative() || player.isSpectator();
        abilities.invulnerable = on || creative;
        abilities.mayfly = on || creative;
        if (!abilities.mayfly) abilities.flying = false;
        player.onUpdateAbilities();
        return Result.ok("God mode " + (on ? "ON" : "OFF") + ".");
    }

    /** One of every Warfront item; the Mana Glider only for humans. */
    public static Result kit(ServerPlayer player) {
        Race race = Race.byId(player.getData(WFRegistry.RACE));
        List<Item> items = new ArrayList<>();
        WFRegistry.ITEMS.getEntries().forEach(h -> items.add(h.get()));
        for (Item item : items) {
            if (item == WFRegistry.MANA_GLIDER.get() && race != Race.HUMAN) continue;
            if (item == WFRegistry.ROCKET_PACK.get() && race != Race.ORC) continue;
            if (item == WFRegistry.ANGEL_WINGS.get() && race != Race.ANGEL) continue;
            if (item == WFRegistry.WIND_CHARM.get() && race != Race.ELF) continue;
            ItemStack stack = new ItemStack(item, Math.min(16, item.getDefaultMaxStackSize()));
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        return Result.ok("Kit given.");
    }

    // ------------------------------------------------------------------ pacing

    public static Result setPreset(ServerPlayer player, @Nullable com.warfront.war.WarState.Preset preset) {
        if (preset == null) return Result.fail("easy|normal|hard|warlord");
        com.warfront.war.WarState.get(player.server).setPreset(preset);
        return Result.ok("Difficulty: " + preset.title() + ".");
    }

    public static Result skipGrace(ServerPlayer player) {
        com.warfront.war.WarState.get(player.server).skipGrace();
        return Result.ok("Grace period over: raids can come now.");
    }

    /** Announces an attack now (it hits after the usual warning). A siege needs a planted War Standard. */
    public static Result forceWarning(ServerPlayer player, com.warfront.war.WarState.Pending type) {
        var clock = com.warfront.war.WarState.get(player.server).clock(player.getUUID());
        if (type == com.warfront.war.WarState.Pending.SIEGE && clock.home == null) {
            WarStandardBlockEntity s = nearestStandard(player);
            if (s == null) return Result.fail("Plant a War Standard first: sieges hit your base.");
            clock.home = s.getBlockPos();
            clock.homeDim = player.level().dimension();
        }
        com.warfront.war.RaidScheduler.warn(player, clock, type, null, player.server.overworld().getGameTime());
        return Result.ok("Warning sent.");
    }

    /** Recalls home at once (no channel), even without an attack; still marks this attack's recall used. */
    public static Result testRecall(ServerPlayer player) {
        var clock = com.warfront.war.WarState.get(player.server).clock(player.getUUID());
        var level = com.warfront.war.RaidScheduler.homeLevel(player, clock);
        if (level == null || clock.home == null) return Result.fail("You have no War Standard to recall to.");
        int n = com.warfront.war.Recall.perform(player, level, clock.home, clock);
        return Result.ok("Recalled home with " + n + " units.");
    }

    // ------------------------------------------------------------------ fortresses

    /** Teleports to the nearest fortress of this faction in the current dimension. */
    public static Result fortressTp(ServerPlayer player, @Nullable NpcFaction f) {
        if (f == null) return Result.fail("Unknown faction.");
        BlockPos at = player.serverLevel().findNearestMapStructure(com.warfront.war.WarMapItem.fortressTag(f), player.blockPosition(), 100, false);
        if (at == null) return Result.fail("No " + f.displayName + " fortress in this dimension.");
        BlockPos ground = player.serverLevel().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.offset(0, 0, 24));
        player.teleportTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
        return Result.ok("At the gates of the " + f.displayName + " fortress.");
    }

    /** Places a fortress 20 blocks ahead (test worlds: it flattens what's there). */
    public static Result fortressPlace(ServerPlayer player, @Nullable NpcFaction f) {
        if (f == null) return Result.fail("Unknown faction.");
        ServerLevel level = player.serverLevel();
        var template = level.getStructureManager().get(com.warfront.Warfront.id("fortress_" + f.name().toLowerCase(Locale.ROOT)));
        if (template.isEmpty()) return Result.fail("The fortress template is missing.");
        Vec3 c = ahead(player, 36);
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(c.x, 0, c.z));
        BlockPos corner = ground.offset(-16, 0, -16);
        template.get().placeInWorld(level, corner, corner, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                level.random, 2);
        return Result.ok("A " + f.displayName + " fortress stands ahead. Walk within 40 blocks of its keep to wake it.");
    }

    /** Puts the nearest fortress back to sleep (garrison gone, warlord gone, not beaten). */
    public static Result fortressReset(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (BlockPos p : BlockPos.betweenClosed(player.blockPosition().offset(-48, -16, -48), player.blockPosition().offset(48, 16, 48))) {
            if (level.getBlockEntity(p) instanceof com.warfront.fortress.FortressCoreBlockEntity core) {
                core.reset(level);
                return Result.ok("Fortress reset.");
            }
        }
        return Result.fail("No fortress within 48 blocks.");
    }

    // ------------------------------------------------------------------ advisor

    public static Result advisorStep(Player player, int step) {
        com.warfront.advisor.Advisor.Step s = com.warfront.advisor.Advisor.Step.byOrdinal(step);
        com.warfront.advisor.Advisor.setStep(player, s);
        return Result.ok("Quest step: " + s.goal + ".");
    }

    public static Result advisorRespawn(ServerPlayer player) {
        com.warfront.advisor.Advisor.spawn(player, player.blockPosition());
        return Result.ok("Advisor respawned beside you.");
    }

    public static Result advisorDisguise(ServerPlayer player, @Nullable Race race) {
        if (race == null) return Result.fail("Unknown race.");
        var a = com.warfront.advisor.Advisor.find(player);
        if (a == null) return Result.fail("Your advisor isn't loaded nearby. Respawn him first.");
        a.setDisguise(race);
        return Result.ok("Advisor now appears as the " + race.displayName() + " " + com.warfront.advisor.Advisor.title(race) + ".");
    }

    /** Server-side check for the god-mode flag after a respawn or a game-mode change. */
    public static void reapplyGod(ServerPlayer player) {
        if (player.getData(WFRegistry.GOD_MODE)) setGod(player, true);
    }
}
