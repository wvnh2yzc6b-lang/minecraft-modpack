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
}
