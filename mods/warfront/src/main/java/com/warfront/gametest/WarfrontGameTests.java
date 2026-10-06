package com.warfront.gametest;

import com.warfront.Warfront;
import com.warfront.army.Formation;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.mana.Mana;
import com.warfront.registry.WFRegistry;
import net.minecraft.world.InteractionHand;
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

    @GameTest(template = ARENA)
    public static void summoningCostsMana(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        ItemStack contract = new ItemStack(WFRegistry.CONTRACTS.get(SoldierRole.SWORDSMAN).get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, contract);
        int cost = SoldierRole.SWORDSMAN.manaCost(Race.HUMAN);

        Mana.set(player, 0F);
        contract.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().getCount() == 2, "summoning without mana should fail");

        Mana.set(player, 100F);
        contract.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().getCount() == 1, "contract should be consumed");
        h.assertTrue(Math.abs(Mana.get(player) - (100F - cost)) < 0.01F,
                "expected " + (100 - cost) + " mana left, had " + Mana.get(player));
        h.assertTrue(SoldierRole.SWORDSMAN.manaCost(Race.HIVE) < cost, "hive troops should be cheaper");
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
        h.assertTrue(!attackers.isEmpty(), "the wave should have attackers");
        // Clean up so the attackers don't wander into other tests.
        standard.stopCampaign(h.getLevel());
        attackers.forEach(SoldierEntity::discard);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void demonFootSoldiersAreWingedImps(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity imp = recruit(h, owner, SoldierRole.SWORDSMAN, Race.DEMON, 3, 4);
        SoldierEntity archfiend = recruit(h, owner, SoldierRole.CAPTAIN, Race.DEMON, 6, 4);
        h.assertTrue(imp.getBody() == com.warfront.army.UnitBody.IMP, "demon swordsmen should be imps");
        h.assertTrue("Imp".equals(imp.getUnitName()), "unit name should be Imp, was " + imp.getUnitName());
        h.assertTrue(imp.getScale() < 0.8F, "imps should be small, scale was " + imp.getScale());
        h.assertTrue(archfiend.getBody() == com.warfront.army.UnitBody.HUMANOID, "archfiends are not imps");
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
}
