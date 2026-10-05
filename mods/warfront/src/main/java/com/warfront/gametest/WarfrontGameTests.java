package com.warfront.gametest;

import com.warfront.Warfront;
import com.warfront.army.Formation;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.registry.WFRegistry;
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
        BlockPos abs = h.absolutePos(new BlockPos(x, 1, z));
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
        Vec3 anchor = h.absoluteVec(new Vec3(4.5, 1, 6.5));
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
                recruit.getHealth() < recruit.getMaxHealth() || raider.getHealth() < raider.getMaxHealth(),
                "the two sides never came to blows"));
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void archerShootsEnemy(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity archer = recruit(h, owner, SoldierRole.ARCHER, Race.ELF, 1, 1);
        Husk husk = h.spawn(EntityType.HUSK, new BlockPos(7, 1, 7));
        husk.setPersistenceRequired();
        h.succeedWhen(() -> h.assertTrue(husk.getLastHurtByMob() == archer || !husk.isAlive(),
                "archer never hit the husk"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void healerMendsWoundedAlly(GameTestHelper h) {
        Player owner = h.makeMockPlayer(GameType.SURVIVAL);
        SoldierEntity healer = recruit(h, owner, SoldierRole.HEALER, Race.HUMAN, 3, 4);
        SoldierEntity wounded = recruit(h, owner, SoldierRole.SWORDSMAN, Race.HUMAN, 6, 4);
        wounded.setHealth(8.0F);
        h.succeedWhen(() -> h.assertTrue(wounded.getHealth() > 8.0F, "healer never healed the wounded soldier"));
    }
}
