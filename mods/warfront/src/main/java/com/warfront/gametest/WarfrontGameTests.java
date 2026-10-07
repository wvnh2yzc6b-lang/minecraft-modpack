package com.warfront.gametest;

import com.warfront.Warfront;
import com.warfront.army.Formation;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.block.ManaWellBlockEntity;
import com.warfront.block.SummoningAltarBlockEntity;
import com.warfront.block.TowerBlockEntity;
import com.warfront.registry.WFRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-world tests run by {@code ./gradlew runGameTestServer} (and in CI). Each test builds a small
 * scenario on a 9x9 stone arena and checks the soldiers behave.
 */
@GameTestHolder(Warfront.MODID)
@PrefixGameTestTemplate(false)
public final class WarfrontGameTests {
    private static final String ARENA = "platform";

    private WarfrontGameTests() {}

    private static SoldierEntity recruit(GameTestHelper h, Player owner, SoldierRole role, Race race, int x, int z) {
        SoldierEntity s = spawnRaw(h, x, z);
        s.setupAsRecruit(owner, role, race);
        s.command(Order.HOLD, Formation.LINE, s.position(), 0F);
        h.getLevel().addFreshEntity(s);
        return s;
    }

    private static SoldierEntity raider(GameTestHelper h, SoldierRole role, int x, int z) {
        SoldierEntity s = spawnRaw(h, x, z);
        s.setupAsRaider(NpcFaction.MARAUDERS, role, UUID.randomUUID(), null, null, 1);
        h.getLevel().addFreshEntity(s);
        return s;
    }

    private static SoldierEntity spawnRaw(GameTestHelper h, int x, int z) {
        SoldierEntity s = WFRegistry.SOLDIER.get().create(h.getLevel());
        if (s == null) throw new IllegalStateException("soldier entity type failed to create");
        // Relative y=1 is the arena floor (the structure sits one block above the test origin).
        BlockPos abs = h.absolutePos(new BlockPos(x, 2, z));
        s.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0F, 0F);
        return s;
    }

    @GameTest(template = ARENA)
    public static void recruitGetsRoleLoadoutAndRaceStats(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity s = recruit(h, owner, SoldierRole.SHIELDBEARER, Race.DWARF, 4, 4);
        h.assertTrue(s.isOwnedBy(owner), "recruit should belong to its commander");
        h.assertTrue(s.getOffhandItem().is(Items.SHIELD), "shieldbearer should carry a shield");
        h.assertTrue(s.getMaxHealth() >= 34, "dwarf shieldbearer should have >= 34 max health, had " + s.getMaxHealth());
        h.assertTrue(s.getScale() < 1.0F, "dwarves should be short");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void formationGivesDistinctSlotsWithShieldsInFront(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierRole[] roles = {SoldierRole.ARCHER, SoldierRole.SHIELDBEARER, SoldierRole.HEALER,
                SoldierRole.SHIELDBEARER, SoldierRole.SPEARMAN, SoldierRole.ARCHER, SoldierRole.SHIELDBEARER,
                SoldierRole.SWORDSMAN, SoldierRole.SHIELDBEARER, SoldierRole.SPEARMAN, SoldierRole.SHIELDBEARER,
                SoldierRole.SHIELDBEARER};
        List<SoldierEntity> army = new ArrayList<>();
        for (int i = 0; i < roles.length; i++) {
            army.add(recruit(h, owner, roles[i], Race.HUMAN, 1 + i % 7, 1 + i / 7));
        }
        Vec3 anchor = h.absoluteVec(new Vec3(4.5, 2, 6.5));
        for (SoldierEntity s : army) s.command(Order.HOLD, Formation.SHIELD_WALL, anchor, 0F);
        for (SoldierEntity s : army) s.recomputeSlot();

        for (int i = 0; i < army.size(); i++) {
            Vec3 a = army.get(i).getSlot();
            h.assertTrue(a != null, "every soldier should have a slot");
            for (int j = i + 1; j < army.size(); j++) {
                h.assertTrue(a.distanceTo(army.get(j).getSlot()) > 0.5, "two soldiers share a slot");
            }
        }
        // Yaw 0 faces +Z: the front rank has the largest Z.
        double shieldZ = army.stream().filter(s -> s.getRole() == SoldierRole.SHIELDBEARER)
                .mapToDouble(s -> s.getSlot().z).min().orElseThrow();
        double archerZ = army.stream().filter(s -> s.getRole() == SoldierRole.ARCHER)
                .mapToDouble(s -> s.getSlot().z).max().orElseThrow();
        h.assertTrue(shieldZ > archerZ, "shieldbearers should stand in front of archers");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void alliesCannotHurtEachOther(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity a = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 3, 4);
        SoldierEntity b = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 5, 4);
        float before = b.getHealth();
        boolean hurt = b.hurt(h.getLevel().damageSources().mobAttack(a), 6.0F);
        h.assertTrue(!hurt && b.getHealth() == before, "friendly fire should be blocked");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void raidersAndRecruitsFight(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity recruit = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 2, 4);
        SoldierEntity raider = raider(h, SoldierRole.SWORDSMAN, 6, 4);
        h.succeedWhen(() -> h.assertTrue(
                recruit.getLastHurtByMob() == raider || raider.getLastHurtByMob() == recruit,
                "the two sides never came to blows"));
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void archerShootsEnemy(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity archer = recruit(h, owner, SoldierRole.ARCHER, Race.ELF, 1, 1);
        Husk husk = h.spawn(EntityType.HUSK, new BlockPos(7, 2, 7));
        husk.setPersistenceRequired();
        h.succeedWhen(() -> h.assertTrue(husk.getLastHurtByMob() == archer, "archer never hit the husk"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void healerMendsWoundedAlly(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity healer = recruit(h, owner, SoldierRole.HEALER, Race.HUMAN, 3, 4);
        SoldierEntity wounded = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 6, 4);
        wounded.setHealth(8.0F);
        h.succeedWhen(() -> {
            h.assertTrue(wounded.isAlive(), "wounded soldier died");
            h.assertTrue(wounded.getHealth() > 8.0F, "healer never healed the wounded soldier");
        });
    }

    // ------------------------------------------------------------------ mana, races, sieges

    /** A complete altar at {@code core}: a 3×3 stone-brick floor and a brazier on each diagonal (optionally one short). */
    private static SummoningAltarBlockEntity altar(GameTestHelper h, BlockPos core, Player owner, boolean complete) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) h.setBlock(core.offset(dx, -1, dz), Blocks.STONE_BRICKS);
        }
        h.setBlock(core, WFRegistry.SUMMONING_ALTAR.get());
        int[][] corners = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
        for (int i = 0; i < (complete ? 4 : 3); i++) {
            h.setBlock(core.offset(corners[i][0], 0, corners[i][1]), WFRegistry.MANA_BRAZIER.get());
        }
        SummoningAltarBlockEntity altar = (SummoningAltarBlockEntity) h.getBlockEntity(core);
        altar.setOwner(owner.getUUID());
        return altar;
    }

    private static ManaWellBlockEntity well(GameTestHelper h, BlockPos pos, Player owner, float mana) {
        h.setBlock(pos, WFRegistry.MANA_WELL.get());
        ManaWellBlockEntity well = (ManaWellBlockEntity) h.getBlockEntity(pos);
        well.setOwner(owner.getUUID());
        well.setMana(mana);
        return well;
    }

    @GameTest(template = ARENA)
    public static void altarSummonsWithWellMana(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(h.absoluteVec(new Vec3(4.5, 2, 7.5)));
        SummoningAltarBlockEntity altar = altar(h, new BlockPos(4, 2, 4), player, true);
        ManaWellBlockEntity well = well(h, new BlockPos(8, 1, 8), player, 0F);
        int cost = SoldierRole.SWORDSMAN.manaCost(Race.HUMAN);
        // Block entities join the mana network on their first tick.
        h.runAfterDelay(2, () -> {
            h.assertTrue(!altar.summon(player, SoldierRole.SWORDSMAN).ok(), "summoning with an empty well should fail");
            well.setMana(100F);
            SummoningAltarBlockEntity.Result result = altar.summon(player, SoldierRole.SWORDSMAN);
            h.assertTrue(result.ok(), "summoning should work with mana in reach: " + result.message());
            h.assertTrue(Math.abs(well.getMana() - (100F - cost)) < 0.01F,
                    "expected " + (100 - cost) + " mana left in the well, had " + well.getMana());
            h.assertEntityPresent(WFRegistry.SOLDIER.get(), new BlockPos(4, 3, 4), 2.0);
            h.assertTrue(SoldierRole.SWORDSMAN.manaCost(Race.HIVE) < cost, "hive troops should be cheaper");
            h.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void incompleteAltarRefusesToSummon(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        SummoningAltarBlockEntity altar = altar(h, new BlockPos(4, 2, 4), player, false);
        ManaWellBlockEntity well = well(h, new BlockPos(8, 1, 8), player, 500F);
        h.runAfterDelay(2, () -> {
            SummoningAltarBlockEntity.Result result = altar.summon(player, SoldierRole.SWORDSMAN);
            h.assertTrue(!result.ok() && result.message().contains("Brazier"), "a missing brazier should block summoning");
            h.assertTrue(well.getMana() == 500F, "a failed summon should not spend mana");
            h.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void towersFireOnlyWithMana(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos towerPos = new BlockPos(1, 1, 4);
        h.setBlock(towerPos, WFRegistry.ARROW_TOWER.get());
        TowerBlockEntity tower = (TowerBlockEntity) h.getBlockEntity(towerPos);
        tower.setOwner(owner.getUUID());
        ManaWellBlockEntity well = well(h, new BlockPos(1, 1, 7), owner, 0F);
        Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(7, 1, 4));
        husk.setPersistenceRequired();
        // Archers in neighbouring test arenas may shoot this husk too: keep it alive so the tower always has a target.
        husk.setInvulnerable(true);
        // Count the tower's own shots: archers in neighbouring test arenas may also shoot at this husk.
        h.startSequence()
                .thenExecuteAfter(80, () -> h.assertTrue(tower.actions() == 0,
                        "a tower with no mana should not fire, but it fired " + tower.actions() + " times"))
                .thenExecute(() -> well.setMana(50F))
                .thenWaitUntil(() -> h.assertTrue(tower.actions() > 0 && well.getMana() < 50F,
                        "a powered tower should draw mana to fire"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void ripeManabloomDropsShards(GameTestHelper h) {
        BlockPos crop = new BlockPos(4, 2, 4);
        h.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState());
        h.setBlock(crop, WFRegistry.MANABLOOM.get().getStateForAge(CropBlock.MAX_AGE));
        // GameTestHelper.destroyBlock breaks without drops; break it for real so the loot table runs.
        h.getLevel().destroyBlock(h.absolutePos(crop), true);
        h.succeedWhen(() -> h.assertItemEntityPresent(WFRegistry.MANA_SHARD.get(), crop, 2.0));
    }

    @GameTest(template = ARENA)
    public static void demonsAreImmuneToFire(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity demon = recruit(h, owner, SoldierRole.SWORDSMAN, Race.DEMON, 4, 4);
        boolean hurt = demon.hurt(h.getLevel().damageSources().inFire(), 5.0F);
        h.assertTrue(!hurt && demon.getHealth() == demon.getMaxHealth(), "fire should not hurt a demon");
        SoldierEntity human = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 6, 4);
        h.assertTrue(human.hurt(h.getLevel().damageSources().inFire(), 5.0F), "fire should hurt a human");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 500)
    public static void raidersBreakThroughWalls(GameTestHelper h) {
        // Box a raider into a cobblestone cell; its objective lies to the east.
        for (int x = 1; x <= 3; x++) {
            for (int z = 3; z <= 5; z++) {
                if (x == 2 && z == 4) continue;
                h.setBlock(new BlockPos(x, 2, z), Blocks.COBBLESTONE);
                h.setBlock(new BlockPos(x, 3, z), Blocks.COBBLESTONE);
            }
        }
        h.setBlock(new BlockPos(2, 4, 4), Blocks.COBBLESTONE);
        SoldierEntity raider = spawnRaw(h, 2, 4);
        raider.setupAsRaider(NpcFaction.MARAUDERS, SoldierRole.SWORDSMAN, UUID.randomUUID(),
                h.absoluteVec(new Vec3(7.5, 2, 4.5)), null, 1);
        h.getLevel().addFreshEntity(raider);
        h.succeedWhen(() -> h.assertTrue(
                h.getBlockState(new BlockPos(3, 2, 4)).isAir() || h.getBlockState(new BlockPos(3, 3, 4)).isAir(),
                "raider never broke out of its cell"));
    }

    @GameTest(template = ARENA)
    public static void warHornStartsWaveCampaign(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        h.setBlock(pos, WFRegistry.WAR_STANDARD.get());
        if (!(h.getBlockEntity(pos) instanceof WarStandardBlockEntity standard)) {
            throw new IllegalStateException("war standard has no block entity");
        }
        h.assertTrue(standard.startCampaign(h.getLevel()), "campaign should start");
        h.assertTrue(standard.isCampaignActive() && standard.isUnderSiege() && standard.getWave() == 1,
                "first wave should begin immediately");
        UUID warband = standard.getWarbandId();
        List<SoldierEntity> attackers = new ArrayList<>();
        for (Entity e : h.getLevel().getAllEntities()) {
            if (e instanceof SoldierEntity s && warband != null && warband.equals(s.getWarbandId())) attackers.add(s);
        }
        h.assertTrue(!attackers.isEmpty(), "the wave should have attackers");
        // Clean up so the attackers don't wander into other tests.
        standard.stopCampaign(h.getLevel());
        attackers.forEach(SoldierEntity::discard);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void demonImpsAreImpalersAndFirecasters(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity imp = recruit(h, owner, SoldierRole.SPEARMAN, Race.DEMON, 3, 4);
        SoldierEntity firecaster = recruit(h, owner, SoldierRole.ARCHER, Race.DEMON, 4, 4);
        SoldierEntity fiend = recruit(h, owner, SoldierRole.SWORDSMAN, Race.DEMON, 5, 4);
        SoldierEntity tiller = recruit(h, owner, SoldierRole.FARMER, Race.DEMON, 6, 4);
        h.assertTrue(imp.getBody() == com.warfront.army.UnitBody.IMP, "demon spearmen should be imps");
        h.assertTrue("Imp Impaler".equals(imp.getUnitName()), "unit name should be Imp Impaler, was " + imp.getUnitName());
        h.assertTrue(firecaster.getBody() == com.warfront.army.UnitBody.IMP, "demon archers should be imps");
        h.assertTrue(imp.getScale() < 0.8F, "imps should be small, scale was " + imp.getScale());
        h.assertTrue(fiend.getBody() == com.warfront.army.UnitBody.HUMANOID, "fiends are not imps");
        h.assertTrue(tiller.getBody() == com.warfront.army.UnitBody.HUMANOID, "demon workers are not imps");
        h.assertTrue(!imp.hurt(h.getLevel().damageSources().fall(), 10.0F), "winged imps take no fall damage");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void championsGetSignatureGear(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity ironbreaker = recruit(h, owner, SoldierRole.CHAMPION, Race.DWARF, 3, 4);
        SoldierEntity berserker = recruit(h, owner, SoldierRole.CHAMPION, Race.ORC, 6, 4);
        h.assertTrue(ironbreaker.getMainHandItem().is(Items.MACE), "Ironbreakers wield a mace");
        h.assertTrue(berserker.getOffhandItem().is(Items.IRON_AXE), "Berserkers dual-wield axes");
        h.assertTrue("Ironbreaker".equals(ironbreaker.getUnitName()), "dwarf champion should be an Ironbreaker");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void demonsTakeWingWithoutAnElytra(GameTestHelper h) {
        Player demon = h.makeMockPlayer(GameType.SURVIVAL);
        demon.setData(WFRegistry.RACE, Race.DEMON.id());
        demon.moveTo(h.absoluteVec(new Vec3(4.5, 6, 4.5)));
        demon.setOnGround(false);
        h.assertTrue(demon.tryToStartFallFlying() && demon.isFallFlying(), "a demon in mid-air should spread its wings");

        Player human = h.makeMockPlayer(GameType.SURVIVAL);
        human.setData(WFRegistry.RACE, Race.HUMAN.id());
        human.moveTo(h.absoluteVec(new Vec3(2.5, 6, 2.5)));
        human.setOnGround(false);
        h.assertTrue(!human.tryToStartFallFlying(), "a human without an elytra cannot glide");

        Vec3 before = demon.getDeltaMovement();
        com.warfront.flight.WingFlight.applyThrust(demon);
        h.assertTrue(demon.getDeltaMovement().subtract(before).length() > 0.01, "wings should add thrust");
        h.succeed();
    }

    // ------------------------------------------------------------------ workers and guards

    private static SoldierEntity posted(GameTestHelper h, Player owner, SoldierRole role, Race race, int x, int z) {
        SoldierEntity s = spawnRaw(h, x, z);
        s.setupAsRecruit(owner, role, race);
        h.getLevel().addFreshEntity(s);
        s.assignPost(s.position(), 0F);
        return s;
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void farmerHarvestsAndReplants(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos crop = new BlockPos(6, 2, 6);
        h.setBlock(crop.below(), Blocks.FARMLAND);
        h.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        SoldierEntity farmer = posted(h, owner, SoldierRole.FARMER, Race.HUMAN, 2, 2);
        farmer.getWorkItems().addItem(new ItemStack(Items.WHEAT_SEEDS, 4));
        h.assertTrue(farmer.getMainHandItem().getItem() instanceof net.minecraft.world.item.HoeItem, "farmers carry a hoe");
        h.succeedWhen(() -> {
            var state = h.getBlockState(crop);
            h.assertTrue(state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) < 7, "the wheat was not harvested and replanted");
            h.assertTrue(com.warfront.entity.work.WorkSites.count(farmer.getWorkItems(), Items.WHEAT) > 0, "the farmer kept no wheat");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void builderRebuildsFromChest(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos low = new BlockPos(6, 2, 4), high = new BlockPos(6, 3, 4);
        h.setBlock(low, Blocks.STONE_BRICKS);
        h.setBlock(high, Blocks.STONE_BRICKS);
        BlockPos chestPos = new BlockPos(2, 2, 7);
        h.setBlock(chestPos, Blocks.CHEST);
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) h.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.STONE_BRICKS, 8));
        SoldierEntity builder = posted(h, owner, SoldierRole.BUILDER, Race.DWARF, 3, 4);
        // Survey only this arena so the builder never touches neighbouring tests.
        builder.setBlueprint(com.warfront.entity.work.Blueprint.survey(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 4)), 4, 1, 3));
        h.assertTrue(builder.getBlueprint().expected(h.absolutePos(high)) != null, "the survey should include the pillar");
        h.setBlock(low, Blocks.AIR);    // raiders knock the pillar down
        h.setBlock(high, Blocks.AIR);
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(low).is(Blocks.STONE_BRICKS) && h.getBlockState(high).is(Blocks.STONE_BRICKS),
                    "the builder did not rebuild the pillar");
            h.assertTrue(chest.getItem(0).getCount() <= 6, "the builder should use bricks from the chest");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void guardAlarmRallysHiddenAllies(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        // An ally sealed in a stone cell cannot see the raider, so only the guard's alarm can send it.
        for (int x = 0; x <= 2; x++) for (int y = 2; y <= 4; y++) for (int z = 6; z <= 8; z++) {
            if (!(x == 1 && z == 7 && y < 4)) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        }
        SoldierEntity ally = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 1, 7);
        SoldierEntity guard = posted(h, owner, SoldierRole.GUARD, Race.HUMAN, 4, 2);
        SoldierEntity enemy = raider(h, SoldierRole.SWORDSMAN, 7, 2);
        h.assertTrue(guard.getOffhandItem().is(Items.SHIELD), "human guards carry a shield");
        h.succeedWhen(() -> h.assertTrue(ally.getTarget() == enemy, "the guard's alarm never reached the hidden ally"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void workersFleeAndKeepTheirPosts(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        owner.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        SoldierEntity farmer = posted(h, owner, SoldierRole.FARMER, Race.ELF, 4, 4);
        SoldierEntity soldier = recruit(h, owner, SoldierRole.SWORDSMAN, Race.ELF, 1, 1);
        h.assertTrue("Grovetender".equals(farmer.getUnitName()), "elven farmers are Grovetenders, was " + farmer.getUnitName());
        h.assertTrue(com.warfront.item.CommanderBatonItem.armyOf(owner).contains(soldier)
                && !com.warfront.item.CommanderBatonItem.armyOf(owner).contains(farmer), "the baton commands the army, not workers");
        SoldierEntity enemy = raider(h, SoldierRole.SWORDSMAN, 7, 4);
        farmer.setTarget(enemy);
        h.assertTrue(farmer.getTarget() == null, "workers never take a target");
        h.runAtTickTime(80, () -> {
            h.assertTrue(enemy.getLastHurtByMob() != farmer, "a farmer fought back");
            h.succeed();
        });
    }
}
