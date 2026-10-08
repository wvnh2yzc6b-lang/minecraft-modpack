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
    public static void eachRaceHasItsOwnSpellSchool(GameTestHelper h) {
        java.util.Set<com.warfront.faction.SpellSchool> seen = new java.util.HashSet<>();
        for (Race r : Race.values()) {
            h.assertTrue(r.school != r.weakSchool, r.displayName() + " is strong and weak in the same school");
            h.assertTrue(seen.add(r.school), r.displayName() + " shares its school " + r.school + " with another race");
        }
        // Iron's Spells isn't in the test world: applying a race must still work without it.
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        Race.DEMON.apply(p);
        Race.clear(p);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void orcRageBuildsIntoFrenzy(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity orc = recruit(h, owner, SoldierRole.SWORDSMAN, Race.ORC, 4, 4);
        for (int i = 0; i < 9; i++) com.warfront.combat.Rage.gain(orc, com.warfront.combat.Rage.ON_HIT);
        h.assertTrue(!com.warfront.combat.Rage.isFrenzied(orc), "90 rage should not frenzy yet, rage "
                + com.warfront.combat.Rage.current(orc));
        com.warfront.combat.Rage.gain(orc, com.warfront.combat.Rage.ON_HIT);
        h.assertTrue(com.warfront.combat.Rage.isFrenzied(orc), "100 rage should frenzy, rage "
                + com.warfront.combat.Rage.current(orc));
        h.assertTrue(com.warfront.combat.Rage.current(orc) == 0, "rage should empty when the frenzy starts");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void demonKillsHarvestSoulsAndBurst(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity demon = recruit(h, owner, SoldierRole.SWORDSMAN, Race.DEMON, 3, 4);
        SoldierEntity prey = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 5, 4);
        double base = demon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        for (int i = 0; i < com.warfront.combat.Souls.MAX + 2; i++) com.warfront.combat.Souls.harvest(demon, prey);
        int souls = com.warfront.combat.Souls.count(demon);
        double empowered = demon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        h.assertTrue(souls == com.warfront.combat.Souls.MAX, "souls should cap at " + com.warfront.combat.Souls.MAX + ", had " + souls);
        h.assertTrue(empowered > base, "souls should raise attack damage: " + base + " -> " + empowered);
        float extra = com.warfront.combat.Souls.burst(demon, prey);
        h.assertTrue(extra > 0 && com.warfront.combat.Souls.count(demon) == 0,
                "a full harvest should burst and empty, extra " + extra + ", souls " + com.warfront.combat.Souls.count(demon));
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void elfArrowsFlyFaster(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity elf = recruit(h, owner, SoldierRole.ARCHER, Race.ELF, 4, 4);
        net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(
                h.getLevel(), elf, new net.minecraft.world.item.ItemStack(Items.ARROW), null);
        arrow.setDeltaMovement(1.0, 0.0, 0.0);
        h.getLevel().addFreshEntity(arrow);
        double vx = arrow.getDeltaMovement().x;
        arrow.discard();
        h.assertTrue(Math.abs(vx - com.warfront.world.GameEvents.ELF_ARROW_SPEED) < 1e-6,
                "elf arrow should leave at " + com.warfront.world.GameEvents.ELF_ARROW_SPEED + "x speed, was " + vx);
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
        Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(7, 2, 4));
        husk.setPersistenceRequired();
        // Archers in neighbouring test arenas may shoot this husk too: keep it alive so the tower always has a target.
        husk.setInvulnerable(true);
        // Count the tower's own shots: archers in neighbouring test arenas may also shoot at this husk.
        h.startSequence()
                .thenExecuteAfter(80, () -> h.assertTrue(tower.actions() == 0,
                        "a tower with no mana should not fire, but it fired " + tower.actions() + " times"))
                .thenExecute(() -> well.setMana(50F))
                .thenWaitUntil(() -> {
                    BlockPos abs = h.absolutePos(towerPos);
                    String key = tower.factionKey(h.getLevel().getServer());
                    h.assertTrue(tower.actions() > 0 && well.getMana() < 50F, "a powered tower should draw mana to fire"
                            + " (shots " + tower.actions() + ", well " + well.getMana() + ", in reach "
                            + com.warfront.mana.ManaNetwork.available(h.getLevel(), abs, key) + ", key " + key
                            + ", well key " + well.factionKey(h.getLevel().getServer()) + ", saw target " + tower.sawTarget() + ", husk alive " + husk.isAlive()
                            + " at " + husk.blockPosition().subtract(abs) + ")");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void hiveSpearmenAreLancersAndBeastsAreCentaurs(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity lancer = recruit(h, owner, SoldierRole.SPEARMAN, Race.HIVE, 2, 2);
        SoldierEntity beast = recruit(h, owner, SoldierRole.BEAST, Race.HIVE, 6, 6);
        h.assertTrue(lancer.getBody() == com.warfront.army.UnitBody.LANCER, "hive spearmen should be lancers");
        h.assertTrue("Lancer-Drone".equals(lancer.getUnitName()), "unit name should be Lancer-Drone, was " + lancer.getUnitName());
        h.assertTrue(beast.getBody() == com.warfront.army.UnitBody.HIVE_BEAST, "the hive beast should use the beast body");
        h.assertTrue(beast.getBbWidth() > 1F && beast.getBbHeight() > beast.getBbWidth(),
                "the beast should have a broad centaur hitbox, was " + beast.getBbWidth() + " wide and " + beast.getBbHeight() + " tall");
        h.assertTrue("Deepmaw".equals(beast.getUnitName()), "unit name should be Deepmaw, was " + beast.getUnitName());
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void beastsAreLimitedAndRaceBound(GameTestHelper h) {
        Player hive = h.makeMockPlayer(GameType.SURVIVAL);
        hive.setData(WFRegistry.RACE, Race.HIVE.id());
        hive.moveTo(h.absoluteVec(new Vec3(4.5, 2, 7.5)));
        Player human = h.makeMockPlayer(GameType.SURVIVAL);
        human.setData(WFRegistry.RACE, Race.HUMAN.id());
        SummoningAltarBlockEntity altar = altar(h, new BlockPos(4, 2, 4), hive, true);
        well(h, new BlockPos(8, 1, 8), hive, 2000F);
        int cap = com.warfront.config.WFConfig.BEAST_LIMIT.get();
        for (int i = 0; i < cap; i++) recruit(h, hive, SoldierRole.BEAST, Race.HIVE, 1 + i % 3, 1 + i / 3);
        h.runAfterDelay(2, () -> {
            SummoningAltarBlockEntity.Result over = altar.summon(hive, SoldierRole.BEAST);
            h.assertTrue(!over.ok() && over.message().contains("most you can field"),
                    "a commander at the beast limit should be refused: " + over.message());
            SummoningAltarBlockEntity.Result humanTry = altar.summon(human, SoldierRole.BEAST);
            h.assertTrue(!humanTry.ok(), "a race without a beast should not summon one");
            h.succeed();
        });
    }

    /** Places {@code n} pylons owned by {@code owner} along the arena's north edge, then its south edge. */
    private static void pylons(GameTestHelper h, Player owner, int n) {
        for (int i = 0; i < n; i++) {
            BlockPos p = i < 9 ? new BlockPos(i, 1, 0) : new BlockPos(i - 9, 1, 8);
            h.setBlock(p, WFRegistry.MANA_PYLON.get());
            ((com.warfront.mana.ManaNodeBlockEntity) h.getBlockEntity(p)).setOwner(owner.getUUID());
        }
    }

    private static com.warfront.block.WarStandardBlockEntity standard(GameTestHelper h, BlockPos pos, Player owner, int wavesWon) {
        h.setBlock(pos, WFRegistry.WAR_STANDARD.get());
        com.warfront.block.WarStandardBlockEntity s = (com.warfront.block.WarStandardBlockEntity) h.getBlockEntity(pos);
        s.setOwner(owner.getUUID());
        s.setWavesWon(wavesWon);
        return s;
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void baseLevelNeedsBuildingsAndWaves(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SummoningAltarBlockEntity altar = altar(h, new BlockPos(4, 2, 4), owner, true);
        well(h, new BlockPos(8, 1, 8), owner, 100F);
        com.warfront.block.WarStandardBlockEntity flag = standard(h, new BlockPos(0, 2, 4), owner, 0);
        pylons(h, owner, 10);
        String key = altar.factionKey(h.getLevel().getServer());
        BlockPos core = h.absolutePos(new BlockPos(4, 2, 4));
        h.runAfterDelay(2, () -> {
            com.warfront.world.BaseLevel.Status built = com.warfront.world.BaseLevel.of(h.getLevel(), core, key);
            h.assertTrue(built.buildings() == 12 && built.level() == 1,
                    "12 buildings and no waves won should stay level 1, was " + built + " at level " + built.level());
            flag.setWavesWon(8);
            com.warfront.world.BaseLevel.Status fought = com.warfront.world.BaseLevel.of(h.getLevel(), core, key);
            h.assertTrue(fought.waves() == 8 && fought.level() == 3,
                    "12 buildings and 8 waves won should be level 3, was " + fought + " at level " + fought.level());
            flag.setWavesWon(40);
            com.warfront.world.BaseLevel.Status capped = com.warfront.world.BaseLevel.of(h.getLevel(), core, key);
            h.assertTrue(capped.level() == 3, "buildings should hold the base at level 3 however many waves are won, was " + capped.level());
            h.assertTrue(com.warfront.world.BaseLevel.beastCap(3) == 6, "a level 3 base should allow 6 beasts");
            h.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void captainsAndChampionsNeedABiggerBase(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        owner.setData(WFRegistry.RACE, Race.HUMAN.id());
        owner.moveTo(h.absoluteVec(new Vec3(4.5, 2, 7.5)));
        SummoningAltarBlockEntity altar = altar(h, new BlockPos(4, 2, 4), owner, true);
        well(h, new BlockPos(8, 1, 8), owner, 2000F);
        standard(h, new BlockPos(0, 2, 4), owner, 3);
        h.runAfterDelay(2, () -> {
            SummoningAltarBlockEntity.Result early = altar.summon(owner, SoldierRole.CAPTAIN);
            h.assertTrue(!early.ok() && early.message().contains("level 2"), "a level 1 base should refuse a captain: " + early.message());
            pylons(h, owner, 4);
            h.runAfterDelay(2, () -> {
                SummoningAltarBlockEntity.Result captain = altar.summon(owner, SoldierRole.CAPTAIN);
                h.assertTrue(captain.ok(), "6 buildings and 3 waves won should summon a captain: " + captain.message());
                SummoningAltarBlockEntity.Result champion = altar.summon(owner, SoldierRole.CHAMPION);
                h.assertTrue(!champion.ok() && champion.message().contains("level 4"),
                        "a level 2 base should refuse a champion: " + champion.message());
                h.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void hiveIsStrongerUnderground(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity drone = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HIVE, 4, 4);
        // Test arenas sit far below Y=40, so this counts as underground.
        com.warfront.world.HiveAdaptation.apply(drone, 60);
        h.assertTrue(drone.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "hive soldiers should gain Strength underground");
        h.assertTrue(!drone.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "hive soldiers should not be weakened underground");
        h.succeed();
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
        // Attackers land 36+ blocks out, possibly in a chunk the test server hasn't made visible, so count what
        // the wave spawned rather than searching the world for them.
        h.assertTrue(standard.getLastSpawned() > 0, "the wave should have attackers, spawned "
                + standard.getLastSpawned() + ", visible " + attackers.size());
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
    public static void guardDutyAlarmRallysHiddenAllies(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        // An ally sealed in a stone cell cannot see the raider, so only the guard's alarm can send it.
        for (int x = 0; x <= 2; x++) for (int y = 2; y <= 4; y++) for (int z = 6; z <= 8; z++) {
            if (!(x == 1 && z == 7 && y < 4)) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        }
        SoldierEntity ally = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 1, 7);
        SoldierEntity guard = recruit(h, owner, SoldierRole.SHIELDBEARER, Race.HUMAN, 4, 2);
        guard.setDuty(com.warfront.army.Duty.GUARD, guard.position(), 0F);
        SoldierEntity enemy = raider(h, SoldierRole.SWORDSMAN, 7, 2);
        h.assertTrue(guard.isPosted() && guard.onWatch(), "a shieldbearer on guard duty should keep watch at its post");
        h.assertTrue(!com.warfront.item.CommanderBatonItem.armyOf(owner).contains(guard), "units on duty leave the baton's army");
        h.succeedWhen(() -> h.assertTrue(ally.getTarget() == enemy, "the guard's alarm never reached the hidden ally"));
    }

    @GameTest(template = ARENA)
    public static void manaGliderIsHumanOnlyAndGlides(GameTestHelper h) {
        Player pilot = h.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack glider = new net.minecraft.world.item.ItemStack(WFRegistry.MANA_GLIDER.get());
        pilot.setData(WFRegistry.RACE, Race.HUMAN.id());
        h.assertTrue(glider.canElytraFly(pilot), "a human should be able to fly the Mana Glider");
        pilot.setData(WFRegistry.RACE, Race.ORC.id());
        h.assertTrue(!glider.canElytraFly(pilot), "only humans fly the Mana Glider");
        pilot.setDeltaMovement(3.0, -1.0, 0.0);
        pilot.setXRot(0F);
        com.warfront.flight.ManaGliderItem.glide(pilot);
        net.minecraft.world.phys.Vec3 v = pilot.getDeltaMovement();
        h.assertTrue(v.length() < 3.0 && v.y >= -com.warfront.flight.ManaGliderItem.MAX_SINK - 1e-6,
                "the glider should bleed speed and soften the sink, motion " + v);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void rocketPackIsOrcOnlyAndBurnsFuel(GameTestHelper h) {
        Player pilot = h.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack pack = new net.minecraft.world.item.ItemStack(WFRegistry.ROCKET_PACK.get());
        net.minecraft.world.item.ItemStack coal = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 2);
        int loaded = com.warfront.flight.RocketPackItem.load(pack, coal);
        int perCoal = com.warfront.config.WFConfig.ROCKET_COAL_SECONDS.get() * 20;
        h.assertTrue(loaded == 2 && coal.isEmpty() && com.warfront.flight.RocketPackItem.fuel(pack) == 2 * perCoal,
                "two coal should load " + 2 * perCoal + " ticks, got " + com.warfront.flight.RocketPackItem.fuel(pack));
        h.assertTrue(com.warfront.flight.RocketPackItem.fuelValue(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT)) == 0,
                "dirt is not rocket fuel");
        pilot.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, pack);
        pilot.setOnGround(false);
        pilot.setData(WFRegistry.RACE, Race.HUMAN.id());
        h.assertTrue(!com.warfront.flight.RocketPackItem.canThrust(pilot), "only orcs fly the rocket pack");
        pilot.setData(WFRegistry.RACE, Race.ORC.id());
        h.assertTrue(com.warfront.flight.RocketPackItem.canThrust(pilot), "a fuelled orc in the air should be able to thrust");
        double before = pilot.getDeltaMovement().y;
        com.warfront.flight.RocketPackItem.thrust(pilot);
        h.assertTrue(pilot.getDeltaMovement().y > before, "thrust should push the orc up");
        h.assertTrue(com.warfront.flight.Rocketry.burn(pilot), "burning a tick of fuel should work");
        net.minecraft.world.item.ItemStack worn = pilot.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        h.assertTrue(com.warfront.flight.RocketPackItem.fuel(worn) == 2 * perCoal - 1, "a tick of thrust should burn a tick of fuel");
        com.warfront.flight.RocketPackItem.setFuel(worn, 0);
        h.assertTrue(!com.warfront.flight.RocketPackItem.canThrust(pilot) && !com.warfront.flight.Rocketry.burn(pilot),
                "an empty pack should not thrust");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void angelWingsFlyOnlyByDayUnderOpenSky(GameTestHelper h) {
        Player angel = h.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack wings = new net.minecraft.world.item.ItemStack(WFRegistry.ANGEL_WINGS.get());
        angel.setData(WFRegistry.RACE, Race.ANGEL.id());
        h.assertTrue(wings.canElytraFly(angel), "an angel's wings should at least glide");
        angel.setData(WFRegistry.RACE, Race.DWARF.id());
        h.assertTrue(!wings.canElytraFly(angel), "only angels use angel wings");
        h.assertTrue(com.warfront.flight.AngelWingsItem.flightAllowed(true, true, false), "day and open sky: free flight");
        h.assertTrue(!com.warfront.flight.AngelWingsItem.flightAllowed(false, true, false), "at night the wings only glide");
        h.assertTrue(!com.warfront.flight.AngelWingsItem.flightAllowed(true, false, false), "under a roof the wings only glide");
        h.assertTrue(!com.warfront.flight.AngelWingsItem.flightAllowed(true, true, true), "no free flight in a thunderstorm");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void windCharmLeapIsElfOnlyAndCostsAShard(GameTestHelper h) {
        Player elf = h.makeMockPlayer(GameType.SURVIVAL);
        elf.setData(WFRegistry.RACE, Race.HUMAN.id());
        elf.getInventory().add(new net.minecraft.world.item.ItemStack(WFRegistry.MANA_SHARD.get(), 2));
        h.assertTrue(!com.warfront.flight.WindCharmItem.leap(elf), "the charm only answers elves");
        elf.setData(WFRegistry.RACE, Race.ELF.id());
        elf.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        h.assertTrue(com.warfront.flight.WindCharmItem.leap(elf), "an elf with shards should leap");
        int shards = elf.getInventory().countItem(WFRegistry.MANA_SHARD.get());
        h.assertTrue(shards == 1, "a Wind Leap costs one Mana Shard, left " + shards);
        h.assertTrue(elf.getDeltaMovement().y >= 0.6 && elf.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),
                "the leap should launch upward and slow the fall, motion " + elf.getDeltaMovement());
        h.assertTrue(com.warfront.flight.WindCharmItem.leap(elf) && !com.warfront.flight.WindCharmItem.leap(elf),
                "no shards, no leap");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void runeDrillBoresThreeByThreeOnlyForDwarves(GameTestHelper h) {
        Player miner = h.makeMockPlayer(GameType.SURVIVAL);
        java.util.List<BlockPos> wall = new java.util.ArrayList<>();
        for (int x = 2; x <= 4; x++) for (int y = 2; y <= 4; y++) wall.add(new BlockPos(x, y, 5));
        wall.forEach(p -> h.setBlock(p, Blocks.STONE));
        BlockPos center = h.absolutePos(new BlockPos(3, 3, 5));
        net.minecraft.world.item.ItemStack drill = new net.minecraft.world.item.ItemStack(WFRegistry.RUNE_DRILL.get());
        miner.setData(WFRegistry.RACE, Race.HUMAN.id());
        int human = com.warfront.tunnel.RuneDrillItem.bore(h.getLevel(), miner, center, net.minecraft.core.Direction.NORTH, drill);
        h.assertTrue(human == 0, "only dwarves can work the Rune Drill, a human bored " + human);
        miner.setData(WFRegistry.RACE, Race.DWARF.id());
        int dwarf = com.warfront.tunnel.RuneDrillItem.bore(h.getLevel(), miner, center, net.minecraft.core.Direction.NORTH, drill);
        h.assertTrue(dwarf == 8, "a dwarf's drill should bore the 8 blocks around the one mined, bored " + dwarf);
        for (BlockPos p : wall) {
            boolean mid = p.equals(new BlockPos(3, 3, 5));
            h.assertTrue(h.getBlockState(p).isAir() != mid, (mid ? "the mined block itself is left to the normal break, " : "a face block should be gone, ") + p);
        }
        h.assertTrue(drill.getDamageValue() == 8, "each extra block should cost a point of durability, used " + drill.getDamageValue());
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void spikeFloorHurtsEnemiesNotOwnUnits(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos spikes = new BlockPos(4, 1, 4);
        h.setBlock(spikes, WFRegistry.SPIKE_FLOOR.get());
        com.warfront.tunnel.TrapBlockEntity trap = (com.warfront.tunnel.TrapBlockEntity) h.getBlockEntity(spikes);
        trap.setOwner(owner.getUUID());
        SoldierEntity mine = posted(h, owner, SoldierRole.SWORDSMAN, Race.DWARF, 2, 2);
        SoldierEntity enemy = raider(h, SoldierRole.SWORDSMAN, 6, 6);
        enemy.setNoAi(true);
        mine.setNoAi(true);
        BlockPos abs = h.absolutePos(spikes);
        h.assertTrue(!com.warfront.tunnel.SpikeFloorBlock.trigger(h.getLevel(), abs, mine) && trap.hits() == 0,
                "spikes must never strike the owner's own units");
        h.assertTrue(com.warfront.tunnel.SpikeFloorBlock.trigger(h.getLevel(), abs, enemy) && trap.hits() == 1,
                "spikes should strike an enemy, hits " + trap.hits());
        h.assertTrue(h.getBlockState(spikes).getValue(com.warfront.tunnel.SpikeFloorBlock.EXTENDED), "the spikes should be up");
        h.assertTrue(!com.warfront.tunnel.SpikeFloorBlock.trigger(h.getLevel(), abs, enemy), "raised spikes don't strike again");
        h.runAfterDelay(com.warfront.tunnel.SpikeFloorBlock.RESET_TICKS + 5, () -> {
            h.assertTrue(!h.getBlockState(spikes).getValue(com.warfront.tunnel.SpikeFloorBlock.EXTENDED), "the spikes should reset");
            h.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void runeMineGoesOffOnceAndBreaksNoBlocks(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos minePos = new BlockPos(4, 1, 4);
        h.setBlock(minePos, WFRegistry.RUNE_MINE.get());
        h.setBlock(new BlockPos(5, 2, 4), Blocks.GLASS);
        ((com.warfront.tunnel.TrapBlockEntity) h.getBlockEntity(minePos)).setOwner(owner.getUUID());
        net.minecraft.world.level.block.state.BlockState floor = h.getBlockState(new BlockPos(3, 1, 4));
        SoldierEntity mine = posted(h, owner, SoldierRole.SWORDSMAN, Race.DWARF, 2, 2);
        SoldierEntity enemy = raider(h, SoldierRole.SWORDSMAN, 4, 5);
        enemy.setNoAi(true);
        mine.setNoAi(true);
        BlockPos abs = h.absolutePos(minePos);
        h.assertTrue(!com.warfront.tunnel.RuneMineBlock.trigger(h.getLevel(), abs, mine), "a rune mine ignores its owner's units");
        h.assertBlockPresent(WFRegistry.RUNE_MINE.get(), minePos);
        h.assertTrue(com.warfront.tunnel.RuneMineBlock.trigger(h.getLevel(), abs, enemy), "a rune mine should go off under an enemy");
        h.assertBlockNotPresent(WFRegistry.RUNE_MINE.get(), minePos);
        h.assertBlockPresent(Blocks.GLASS, new BlockPos(5, 2, 4));
        h.assertTrue(h.getBlockState(new BlockPos(3, 1, 4)) == floor, "the blast must not break the floor");
        h.assertTrue(!com.warfront.tunnel.RuneMineBlock.trigger(h.getLevel(), abs, enemy), "a spent mine can't go off again");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void flameVentFiresOnlyWithMana(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos ventPos = new BlockPos(1, 2, 4);
        h.setBlock(ventPos, WFRegistry.FLAME_VENT.get().defaultBlockState()
                .setValue(com.warfront.tunnel.FlameVentBlock.FACING, net.minecraft.core.Direction.EAST));
        com.warfront.tunnel.TrapBlockEntity vent = (com.warfront.tunnel.TrapBlockEntity) h.getBlockEntity(ventPos);
        vent.setOwner(owner.getUUID());
        ManaWellBlockEntity well = well(h, new BlockPos(1, 1, 7), owner, 0F);
        Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(5, 2, 4));
        husk.setPersistenceRequired();
        husk.setInvulnerable(true);
        h.startSequence()
                .thenExecuteAfter(60, () -> h.assertTrue(vent.hits() == 0,
                        "a flame vent with no mana should not fire, but it fired " + vent.hits() + " times"))
                .thenExecute(() -> well.setMana(50F))
                .thenWaitUntil(() -> h.assertTrue(vent.hits() > 0 && well.getMana() < 50F,
                        "a powered vent should draw mana to fire (bursts " + vent.hits() + ", well " + well.getMana()
                                + ", husk at " + husk.blockPosition().subtract(h.absolutePos(ventPos)) + ")"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void raidersCannotBreachRuneStone(GameTestHelper h) {
        for (net.minecraft.world.level.block.Block b : java.util.List.of(WFRegistry.RUNE_STONE.get(), WFRegistry.RUNE_STONE_STAIRS.get(),
                WFRegistry.RUNE_STONE_SLAB.get(), WFRegistry.RUNE_STONE_WALL.get())) {
            h.assertTrue(com.warfront.entity.ai.BreachGoal.isProtected(b.defaultBlockState()), "raiders should not breach " + b);
            h.assertTrue(b.getExplosionResistance() >= Blocks.OBSIDIAN.getExplosionResistance(), b + " should resist blasts like obsidian");
        }
        h.assertTrue(!com.warfront.entity.ai.BreachGoal.isProtected(Blocks.STONE_BRICKS.defaultBlockState()), "plain stone bricks can be breached");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void resolveBuildsInCombatAndDrainsOutOfIt(GameTestHelper h) {
        Player dwarf = h.makeMockPlayer(GameType.SURVIVAL);
        dwarf.setData(WFRegistry.RACE, Race.DWARF.id());
        long now = h.getLevel().getGameTime();
        com.warfront.combat.Resolve.markCombat(dwarf);
        for (int i = 0; i < 3; i++) com.warfront.combat.Resolve.tickSecond(dwarf, now + i * 20);
        int fought = com.warfront.combat.Resolve.current(dwarf);
        h.assertTrue(fought == 3 * com.warfront.combat.Resolve.PER_SECOND, "three seconds of fighting should give 12 Resolve, got " + fought);
        com.warfront.combat.Resolve.set(dwarf, 60);
        var armor = dwarf.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                .getModifier(com.warfront.Warfront.id("resolve_armor"));
        h.assertTrue(armor != null && armor.amount() == 3, "60 Resolve should give +3 armor, modifier " + armor);
        com.warfront.combat.Resolve.tickSecond(dwarf, now + 400);
        h.assertTrue(com.warfront.combat.Resolve.current(dwarf) == 60 - com.warfront.combat.Resolve.IDLE_LOSS,
                "out of combat Resolve should drain, now " + com.warfront.combat.Resolve.current(dwarf));
        com.warfront.combat.Resolve.set(dwarf, 0);
        h.assertTrue(dwarf.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                .getModifier(com.warfront.Warfront.id("resolve_armor")) == null, "no Resolve, no armor bonus");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void oathOfStoneFiresAtFullAndShakesTheGround(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity dwarf = posted(h, owner, SoldierRole.SHIELDBEARER, Race.DWARF, 4, 4);
        SoldierEntity enemy = raider(h, SoldierRole.SWORDSMAN, 6, 4);
        dwarf.setNoAi(true);
        enemy.setNoAi(true);
        com.warfront.combat.Resolve.set(dwarf, com.warfront.combat.Resolve.MAX);
        com.warfront.combat.Resolve.tickSecond(dwarf);
        h.assertTrue(com.warfront.combat.Resolve.inOath(dwarf) && com.warfront.combat.Resolve.current(dwarf) == 0,
                "a dwarf soldier at full Resolve should swear the Oath and spend it");
        int hit = com.warfront.combat.Resolve.shockwave(dwarf);
        h.assertTrue(hit == 1, "the shockwave should hit the one enemy nearby, hit " + hit);
        h.assertTrue(enemy.getDeltaMovement().horizontalDistance() > 0.2, "the shockwave should knock the enemy back, motion "
                + enemy.getDeltaMovement());
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void dwarvesMineMoreManaCrystals(GameTestHelper h) {
        Player dwarf = h.makeMockPlayer(GameType.SURVIVAL);
        dwarf.setData(WFRegistry.RACE, Race.DWARF.id());
        Player human = h.makeMockPlayer(GameType.SURVIVAL);
        human.setData(WFRegistry.RACE, Race.HUMAN.id());
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(0, 0, 0))).expandTowards(9, 9, 9);
        int[] crystals = new int[2];
        Player[] miners = {dwarf, human};
        for (int m = 0; m < 2; m++) {
            for (int x = 1; x <= 7; x++) {
                for (int z = 2; z <= 4; z++) {
                    BlockPos p = new BlockPos(x, 2, z);
                    h.setBlock(p, WFRegistry.MANA_ORE.get());
                    h.getLevel().destroyBlock(h.absolutePos(p), true, miners[m]);
                }
            }
            for (net.minecraft.world.entity.item.ItemEntity item : h.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, box)) {
                if (item.getItem().is(WFRegistry.MANA_CRYSTAL.get())) crystals[m] += item.getItem().getCount();
                item.discard();
            }
        }
        h.assertTrue(crystals[0] > crystals[1] && crystals[1] > 0, "dwarves should get more crystals from 21 Mana Ore: dwarf "
                + crystals[0] + ", human " + crystals[1]);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void valorRisesFromPlayerAndUnitKills(GameTestHelper h) {
        Player human = h.makeMockPlayer(GameType.SURVIVAL);
        human.setData(WFRegistry.RACE, Race.HUMAN.id());
        human.moveTo(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
        SoldierEntity enemy = raider(h, SoldierRole.SWORDSMAN, 6, 6);
        enemy.setNoAi(true);
        com.warfront.combat.Valor.creditKill(human, enemy);
        h.assertTrue(com.warfront.combat.Valor.current(human) == com.warfront.combat.Valor.PLAYER_KILL,
                "a human's own kill should give 10 Valor, got " + com.warfront.combat.Valor.current(human));
        SoldierEntity unit = posted(h, human, SoldierRole.SWORDSMAN, Race.HUMAN, 3, 3);
        com.warfront.combat.Valor.creditUnitKill(human, unit);
        h.assertTrue(com.warfront.combat.Valor.current(human) == com.warfront.combat.Valor.PLAYER_KILL + com.warfront.combat.Valor.UNIT_KILL,
                "a nearby unit's kill should add 4 Valor, got " + com.warfront.combat.Valor.current(human));
        Player orc = h.makeMockPlayer(GameType.SURVIVAL);
        orc.setData(WFRegistry.RACE, Race.ORC.id());
        com.warfront.combat.Valor.creditKill(orc, enemy);
        h.assertTrue(com.warfront.combat.Valor.current(orc) == 0, "only humans gather Valor");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void rallyHealsBuffsAndEndsTheRout(GameTestHelper h) {
        Player human = h.makeMockPlayer(GameType.SURVIVAL);
        human.setData(WFRegistry.RACE, Race.HUMAN.id());
        human.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        SoldierEntity unit = posted(h, human, SoldierRole.SWORDSMAN, Race.HUMAN, 5, 5);
        unit.setNoAi(true);
        unit.setHealth(unit.getMaxHealth() * 0.3F);
        unit.routFor(200);
        float before = unit.getHealth();
        h.assertTrue(com.warfront.combat.Valor.rally(human) < 0, "no Rally before Valor is full");
        com.warfront.combat.Valor.set(human, com.warfront.combat.Valor.MAX);
        int reached = com.warfront.combat.Valor.rally(human);
        h.assertTrue(reached >= 2, "the Rally should reach the player and their unit, reached " + reached);
        h.assertTrue(unit.getHealth() > before && unit.hasEffect(WFRegistry.RALLIED) && !unit.isRouting(),
                "the rallied unit should be healed, buffed and done routing (health " + before + " -> " + unit.getHealth() + ")");
        h.assertTrue(com.warfront.combat.Valor.current(human) == 0, "the Rally spends all Valor");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void claimedVillagersCanBeHired(GameTestHelper h) {
        Player human = h.makeMockPlayer(GameType.SURVIVAL);
        human.setData(WFRegistry.RACE, Race.HUMAN.id());
        net.minecraft.world.entity.npc.Villager villager = h.spawn(EntityType.VILLAGER, new BlockPos(4, 2, 4));
        net.minecraft.world.item.ItemStack shards = new net.minecraft.world.item.ItemStack(WFRegistry.MANA_SHARD.get(), 3);
        h.assertTrue(com.warfront.item.VillageCharterItem.hire(human, villager, shards) == null, "no charter, no hiring");
        com.warfront.item.VillageCharterItem.claimAt(human, h.absolutePos(new BlockPos(4, 2, 4)));
        SoldierEntity worker = com.warfront.item.VillageCharterItem.hire(human, villager, shards);
        h.assertTrue(worker != null && worker.getRole() == SoldierRole.FARMER && human.getUUID().equals(worker.getOwnerUUID()),
                "a hired villager should become the player's Farmhand, got " + (worker == null ? "nothing" : worker.getRole()));
        h.assertTrue(villager.isRemoved() && shards.getCount() == 2, "the villager is replaced and a shard is paid");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void merchantSellsCheaperToHumans(GameTestHelper h) {
        net.minecraft.world.item.trading.MerchantOffer offer = new net.minecraft.world.item.trading.MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(WFRegistry.MANA_CRYSTAL.get(), 5),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND), 3, 2, 0.05F);
        int human = com.warfront.merchant.MerchantEntity.priceFor(offer, true);
        int other = com.warfront.merchant.MerchantEntity.priceFor(offer, false);
        h.assertTrue(human == 4 && other == 5, "humans should pay 4 crystals where others pay 5, got " + human + " and " + other);
        long now = 1000;
        Player humanPlayer = h.makeMockPlayer(GameType.SURVIVAL);
        humanPlayer.setData(WFRegistry.RACE, Race.HUMAN.id());
        long wait = com.warfront.merchant.Caravan.nextVisit(now, net.minecraft.util.RandomSource.create(5), humanPlayer) - now;
        long base = com.warfront.merchant.Caravan.nextVisit(now, net.minecraft.util.RandomSource.create(5)) - now;
        h.assertTrue(wait < base, "the caravan should come to humans sooner: " + wait + " vs " + base);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void radianceGathersInTheSunAndFadesBelow(GameTestHelper h) {
        Player angel = h.makeMockPlayer(GameType.SURVIVAL);
        angel.setData(WFRegistry.RACE, Race.ANGEL.id());
        for (int i = 0; i < 3; i++) com.warfront.combat.Radiance.tickSecond(angel, true);
        h.assertTrue(com.warfront.combat.Radiance.current(angel) == 3 * com.warfront.combat.Radiance.SUN_GAIN,
                "three sunlit seconds should give 6 Radiance, got " + com.warfront.combat.Radiance.current(angel));
        com.warfront.combat.Radiance.tickSecond(angel, false);
        h.assertTrue(com.warfront.combat.Radiance.current(angel) == 3 * com.warfront.combat.Radiance.SUN_GAIN - com.warfront.combat.Radiance.FADE,
                "out of the sun Radiance should fade, got " + com.warfront.combat.Radiance.current(angel));
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void judgmentSmitesEnemiesAndSparesAllies(GameTestHelper h) {
        Player angel = h.makeMockPlayer(GameType.SURVIVAL);
        angel.setData(WFRegistry.RACE, Race.ANGEL.id());
        SoldierEntity ally = posted(h, angel, SoldierRole.SWORDSMAN, Race.ANGEL, 3, 4);
        SoldierEntity enemy = raider(h, SoldierRole.SWORDSMAN, 5, 4);
        net.minecraft.world.entity.monster.Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 6));
        ally.setNoAi(true);
        enemy.setNoAi(true);
        String key = com.warfront.faction.Factions.keyOf(h.getLevel().getServer(), angel);
        java.util.Map<net.minecraft.world.entity.LivingEntity, Float> hits = com.warfront.combat.Radiance.strike(h.getLevel(),
                h.absoluteVec(new Vec3(4.5, 2, 4.5)), key, com.warfront.combat.Radiance.RADIUS);
        h.assertTrue(!hits.containsKey(ally), "Judgment must spare the caster's own units");
        h.assertTrue(Float.valueOf(com.warfront.combat.Radiance.DAMAGE).equals(hits.get(enemy)), "an enemy should take 12, hits " + hits.values());
        h.assertTrue(Float.valueOf(com.warfront.combat.Radiance.DAMAGE * com.warfront.combat.Radiance.BONUS).equals(hits.get(zombie)),
                "the undead should take half again, got " + hits.get(zombie));
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void sunwellsGatherManaByDayOnly(GameTestHelper h) {
        Player angel = h.makeMockPlayer(GameType.SURVIVAL);
        ManaWellBlockEntity well = well(h, new BlockPos(4, 1, 4), angel, 0F);
        h.assertTrue(well.sunTick(true) == 0F, "a plain Mana Well gathers nothing from the sun");
        well.setSunwell(true);
        h.assertTrue(well.sunTick(true) > 0F && well.getMana() > 0F, "a Sunwell under open sky should gather mana by day");
        float before = well.getMana();
        h.assertTrue(well.sunTick(false) == 0F && well.getMana() == before, "a Sunwell gathers nothing at night");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void swarmMeterFillsTwiceAsFastUnderground(GameTestHelper h) {
        Player hive = h.makeMockPlayer(GameType.SURVIVAL);
        hive.setData(WFRegistry.RACE, Race.HIVE.id());
        com.warfront.combat.SwarmCall.gain(hive, com.warfront.combat.SwarmCall.PLAYER_KILL, false);
        h.assertTrue(com.warfront.combat.SwarmCall.current(hive) == 8, "a kill on the surface gives 8, got " + com.warfront.combat.SwarmCall.current(hive));
        com.warfront.combat.SwarmCall.gain(hive, com.warfront.combat.SwarmCall.PLAYER_KILL, true);
        h.assertTrue(com.warfront.combat.SwarmCall.current(hive) == 24, "a kill underground gives 16 more, got " + com.warfront.combat.SwarmCall.current(hive));
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void broodCallSummonsFourOutsideTheArmyCap(GameTestHelper h) {
        Player hive = h.makeMockPlayer(GameType.SURVIVAL);
        hive.setData(WFRegistry.RACE, Race.HIVE.id());
        hive.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        h.assertTrue(com.warfront.combat.SwarmCall.call(hive, false).isEmpty(), "no Brood Call below 50");
        com.warfront.combat.SwarmCall.set(hive, 60);
        java.util.List<SoldierEntity> brood = com.warfront.combat.SwarmCall.call(hive, false);
        h.assertTrue(brood.size() == 4 && com.warfront.combat.SwarmCall.current(hive) == 10,
                "a Brood Call spends 50 for 4 units, got " + brood.size() + " and meter " + com.warfront.combat.SwarmCall.current(hive));
        for (SoldierEntity s : brood) {
            h.assertTrue(s.isSwarmCalled() && !s.countsTowardArmy() && hive.getUUID().equals(s.getOwnerUUID()),
                    "called units are the player's but outside the army cap");
        }
        com.warfront.combat.SwarmCall.set(hive, com.warfront.combat.SwarmCall.MAX);
        java.util.List<SoldierEntity> maw = com.warfront.combat.SwarmCall.call(hive, true);
        h.assertTrue(maw.size() == 1 && maw.get(0).getRole() == SoldierRole.BEAST && !maw.get(0).isHero()
                && !maw.get(0).countsTowardArmy() && com.warfront.combat.SwarmCall.current(hive) == 0,
                "a Deepmaw Call at 100 brings one temporary Deepmaw that ignores the beast limit and never falls as a hero");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void swarmCalledUnitsBurrowAwayAfterTheFight(GameTestHelper h) {
        Player hive = h.makeMockPlayer(GameType.SURVIVAL);
        hive.setData(WFRegistry.RACE, Race.HIVE.id());
        hive.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        com.warfront.combat.SwarmCall.set(hive, com.warfront.combat.SwarmCall.BROOD);
        SoldierEntity s = com.warfront.combat.SwarmCall.call(hive, false).get(0);
        h.assertTrue(!com.warfront.combat.SwarmCall.idleTick(s, true, true), "a unit still fighting stays");
        for (int i = 0; i < com.warfront.combat.SwarmCall.LEAVE_SECONDS - 1; i++) com.warfront.combat.SwarmCall.idleTick(s, false, true);
        h.assertTrue(!s.isRemoved(), "it stays until 20 quiet seconds have passed");
        h.assertTrue(com.warfront.combat.SwarmCall.idleTick(s, false, true) && s.isRemoved(), "after 20 quiet seconds it burrows away");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void towersShowWhetherTheyHaveMana(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos towerPos = new BlockPos(1, 1, 1);
        h.setBlock(towerPos, WFRegistry.ARCANE_SPIRE.get());
        TowerBlockEntity tower = (TowerBlockEntity) h.getBlockEntity(towerPos);
        tower.setOwner(owner.getUUID());
        tower.setRace(Race.DWARF.id());
        ManaWellBlockEntity well = well(h, new BlockPos(1, 1, 3), owner, 0F);
        h.assertTrue(Race.DWARF.id().equals(tower.race()), "the tower should remember its owner's race for its look");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(!tower.isPowered(), "a tower with no mana should show as unpowered"))
                .thenExecute(() -> well.setMana(100F))
                .thenWaitUntil(() -> h.assertTrue(tower.isPowered(), "a tower with mana in reach should show as powered"))
                .thenSucceed();
    }

    private static com.warfront.racetower.RaceTowerBlockEntity raceTower(GameTestHelper h, com.warfront.racetower.RaceTowerType type,
                                                                        Player owner, float mana) {
        BlockPos pos = new BlockPos(1, 1, 4);
        h.setBlock(pos, WFRegistry.RACE_TOWERS.get(type).get());
        com.warfront.racetower.RaceTowerBlockEntity tower = (com.warfront.racetower.RaceTowerBlockEntity) h.getBlockEntity(pos);
        tower.setOwner(owner.getUUID());
        well(h, new BlockPos(1, 1, 7), owner, mana);
        return tower;
    }

    private static Husk target(GameTestHelper h, int x, int z) {
        Husk husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(x, 2, z));
        husk.setPersistenceRequired();
        husk.setInvulnerable(true);
        return husk;
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void raceTowersKeepToTheirRaceLevelAndCap(GameTestHelper h) {
        Player human = h.makeMockPlayer(GameType.SURVIVAL);
        human.setData(WFRegistry.RACE, Race.HUMAN.id());
        Player orc = h.makeMockPlayer(GameType.SURVIVAL);
        orc.setData(WFRegistry.RACE, Race.ORC.id());
        var ballista = com.warfront.racetower.RaceTowerType.BALLISTA;
        BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
        h.assertTrue(com.warfront.racetower.RaceTowers.refusal(h.getLevel(), at, orc, ballista) != null, "an orc can't raise a Ballista");
        String noBase = com.warfront.racetower.RaceTowers.refusal(h.getLevel(), at, human, ballista);
        h.assertTrue(noBase != null && noBase.contains("level 2"), "no base, no Ballista: " + noBase);
        ManaWellBlockEntity well = well(h, new BlockPos(4, 1, 7), human, 0F);
        well.setTestLevel(2);
        for (int x : new int[]{1, 7}) {
            h.setBlock(new BlockPos(x, 1, 1), WFRegistry.RACE_TOWERS.get(ballista).get());
            ((com.warfront.racetower.RaceTowerBlockEntity) h.getBlockEntity(new BlockPos(x, 1, 1))).setOwner(human.getUUID());
        }
        h.runAfterDelay(5, () -> {
            String full = com.warfront.racetower.RaceTowers.refusal(h.getLevel(), at, human, ballista);
            h.assertTrue(full != null && full.contains("most race towers"), "a level 2 base holds two race towers: " + full);
            well.setTestLevel(3);
            h.assertTrue(com.warfront.racetower.RaceTowers.refusal(h.getLevel(), at, human, ballista) == null,
                    "a level 3 base has room for more");
            h.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void ballistaFiresOnlyWithMana(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.BALLISTA, owner, 0F);
        target(h, 6, 4);
        ManaWellBlockEntity well = (ManaWellBlockEntity) h.getBlockEntity(new BlockPos(1, 1, 7));
        h.startSequence()
                .thenExecuteAfter(80, () -> h.assertTrue(tower.actions() == 0, "no mana, no bolt; fired " + tower.actions()))
                .thenExecute(() -> well.setMana(50F))
                .thenWaitUntil(() -> h.assertTrue(tower.actions() > 0 && well.getMana() < 50F, "a powered Ballista should fire"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void thornwoodSentinelRootsEnemies(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.THORNWOOD_SENTINEL, owner, 100F);
        Husk husk = target(h, 4, 4);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0 && husk.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                && husk.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "the Sentinel should root and mark the enemy"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void runeCannonFires(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.RUNE_CANNON, owner, 100F);
        target(h, 6, 4);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0, "the Rune Cannon should fire at the enemy"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void warDrumFeedsOrcRage(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.WAR_DRUM_TOTEM, owner, 100F);
        SoldierEntity orc = posted(h, owner, SoldierRole.SWORDSMAN, Race.ORC, 4, 4);
        orc.setNoAi(true);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0 && com.warfront.combat.Rage.current(orc) > 0,
                "the drum should give nearby orcs Rage (beats " + tower.actions() + ")"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void soulPyreSetsEnemiesAlight(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.SOUL_PYRE, owner, 100F);
        Husk husk = target(h, 5, 4);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0 && husk.getRemainingFireTicks() > 0, "the Soul Pyre should set the enemy alight"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void sunLanceFires(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.SUN_LANCE, owner, 100F);
        target(h, 6, 4);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0, "the Sun Lance should strike the enemy"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void lurkerPitAmbushesWithAcid(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.LURKER_PIT, owner, 100F);
        Husk husk = target(h, 3, 4);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0 && tower.isRevealed() && husk.hasEffect(WFRegistry.CORRODED),
                "the Lurker Pit should rise and corrode the enemy"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void watchtowerBellRingsAndExtendsTowerRange(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var bell = raceTower(h, com.warfront.racetower.RaceTowerType.WATCHTOWER_BELL, owner, 100F);
        h.setBlock(new BlockPos(3, 1, 1), WFRegistry.ARROW_TOWER.get());
        ((TowerBlockEntity) h.getBlockEntity(new BlockPos(3, 1, 1))).setOwner(owner.getUUID());
        target(h, 6, 4);
        String key = com.warfront.faction.Factions.keyOf(h.getLevel().getServer(), owner);
        h.succeedWhen(() -> {
            h.assertTrue(bell.actions() > 0, "the bell should ring for an enemy in range");
            double bonus = com.warfront.racetower.RaceTowers.rangeBonus(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 1)), key);
            h.assertTrue(bonus == 1.25, "a tower beside the bell should reach a quarter further, got " + bonus);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void moonwellHealsElves(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.MOONWELL_GROVE, owner, 100F);
        SoldierEntity elf = posted(h, owner, SoldierRole.ARCHER, Race.ELF, 4, 4);
        elf.setNoAi(true);
        elf.setHealth(4F);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0 && elf.getHealth() > 4F, "the Moonwell should heal the elf"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void stoneWardenDrawsTheBlows(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.STONE_WARDEN, owner, 100F);
        target(h, 2, 4);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0 && tower.health() < com.warfront.racetower.RaceTowerBlockEntity.WARDEN_HEALTH,
                "a taunted enemy beside the Warden should wear it down (health " + tower.health() + ")"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void goblinCatapultLobsBombs(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.GOBLIN_CATAPULT, owner, 100F);
        target(h, 7, 4);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0, "the catapult should lob a bomb at the enemy"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void brimstoneChainsDragEnemiesIn(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.BRIMSTONE_CHAINS, owner, 100F);
        Husk husk = target(h, 6, 4);
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0 && husk.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),
                "the chains should grab and hold the enemy"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void choirBellShieldsAndCleanses(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.CHOIR_BELL, owner, 100F);
        SoldierEntity unit = posted(h, owner, SoldierRole.SWORDSMAN, Race.ANGEL, 4, 4);
        unit.setNoAi(true);
        unit.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 400, 0));
        h.succeedWhen(() -> h.assertTrue(tower.actions() > 0 && unit.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION)
                && !unit.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "the Choir Bell should shield and cleanse the unit"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void broodNestHatchesTemporarySwarmlings(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        var tower = raceTower(h, com.warfront.racetower.RaceTowerType.BROOD_NEST, owner, 100F);
        target(h, 6, 4);
        h.succeedWhen(() -> {
            h.assertTrue(tower.actions() > 0, "the nest should hatch a swarmling with an enemy near");
            java.util.List<SoldierEntity> spawn = h.getLevel().getEntitiesOfClass(SoldierEntity.class,
                    new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(1, 2, 4))).inflate(8), s -> owner.getUUID().equals(s.getOwnerUUID()));
            h.assertTrue(!spawn.isEmpty() && spawn.stream().allMatch(s -> s.isTemporary() && !s.countsTowardArmy()),
                    "swarmlings are temporary and outside the army cap");
        });
    }

    @GameTest(template = ARENA)
    public static void orcWorkersAreGoblins(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity goblin = posted(h, owner, SoldierRole.FARMER, Race.ORC, 2, 4);
        SoldierEntity brute = recruit(h, owner, SoldierRole.SWORDSMAN, Race.ORC, 6, 4);
        h.assertTrue(goblin.getBody() == com.warfront.army.UnitBody.GOBLIN, "orc farmers should be goblins");
        h.assertTrue(brute.getBody() == com.warfront.army.UnitBody.HUMANOID, "orc soldiers are not goblins");
        h.assertTrue(goblin.getScale() < brute.getScale() * 0.8F,
                "goblins should be well under orc size: " + goblin.getScale() + " vs " + brute.getScale());
        h.succeed();
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

    // ------------------------------------------------------------------ test mode

    private static Player tester(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.CREATIVE);
        p.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        return p;
    }

    @GameTest(template = ARENA)
    public static void testModeIsGatedAndSpawnsUnits(GameTestHelper h) {
        Player p = tester(h);
        h.assertTrue(!com.warfront.test.TestActions.allowed(p), "test tools need test mode on");
        p.setData(WFRegistry.TEST_MODE, true);
        h.assertTrue(!com.warfront.test.TestActions.allowed(p), "test tools also need operator rights");
        var r = com.warfront.test.TestActions.spawn(p, Race.ORC, SoldierRole.SWORDSMAN, 3, "friendly");
        int owned = com.warfront.test.TestActions.owned(p).size();
        h.assertTrue(r.ok() && owned == 3, "spawn 3 friendly should give 3 owned units, had " + owned + " (" + r.message() + ")");
        com.warfront.test.TestActions.army(p, "heal");
        com.warfront.test.TestActions.army(p, "dismiss");
        h.assertTrue(com.warfront.test.TestActions.owned(p).isEmpty(), "dismiss all should remove the army");
        var e = com.warfront.test.TestActions.spawn(p, null, SoldierRole.ARCHER, 2, "the_swarm");
        List<SoldierEntity> swarm = h.getLevel().getEntitiesOfClass(SoldierEntity.class, p.getBoundingBox().inflate(12),
                s -> NpcFaction.THE_SWARM.key().equals(s.getFactionKey()));
        h.assertTrue(e.ok() && swarm.size() >= 2, "enemy spawn should make Swarm raiders, found " + swarm.size());
        swarm.forEach(SoldierEntity::discard);
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void testModeSetsBaseLevelAndMana(GameTestHelper h) {
        Player p = tester(h);
        BlockPos wellPos = new BlockPos(4, 2, 4);
        h.setBlock(wellPos, WFRegistry.MANA_WELL.get());
        if (!(h.getBlockEntity(wellPos) instanceof ManaWellBlockEntity well)) throw new IllegalStateException("no well");
        well.setOwner(p.getUUID());
        h.runAtTickTime(3, () -> {
            BlockPos abs = h.absolutePos(wellPos);
            String key = "p:" + p.getUUID();
            com.warfront.test.TestActions.setBaseLevel(p, 4, 3);
            var base = com.warfront.world.BaseLevel.of(h.getLevel(), abs, key);
            h.assertTrue(base.level() == 4 && base.isTest(), "TEST base level should be 4, was " + base.level());
            com.warfront.test.TestActions.fillMana(p, 3);
            h.assertTrue(well.getMana() >= ManaWellBlockEntity.capacity(), "fill should top up the well, has " + well.getMana());
            com.warfront.test.TestActions.setInfinite(p, true, 3);
            well.take(50F);
            h.assertTrue(well.getMana() >= ManaWellBlockEntity.capacity(), "an infinite well never drains");
            com.warfront.test.TestActions.setInfinite(p, false, 3);
            com.warfront.test.TestActions.setBaseLevel(p, 0, 3);
            h.assertTrue(!com.warfront.world.BaseLevel.of(h.getLevel(), abs, key).isTest(), "level 0 clears the TEST level");
            h.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void testModeStartsAndEndsWaves(GameTestHelper h) {
        Player p = tester(h);
        BlockPos pos = new BlockPos(4, 2, 4);
        h.setBlock(pos, WFRegistry.WAR_STANDARD.get());
        if (!(h.getBlockEntity(pos) instanceof WarStandardBlockEntity standard)) throw new IllegalStateException("no standard");
        var r = com.warfront.test.TestActions.jumpToWave(p, 6);
        h.assertTrue(r.ok() && standard.isUnderSiege() && standard.getWave() == 6,
                "jump to wave 6 should start wave 6, wave " + standard.getWave() + " (" + r.message() + ")");
        com.warfront.test.TestActions.endSiege(p);
        h.assertTrue(!standard.isUnderSiege(), "end siege should stop the wave");
        var s = com.warfront.test.TestActions.startWave(p, NpcFaction.IRONBEARD_CLAN);
        h.assertTrue(s.ok() && standard.isUnderSiege() && standard.getLastSpawned() > 0, "start wave should spawn attackers");
        com.warfront.test.TestActions.endSiege(p);
        h.succeed();
    }

    // ------------------------------------------------------------------ advisor

    @GameTest(template = ARENA)
    public static void advisorQuestAdvancesAndSkips(GameTestHelper h) {
        Player p = tester(h);
        com.warfront.advisor.Advisor.setStep(p, com.warfront.advisor.Advisor.Step.WELL);
        com.warfront.advisor.Advisor.complete(p, com.warfront.advisor.Advisor.Step.ALTAR);
        h.assertTrue(com.warfront.advisor.Advisor.step(p) == com.warfront.advisor.Advisor.Step.WELL, "doing a later step early shouldn't count");
        com.warfront.advisor.Advisor.complete(p, com.warfront.advisor.Advisor.Step.WELL);
        h.assertTrue(com.warfront.advisor.Advisor.step(p) == com.warfront.advisor.Advisor.Step.BLOOM, "placing a well should advance to Manabloom");
        h.assertTrue(p.getInventory().countItem(WFRegistry.MANA_SHARD.get()) >= 8, "the well step rewards 8 shards");
        com.warfront.advisor.Advisor.setStep(p, com.warfront.advisor.Advisor.Step.SUMMON);
        for (int i = 0; i < com.warfront.advisor.Advisor.SUMMONS_NEEDED - 1; i++) {
            com.warfront.advisor.Advisor.complete(p, com.warfront.advisor.Advisor.Step.SUMMON);
        }
        h.assertTrue(com.warfront.advisor.Advisor.step(p) == com.warfront.advisor.Advisor.Step.SUMMON, "two summons aren't three");
        com.warfront.advisor.Advisor.complete(p, com.warfront.advisor.Advisor.Step.SUMMON);
        h.assertTrue(com.warfront.advisor.Advisor.step(p) == com.warfront.advisor.Advisor.Step.STANDARD, "three summons finish the step");
        com.warfront.advisor.Advisor.skip(p);
        h.assertTrue(com.warfront.advisor.Advisor.step(p) == com.warfront.advisor.Advisor.Step.DONE, "skipping ends the quest line");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void advisorCannotBeHurtOrPushed(GameTestHelper h) {
        com.warfront.advisor.AdvisorEntity a = WFRegistry.ADVISOR_ENTITY.get().create(h.getLevel());
        if (a == null) throw new IllegalStateException("advisor failed to create");
        BlockPos abs = h.absolutePos(new BlockPos(4, 2, 4));
        a.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0F, 0F);
        a.setDisguise(Race.DWARF);
        h.getLevel().addFreshEntity(a);
        Player p = tester(h);
        boolean hurt = a.hurt(h.getLevel().damageSources().playerAttack(p), 100F);
        h.assertTrue(!hurt && a.isAlive() && a.getHealth() == a.getMaxHealth(), "the advisor can't be hurt");
        h.assertTrue(!a.isPushable(), "the advisor can't be pushed");
        h.assertTrue("Runekeeper".equals(com.warfront.advisor.Advisor.title(a.getDisguise())), "a dwarf advisor is a Runekeeper");
        a.discard();
        h.succeed();
    }

    // ------------------------------------------------------------------ difficulty and pacing

    @GameTest(template = ARENA)
    public static void difficultyPresetScalesRaiders(GameTestHelper h) {
        var war = com.warfront.war.WarState.get(h.getLevel().getServer());
        var old = war.preset();
        BlockPos at = h.absolutePos(new BlockPos(4, 2, 4));
        war.setPreset(com.warfront.war.WarState.Preset.NORMAL);
        SoldierEntity normal = com.warfront.world.WarbandSpawner.spawnInto(h.getLevel(), UUID.randomUUID(), NpcFaction.MARAUDERS,
                List.of(SoldierRole.SWORDSMAN), at, null, null, 1).get(0);
        war.setPreset(com.warfront.war.WarState.Preset.HARD);
        SoldierEntity hard = com.warfront.world.WarbandSpawner.spawnInto(h.getLevel(), UUID.randomUUID(), NpcFaction.MARAUDERS,
                List.of(SoldierRole.SWORDSMAN), at, null, null, 1).get(0);
        war.setPreset(old);
        float ratio = hard.getMaxHealth() / normal.getMaxHealth();
        normal.discard();
        hard.discard();
        h.assertTrue(Math.abs(ratio - 1.25F) < 0.05F, "Hard raiders should have 1.25x health, ratio " + ratio);
        int easy = com.warfront.world.WarbandSpawner.raidComposition(net.minecraft.util.RandomSource.create(7), NpcFaction.MARAUDERS, 3,
                com.warfront.war.WarState.Preset.EASY).size();
        int warlord = com.warfront.world.WarbandSpawner.raidComposition(net.minecraft.util.RandomSource.create(7), NpcFaction.MARAUDERS, 3,
                com.warfront.war.WarState.Preset.WARLORD).size();
        int level1 = com.warfront.world.WarbandSpawner.raidComposition(net.minecraft.util.RandomSource.create(7), NpcFaction.MARAUDERS, 1,
                com.warfront.war.WarState.Preset.WARLORD).size();
        h.assertTrue(warlord > easy && warlord > level1, "raids grow with preset and base level: easy " + easy
                + ", warlord " + warlord + ", warlord at level 1 " + level1);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void noRaidsInTheGracePeriod(GameTestHelper h) {
        int raid = com.warfront.advisor.Advisor.Step.RAID.ordinal();
        int standard = com.warfront.advisor.Advisor.Step.STANDARD.ordinal();
        h.assertTrue(!com.warfront.war.RaidScheduler.graceOver(1, 3, raid), "no raids before day 3");
        h.assertTrue(!com.warfront.war.RaidScheduler.graceOver(5, 3, standard), "no raids before the War Standard is planted");
        h.assertTrue(com.warfront.war.RaidScheduler.graceOver(3, 3, raid), "raids may come from day 3 with a standard planted");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void recallBringsFollowersHomeOncePerRaid(GameTestHelper h) {
        Player p = tester(h);
        SoldierEntity follower = recruit(h, p, SoldierRole.SWORDSMAN, Race.HUMAN, 7, 7);
        follower.command(Order.FOLLOW, Formation.LINE, null, 0F);
        SoldierEntity holder = recruit(h, p, SoldierRole.ARCHER, Race.HUMAN, 7, 1);
        BlockPos home = h.absolutePos(new BlockPos(1, 2, 1));
        var clock = new com.warfront.war.WarState.Clock();
        clock.home = home;
        clock.pending = com.warfront.war.WarState.Pending.RAID;
        long now = h.getLevel().getGameTime();
        h.assertTrue(com.warfront.war.Recall.refusal(clock, now) == null, "recall should be allowed during a warning");
        Vec3 holderAt = holder.position();
        int n = com.warfront.war.Recall.perform(p, h.getLevel(), home, clock);
        h.assertTrue(p.distanceToSqr(Vec3.atCenterOf(home)) < 9, "the player should land by the standard");
        h.assertTrue(n == 1 && follower.distanceToSqr(Vec3.atCenterOf(home)) < 25, "the following unit should come along, moved " + n);
        h.assertTrue(holder.position().distanceToSqr(holderAt) < 1, "units holding a position stay");
        h.assertTrue(com.warfront.war.Recall.refusal(clock, now) != null, "only one recall per attack");
        h.succeed();
    }

    // ------------------------------------------------------------------ daily upkeep

    @GameTest(template = ARENA)
    public static void messHallFeedsTroopsWhileItHasFood(GameTestHelper h) {
        Player owner = tester(h);
        BlockPos hallPos = new BlockPos(1, 2, 1);
        h.setBlock(hallPos, WFRegistry.MESS_HALL.get());
        var hall = (com.warfront.upkeep.MessHallBlockEntity) h.getBlockEntity(hallPos);
        hall.setOwner(owner.getUUID());
        hall.setItem(0, new ItemStack(Items.BREAD, 1));   // 5 food points: enough for one unit
        SoldierEntity first = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 4, 4);
        SoldierEntity second = recruit(h, owner, SoldierRole.ARCHER, Race.HUMAN, 6, 6);
        var hungry = com.warfront.upkeep.Upkeep.feed(hall, 1000L);
        boolean a = first.hasEffect(WFRegistry.WELL_FED), b = second.hasEffect(WFRegistry.WELL_FED);
        h.assertTrue(a != b, "one loaf feeds exactly one unit (fed: " + a + ", " + b + ")");
        h.assertTrue(hungry.contains(owner.getUUID()), "the unit left hungry should be reported");
        h.assertTrue(hall.foodPoints() == 0, "the loaf should be eaten");
        h.assertTrue(!hall.canPlaceItem(0, new ItemStack(Items.STONE)), "a Mess Hall only takes food");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void farmerDeliversFoodToTheMessHall(GameTestHelper h) {
        Player owner = tester(h);
        BlockPos hallPos = new BlockPos(2, 2, 7);
        h.setBlock(hallPos, WFRegistry.MESS_HALL.get());
        var hall = (com.warfront.upkeep.MessHallBlockEntity) h.getBlockEntity(hallPos);
        hall.setOwner(owner.getUUID());
        SoldierEntity farmer = posted(h, owner, SoldierRole.FARMER, Race.HUMAN, 4, 4);
        farmer.getWorkItems().addItem(new ItemStack(Items.BREAD, 40));
        h.succeedWhen(() -> h.assertTrue(hall.foodPoints() > 0, "the farmer should carry food to the Mess Hall"));
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void buildersRepairRaidDamageFirst(GameTestHelper h) {
        Player owner = tester(h);
        BlockPos near = new BlockPos(4, 2, 3), far = new BlockPos(7, 2, 7);
        h.setBlock(near, Blocks.STONE_BRICKS);
        h.setBlock(far, Blocks.STONE_BRICKS);
        SoldierEntity builder = posted(h, owner, SoldierRole.BUILDER, Race.DWARF, 3, 3);
        builder.setBlueprint(com.warfront.entity.work.Blueprint.survey(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 4)), 4, 1, 3));
        builder.getWorkItems().addItem(new ItemStack(Items.STONE_BRICKS, 1));   // one brick: it must choose
        h.setBlock(near, Blocks.AIR);
        com.warfront.upkeep.RaidDamage.log(h.getLevel(), h.absolutePos(far));
        h.setBlock(far, Blocks.AIR);
        h.succeedWhen(() -> {
            h.assertTrue(h.getBlockState(far).is(Blocks.STONE_BRICKS), "the raid-broken block should be rebuilt");
            h.assertTrue(h.getBlockState(near).isAir(), "raid damage comes before other repairs");
        });
    }

    // ------------------------------------------------------------------ veterancy and fallen heroes

    @GameTest(template = ARENA)
    public static void xpRaisesRankHealthAndDamage(GameTestHelper h) {
        Player owner = tester(h);
        SoldierEntity s = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 4, 4);
        float hp = s.getMaxHealth();
        double dmg = s.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        s.addXp(com.warfront.army.Veterancy.threshold(2) - 1);
        h.assertTrue(s.getRank() == 1, "just short of Veteran should be Regular, was " + s.getRank());
        s.addXp(1);
        h.assertTrue(s.getRank() == 2, "reaching the threshold should make a Veteran, was " + s.getRank());
        double bonus = 1 + com.warfront.army.Veterancy.bonus(2);
        h.assertTrue(Math.abs(s.getMaxHealth() - hp * bonus) < 0.6, "Veteran health should be x" + bonus + ": " + hp + " -> " + s.getMaxHealth());
        double dmg2 = s.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        h.assertTrue(dmg2 > dmg, "Veteran damage should rise: " + dmg + " -> " + dmg2);
        h.assertTrue(com.warfront.army.Veterancy.neverRouts(3) && !com.warfront.army.Veterancy.neverRouts(2), "Elite and Legend never rout");
        SoldierEntity farmer = posted(h, owner, SoldierRole.FARMER, Race.HUMAN, 2, 2);
        farmer.addXp(5000);
        h.assertTrue(farmer.getRank() == 0, "workers don't rank");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void heroesFallAndAreRevivedButFootmenDie(GameTestHelper h) {
        Player owner = tester(h);
        SoldierEntity champion = recruit(h, owner, SoldierRole.CHAMPION, Race.HUMAN, 3, 4);
        SoldierEntity footman = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 6, 4);
        champion.hurt(h.getLevel().damageSources().generic(), 1000F);
        h.assertTrue(champion.isAlive() && champion.isFallen() && champion.getHealth() == 1F,
                "a Champion at lethal damage should fall, not die (alive " + champion.isAlive() + ", fallen " + champion.isFallen() + ")");
        h.assertTrue(!champion.hurt(h.getLevel().damageSources().generic(), 5F), "a fallen hero can't be hurt");
        champion.revive();
        h.assertTrue(!champion.isFallen() && Math.abs(champion.getHealth() - champion.getMaxHealth() * 0.5F) < 0.01F,
                "a revived hero gets up at half health, has " + champion.getHealth());
        footman.hurt(h.getLevel().damageSources().generic(), 1000F);
        h.assertTrue(!footman.isAlive(), "rank-and-file troops still die");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void unrevivedHeroReturnsAtHalfCostWithItsRank(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        owner.moveTo(h.absoluteVec(new Vec3(4.5, 2, 7.5)));
        SummoningAltarBlockEntity altar = altar(h, new BlockPos(4, 2, 4), owner, true);
        ManaWellBlockEntity well = well(h, new BlockPos(8, 1, 8), owner, 0F);
        SoldierEntity captain = recruit(h, owner, SoldierRole.CAPTAIN, Race.HUMAN, 1, 7);
        captain.setXp(com.warfront.army.Veterancy.threshold(3));
        captain.hurt(h.getLevel().damageSources().generic(), 1000F);
        captain.giveUpWaiting();
        var war = com.warfront.war.WarState.get(h.getLevel().getServer());
        var list = war.returning(owner.getUUID());
        h.assertTrue(list.size() == 1 && list.get(0).role() == SoldierRole.CAPTAIN, "the captain should be on the returning list");
        var hero = list.get(0);
        war.takeReturning(owner.getUUID(), 0);
        war.addReturning(owner.getUUID(), new com.warfront.war.WarState.Returning(hero.role(), hero.race(), hero.xp(), 0));   // ready now
        int half = SummoningAltarBlockEntity.returnCost(hero);
        h.assertTrue(half == SoldierRole.CAPTAIN.manaCost(Race.HUMAN) / 2, "a returning hero costs half");
        h.runAfterDelay(2, () -> {
            well.setMana(100F);
            var r = altar.summonReturning(owner, 0);
            h.assertTrue(r.ok(), "the returning hero should be summoned: " + r.message());
            h.assertTrue(Math.abs(well.getMana() - (100F - half)) < 0.01F, "it should cost " + half + ", well has " + well.getMana());
            SoldierEntity back = h.getLevel().getEntitiesOfClass(SoldierEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(4, 3, 4))).inflate(2),
                    e -> e.isOwnedBy(owner) && e.getRole() == SoldierRole.CAPTAIN).stream().findFirst().orElse(null);
            h.assertTrue(back != null && back.getRank() == 3, "it should come back an Elite, rank " + (back == null ? "none" : back.getRank()));
            h.assertTrue(war.returning(owner.getUUID()).isEmpty(), "the returning list should be empty");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ bounties and camps

    @GameTest(template = ARENA)
    public static void takenCampShrinksItsRaid(GameTestHelper h) {
        var clock = new com.warfront.war.WarState.Clock();
        var preset = com.warfront.war.WarState.Preset.NORMAL;
        int full = com.warfront.war.RaidScheduler.raidRoles(clock, NpcFaction.MARAUDERS, net.minecraft.util.RandomSource.create(3), 3, preset).size();
        clock.shrinkFaction = NpcFaction.MARAUDERS.name();
        int other = com.warfront.war.RaidScheduler.raidRoles(clock, NpcFaction.BLACK_LEGION, net.minecraft.util.RandomSource.create(3), 3, preset).size();
        h.assertTrue(!clock.shrinkFaction.isEmpty(), "another faction's raid doesn't use up the shrink");
        int shrunk = com.warfront.war.RaidScheduler.raidRoles(clock, NpcFaction.MARAUDERS, net.minecraft.util.RandomSource.create(3), 3, preset).size();
        h.assertTrue(shrunk == Math.max(1, Math.round(full * 2F / 3F)), "the raid should come a third smaller: " + full + " -> " + shrunk);
        h.assertTrue(clock.shrinkFaction.isEmpty(), "the shrink applies once");
        h.assertTrue(other > 0, "other raids still come");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void deliveryBountyPaysAndBountiesExpire(GameTestHelper h) {
        Player p = tester(h);
        var war = com.warfront.war.WarState.get(h.getLevel().getServer());
        long now = h.getLevel().getServer().overworld().getGameTime();
        var list = war.bounties(p.getUUID());
        var deliver = new com.warfront.war.Bounties.Bounty(UUID.randomUUID(), com.warfront.war.Bounties.Type.DELIVER, BlockPos.ZERO,
                "minecraft:oak_log", 64, null, now + 1000, 9, 1);
        var stale = new com.warfront.war.Bounties.Bounty(UUID.randomUUID(), com.warfront.war.Bounties.Type.SCOUT, BlockPos.ZERO,
                "", 0, null, now - 1, 5, 0);
        list.add(deliver);
        list.add(stale);
        com.warfront.war.Bounties.expire(war, p.getUUID(), now);
        h.assertTrue(!list.contains(stale) && list.contains(deliver), "expired bounties are dropped, live ones kept");
        ItemStack logs = new ItemStack(Items.OAK_LOG, 64);
        p.getInventory().add(logs);
        h.assertTrue(com.warfront.war.Bounties.deliver(p, p.getInventory().getItem(p.getInventory().findSlotMatchingItem(new ItemStack(Items.OAK_LOG)))),
                "handing over 64 logs should fulfil the bounty");
        h.assertTrue(p.getInventory().countItem(Items.OAK_LOG) == 0, "the logs should be handed over");
        h.assertTrue(p.getInventory().countItem(WFRegistry.MANA_SHARD.get()) == 9 && p.getInventory().countItem(WFRegistry.MANA_CRYSTAL.get()) == 1,
                "the bounty should pay 9 shards and a crystal");
        h.assertTrue(war.bounties(p.getUUID()).isEmpty(), "the bounty is done");
        h.succeed();
    }

    // ------------------------------------------------------------------ the merchant

    @GameTest(template = ARENA)
    public static void merchantComesOnScheduleButNotDuringARaid(GameTestHelper h) {
        var c = new com.warfront.war.WarState.Clock();
        long now = 100_000L;
        c.merchantDue = now - 1;
        h.assertTrue(!com.warfront.merchant.Caravan.shouldArrive(c, now), "no base, no merchant");
        c.home = BlockPos.ZERO;
        h.assertTrue(com.warfront.merchant.Caravan.shouldArrive(c, now), "the merchant should come when due");
        c.pending = com.warfront.war.WarState.Pending.RAID;
        h.assertTrue(!com.warfront.merchant.Caravan.shouldArrive(c, now), "not while a raid is coming");
        c.pending = com.warfront.war.WarState.Pending.NONE;
        c.activeUntil = now + 100;
        h.assertTrue(!com.warfront.merchant.Caravan.shouldArrive(c, now), "not while a raid is underway");
        c.activeUntil = 0;
        c.merchantDue = now + 10;
        h.assertTrue(!com.warfront.merchant.Caravan.shouldArrive(c, now), "not before he's due");
        long next = com.warfront.merchant.Caravan.nextVisit(now, net.minecraft.util.RandomSource.create(1));
        h.assertTrue(next >= now + 3 * 24000L && next <= now + 5 * 24000L, "visits are 3 to 5 days apart");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void merchantTradesForCrystals(GameTestHelper h) {
        var offers = com.warfront.merchant.MerchantEntity.drawOffers(net.minecraft.util.RandomSource.create(5), 4);
        var buy = offers.stream().filter(o -> o.getResult().is(Items.DIAMOND)).findFirst().orElseThrow();
        ItemStack crystals = new ItemStack(WFRegistry.MANA_CRYSTAL.get(), 10);
        h.assertTrue(buy.satisfiedBy(crystals, ItemStack.EMPTY), "crystals should pay for the diamond trade");
        h.assertTrue(buy.take(crystals, ItemStack.EMPTY), "the trade should go through");
        h.assertTrue(crystals.getCount() == 6, "four crystals should be paid, " + crystals.getCount() + " left");
        h.assertTrue(buy.getResult().getCount() == 3, "a level 4 base gets 3 diamonds per trade");
        h.assertTrue(offers.stream().anyMatch(o -> o.getCostA().is(WFRegistry.WAR_MARK.get())), "he buys War Marks");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void captainsAndChampionsStandTaller(GameTestHelper h) {
        Player owner = tester(h);
        for (Race race : new Race[]{Race.HUMAN, Race.ORC, Race.DWARF}) {
            SoldierEntity soldier = recruit(h, owner, SoldierRole.SWORDSMAN, race, 2, 2);
            SoldierEntity captain = recruit(h, owner, SoldierRole.CAPTAIN, race, 4, 2);
            SoldierEntity champion = recruit(h, owner, SoldierRole.CHAMPION, race, 6, 2);
            float c = captain.getScale() / soldier.getScale(), ch = champion.getScale() / soldier.getScale();
            h.assertTrue(Math.abs(c - 1.15F) < 0.01F, race.id() + " Captain should be 1.15x a soldier, was " + c);
            h.assertTrue(Math.abs(ch - 1.30F) < 0.01F, race.id() + " Champion should be 1.30x a soldier, was " + ch);
            soldier.discard();
            captain.discard();
            champion.discard();
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ alerts

    @GameTest(template = ARENA)
    public static void alertsGoOutForRankUpsFallenHeroesAndWaves(GameTestHelper h) {
        List<com.warfront.alert.Alerts.Sent> sent = new ArrayList<>();
        com.warfront.alert.Alerts.capture = sent;
        try {
            Player owner = tester(h);
            SoldierEntity s = recruit(h, owner, SoldierRole.SWORDSMAN, Race.ELF, 2, 2);
            s.addXp(com.warfront.army.Veterancy.threshold(1));
            SoldierEntity hero = recruit(h, owner, SoldierRole.CAPTAIN, Race.ELF, 6, 2);
            hero.hurt(h.getLevel().damageSources().generic(), 1000F);
            BlockPos pos = new BlockPos(4, 2, 6);
            h.setBlock(pos, WFRegistry.WAR_STANDARD.get());
            WarStandardBlockEntity standard = (WarStandardBlockEntity) h.getBlockEntity(pos);
            standard.setOwner(owner.getUUID());
            standard.startSiege(h.getLevel());
            UUID warband = standard.getWarbandId();
            standard.endSiege(h.getLevel());
            h.assertTrue(sent.stream().anyMatch(a -> a.key().equals("rank_up") && a.player().equals(owner.getUUID())), "a rank-up toast for the owner");
            h.assertTrue(sent.stream().anyMatch(a -> a.key().equals("hero_fallen") && a.player().equals(owner.getUUID())), "a hero-fallen toast for the owner");
            h.assertTrue(sent.stream().anyMatch(a -> a.key().equals("wave_start") && a.kind() == com.warfront.alert.Alerts.Kind.BANNER),
                    "a wave-start banner (sent " + sent.stream().map(com.warfront.alert.Alerts.Sent::key).toList() + ", warband " + warband + ")");
        } finally {
            com.warfront.alert.Alerts.capture = null;
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ siege outposts

    @GameTest(template = ARENA)
    public static void outpostsComeEveryFifthWave(GameTestHelper h) {
        h.assertTrue(com.warfront.outpost.Outposts.shouldRaise(5) && com.warfront.outpost.Outposts.shouldRaise(10),
                "waves 5 and 10 raise outposts");
        h.assertTrue(!com.warfront.outpost.Outposts.shouldRaise(4) && !com.warfront.outpost.Outposts.shouldRaise(6),
                "waves 4 and 6 don't");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void outpostHoldsTheWaveAndComesDownClean(GameTestHelper h) {
        Player p = tester(h);
        BlockPos standardPos = new BlockPos(4, 2, 4), chestPos = new BlockPos(7, 2, 7);
        BlockPos wallA = new BlockPos(1, 2, 1), wallB = new BlockPos(1, 3, 1);
        h.setBlock(standardPos, WFRegistry.WAR_STANDARD.get());
        h.setBlock(wallA, Blocks.SPRUCE_LOG);
        h.setBlock(wallB, Blocks.SPRUCE_LOG);
        h.setBlock(chestPos, WFRegistry.RAID_CHEST.get());
        var standard = (WarStandardBlockEntity) h.getBlockEntity(standardPos);
        var chest = (com.warfront.outpost.RaidChestBlockEntity) h.getBlockEntity(chestPos);
        chest.link(h.absolutePos(standardPos), UUID.randomUUID(), List.of(h.absolutePos(wallA), h.absolutePos(wallB), h.absolutePos(chestPos)));
        com.warfront.outpost.Outposts.fill(chest, NpcFaction.IRONBEARD_CLAN, 5, h.getLevel().random);
        standard.setOutpostChest(h.absolutePos(chestPos));
        h.assertTrue(standard.hasOutpost(h.getLevel()), "the wave can't be won while the outpost stands");
        var state = h.getBlockState(chestPos);
        h.assertTrue(state.getDestroyProgress(p, h.getLevel(), h.absolutePos(chestPos)) == 0F, "a full raid chest can't be broken");
        chest.clearContent();
        h.assertTrue(state.getDestroyProgress(p, h.getLevel(), h.absolutePos(chestPos)) > 0F, "an empty raid chest can be broken");
        h.getLevel().destroyBlock(h.absolutePos(chestPos), false);
        h.assertTrue(h.getBlockState(wallA).isAir() && h.getBlockState(wallB).isAir(), "every outpost block should be removed");
        h.assertTrue(!standard.hasOutpost(h.getLevel()), "with the outpost gone the wave can be won");
        h.succeed();
    }

    // ------------------------------------------------------------------ campaign

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void beatenRaidsFillTheMeterAndTheMapNeedsBaseLevel3(GameTestHelper h) {
        Player p = tester(h);
        NpcFaction f = NpcFaction.MARAUDERS;
        com.warfront.war.Campaign.raidBeaten(p, f, null);
        h.assertTrue(com.warfront.war.Campaign.meter(p, f) == 1, "beating a raid raises its faction's meter");
        int full = com.warfront.config.WFConfig.WAR_MAP_RAIDS.get();
        com.warfront.war.Campaign.setMeter(p, f, full - 1);
        SoldierEntity last = raider(h, SoldierRole.SWORDSMAN, 6, 6);
        h.assertTrue(!com.warfront.war.Campaign.raidBeaten(p, f, last), "no map before the meter is full");
        h.assertTrue(!com.warfront.war.Campaign.raidBeaten(p, f, last), "no map below base level 3");
        BlockPos wellPos = new BlockPos(2, 2, 2);
        h.setBlock(wellPos, WFRegistry.MANA_WELL.get());
        var well = (ManaWellBlockEntity) h.getBlockEntity(wellPos);
        well.setOwner(p.getUUID());
        h.runAfterDelay(2, () -> {
            well.setTestLevel(3);
            boolean dropped = com.warfront.war.Campaign.raidBeaten(p, f, last);
            h.assertTrue(dropped, "with a full meter at base level 3 the last raider drops the War Map");
            boolean onGround = !h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, last.getBoundingBox().inflate(3),
                    e -> e.getItem().is(WFRegistry.WAR_MAP.get()) && com.warfront.war.Campaign.mapFaction(e.getItem()) == f).isEmpty();
            h.assertTrue(onGround, "the War Map should lie by the last raider");
            last.discard();
            h.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void renegadesWaitForTheOtherSixWarlords(GameTestHelper h) {
        Player p = tester(h);
        p.setData(WFRegistry.RACE, Race.HUMAN.id());
        NpcFaction own = com.warfront.war.Campaign.renegades(Race.HUMAN);
        h.assertTrue(own == NpcFaction.BLACK_LEGION, "human renegades are the Black Legion");
        var clock = new com.warfront.war.WarState.Clock();
        var rng = net.minecraft.util.RandomSource.create(9);
        for (int i = 0; i < 200; i++) {
            h.assertTrue(com.warfront.war.Campaign.pick(p, clock, rng) != own, "renegades raided before the six warlords fell");
        }
        int beaten = 0;
        for (NpcFaction f : NpcFaction.values()) {
            if (f == own) continue;
            if (++beaten == 6) h.assertTrue(!com.warfront.war.Campaign.mayRaid(p, own), "five warlords aren't enough");
            com.warfront.war.Campaign.setWarlordBeaten(p, f, true);
        }
        h.assertTrue(com.warfront.war.Campaign.mayRaid(p, own), "after six warlords the renegades come");
        int inARow = 0, maxRow = 0;
        NpcFaction prev = null;
        for (int i = 0; i < 300; i++) {
            NpcFaction f = com.warfront.war.Campaign.pick(p, clock, rng);
            inARow = f == prev ? inARow + 1 : 1;
            maxRow = Math.max(maxRow, inARow);
            prev = f;
        }
        h.assertTrue(maxRow <= 3, "no faction raids more than three times in a row, saw " + maxRow);
        h.succeed();
    }

    // ------------------------------------------------------------------ fortresses and warlords

    @GameTest(template = ARENA)
    public static void fortressTemplatesLoadAndTheGarrisonIsHostile(GameTestHelper h) {
        for (NpcFaction f : NpcFaction.values()) {
            var t = h.getLevel().getStructureManager().get(Warfront.id("fortress_" + f.name().toLowerCase(java.util.Locale.ROOT)));
            h.assertTrue(t.isPresent() && t.get().getSize().getX() == 33, "the " + f.displayName + " fortress template should load");
        }
        Player p = tester(h);
        BlockPos pos = new BlockPos(4, 2, 4);
        h.setBlock(pos, WFRegistry.FORTRESS_CORE.get().defaultBlockState().setValue(com.warfront.fortress.FortressCoreBlock.FACTION, 1));
        var core = (com.warfront.fortress.FortressCoreBlockEntity) h.getBlockEntity(pos);
        core.activate(h.getLevel());
        var garrison = core.garrison(h.getLevel());
        h.assertTrue(garrison.size() >= 8, "the garrison should take its posts, found " + garrison.size());
        h.assertTrue(garrison.stream().allMatch(s -> com.warfront.faction.Factions.relation(h.getLevel().getServer(), s.getFactionKey(),
                "p:" + p.getUUID()) == com.warfront.faction.Relation.ENEMY), "defenders are hostile to players");
        h.assertTrue(core.getWarlord() != null && h.getLevel().getEntity(core.getWarlord()) instanceof SoldierEntity w && w.isWarlord(),
                "the warlord rises from the seat");
        core.reset(h.getLevel());
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void beatenWarlordsDropRewardsAndSevenMakeAWarlord(GameTestHelper h) {
        Player p = tester(h);
        BlockPos pos = new BlockPos(4, 2, 4);
        h.setBlock(pos, WFRegistry.FORTRESS_CORE.get().defaultBlockState().setValue(com.warfront.fortress.FortressCoreBlock.FACTION, 0));
        var core = (com.warfront.fortress.FortressCoreBlockEntity) h.getBlockEntity(pos);
        core.activate(h.getLevel());
        SoldierEntity w = (SoldierEntity) h.getLevel().getEntity(core.getWarlord());
        w.hurt(h.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        h.assertTrue(core.isDefeated(), "the warlord's death ends the fortress");
        var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(WFRegistry.TROPHY_BANNER_ITEM.get())), "the trophy banner drops");
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(WFRegistry.SKULLSPLITTER.get())), "the Marauder warlord drops Skullsplitter");
        drops.forEach(Entity::discard);
        h.assertTrue(core.garrison(h.getLevel()).isEmpty(), "the garrison routs");
        for (NpcFaction f : NpcFaction.values()) com.warfront.fortress.Warlords.beaten(p, f);
        h.assertTrue(com.warfront.war.Campaign.warlordsBeaten(p) == 7 && p.getData(WFRegistry.WARLORD_TITLE),
                "seven warlords make a Warlord");
        h.assertTrue(p.getInventory().countItem(WFRegistry.SEAL_OF_SEVEN.get()) == 1, "the Seal of the Seven is the clue to the last one");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void trophyBannerAuraReachesUnitsNearTheStandard(GameTestHelper h) {
        Player p = tester(h);
        BlockPos standardPos = new BlockPos(4, 2, 4), bannerPos = new BlockPos(2, 2, 2);
        h.setBlock(standardPos, WFRegistry.WAR_STANDARD.get());
        var standard = (WarStandardBlockEntity) h.getBlockEntity(standardPos);
        standard.setOwner(p.getUUID());
        h.setBlock(bannerPos, WFRegistry.TROPHY_BANNER.get());
        SoldierEntity s = recruit(h, p, SoldierRole.SWORDSMAN, Race.HUMAN, 6, 6);
        h.assertTrue(com.warfront.fortress.TrophyBanners.standardNear(h.getLevel(), h.absolutePos(bannerPos)) != null,
                "the banner should find the War Standard within 8 blocks");
        com.warfront.fortress.TrophyBanners.apply(h.getLevel(), h.absolutePos(bannerPos), NpcFaction.MARAUDERS,
                standard.factionKey(h.getLevel().getServer()));
        h.assertTrue(s.hasEffect(WFRegistry.TROPHY_AURAS.get(NpcFaction.MARAUDERS)), "your units near the standard get the aura");
        h.succeed();
    }
}
