package com.warfront.world;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Spawns NPC warbands: roaming raiding parties and siege armies. */
public final class WarbandSpawner {
    private WarbandSpawner() {}

    /** Army composition for a siege wave; grows with every wave survived. */
    public static List<SoldierRole> siegeComposition(int wave) {
        List<SoldierRole> roles = new ArrayList<>();
        add(roles, SoldierRole.SHIELDBEARER, 2 + wave / 2);
        add(roles, SoldierRole.SPEARMAN, 1 + wave / 3);
        add(roles, SoldierRole.SWORDSMAN, 1 + wave / 2);
        add(roles, SoldierRole.ARCHER, 1 + wave / 2);
        add(roles, SoldierRole.HEALER, wave >= 3 ? 1 + wave / 5 : 0);
        add(roles, SoldierRole.CAPTAIN, wave >= 2 ? 1 + wave / 6 : 0);
        return roles.size() > 32 ? roles.subList(0, 32) : roles;
    }

    /** A small raiding party. */
    public static List<SoldierRole> raidComposition(RandomSource random) {
        List<SoldierRole> roles = new ArrayList<>();
        int size = 4 + random.nextInt(4);
        add(roles, SoldierRole.SHIELDBEARER, 1 + random.nextInt(2));
        add(roles, SoldierRole.ARCHER, 1 + random.nextInt(2));
        if (random.nextBoolean()) roles.add(SoldierRole.CAPTAIN);
        SoldierRole[] fill = {SoldierRole.SWORDSMAN, SoldierRole.SPEARMAN, SoldierRole.SWORDSMAN, SoldierRole.HEALER};
        while (roles.size() < size) roles.add(fill[random.nextInt(fill.length)]);
        return roles;
    }

    private static void add(List<SoldierRole> list, SoldierRole role, int n) {
        for (int i = 0; i < n; i++) list.add(role);
    }

    /** Spawns the warband around {@code center} and returns its id. */
    public static UUID spawn(ServerLevel level, NpcFaction faction, List<SoldierRole> roles, BlockPos center,
                             @Nullable Vec3 objective, @Nullable BlockPos siegeTarget, int tier) {
        UUID warband = UUID.randomUUID();
        float yaw = objective == null ? 0F
                : (float) (Mth.atan2(objective.z - center.getZ(), objective.x - center.getX()) * Mth.RAD_TO_DEG) - 90F;
        for (SoldierRole role : roles) {
            SoldierEntity soldier = WFRegistry.SOLDIER.get().create(level);
            if (soldier == null) continue;
            int x = center.getX() + level.random.nextInt(7) - 3;
            int z = center.getZ() + level.random.nextInt(7) - 3;
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            soldier.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, yaw, 0F);
            soldier.setupAsRaider(faction, role, warband, objective, siegeTarget, tier);
            level.addFreshEntity(soldier);
        }
        return warband;
    }
}
