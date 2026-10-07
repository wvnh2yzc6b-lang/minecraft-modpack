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
    private static final int MAX_WAVE_SIZE = 40;

    /** How far from its objective a tunneling Swarm brood breaks the surface. */
    private static final double TUNNEL_DISTANCE = 14.0;

    private WarbandSpawner() {}

    /** Dirt and stone burst up where a tunneling soldier breaks through. */
    private static void emerge(ServerLevel level, BlockPos at) {
        net.minecraft.world.level.block.state.BlockState ground = level.getBlockState(at.below());
        if (ground.isAir()) ground = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                        net.minecraft.core.particles.ParticleTypes.BLOCK, ground),
                at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, 40, 0.5, 0.4, 0.5, 0.15);
        level.playSound(null, at, net.minecraft.sounds.SoundEvents.GRAVEL_BREAK, net.minecraft.sounds.SoundSource.HOSTILE,
                1.2F, 0.6F);
    }

    /** Army composition for a siege wave; grows with every wave survived. */
    public static List<SoldierRole> siegeComposition(int wave, NpcFaction faction) {
        List<SoldierRole> roles = new ArrayList<>();
        double m = faction.sizeMultiplier;
        add(roles, SoldierRole.SHIELDBEARER, (int) Math.round((2 + wave / 2) * m));
        add(roles, SoldierRole.SPEARMAN, (int) Math.round((1 + wave / 3) * m));
        add(roles, SoldierRole.SWORDSMAN, (int) Math.round((1 + wave / 2) * m));
        add(roles, SoldierRole.ARCHER, (int) Math.round((1 + wave / 2) * m));
        add(roles, SoldierRole.HEALER, wave >= 3 ? 1 + wave / 5 : 0);
        add(roles, SoldierRole.CAPTAIN, wave >= 2 || wave % 5 == 0 ? 1 + wave / 6 : 0);
        add(roles, SoldierRole.CHAMPION, wave >= 4 ? 1 + wave / 8 : 0);
        return roles.size() > MAX_WAVE_SIZE ? new ArrayList<>(roles.subList(0, MAX_WAVE_SIZE)) : roles;
    }

    /** A small raiding party. */
    public static List<SoldierRole> raidComposition(RandomSource random, NpcFaction faction) {
        List<SoldierRole> roles = new ArrayList<>();
        int size = (int) Math.round((4 + random.nextInt(4)) * faction.sizeMultiplier);
        add(roles, SoldierRole.SHIELDBEARER, 1 + random.nextInt(2));
        add(roles, SoldierRole.ARCHER, 1 + random.nextInt(2));
        if (random.nextBoolean()) roles.add(SoldierRole.CAPTAIN);
        if (random.nextFloat() < 0.15F) roles.add(SoldierRole.CHAMPION);
        SoldierRole[] fill = {SoldierRole.SWORDSMAN, SoldierRole.SPEARMAN, SoldierRole.SWORDSMAN, SoldierRole.HEALER};
        while (roles.size() < size) roles.add(fill[random.nextInt(fill.length)]);
        return roles;
    }

    private static void add(List<SoldierRole> list, SoldierRole role, int n) {
        for (int i = 0; i < n; i++) list.add(role);
    }

    /** Spawns a new warband around {@code center} and returns its id. */
    public static UUID spawn(ServerLevel level, NpcFaction faction, List<SoldierRole> roles, BlockPos center,
                             @Nullable Vec3 objective, @Nullable BlockPos siegeTarget, int tier) {
        UUID warband = UUID.randomUUID();
        spawnInto(level, warband, faction, roles, center, objective, siegeTarget, tier);
        return warband;
    }

    /** Spawns soldiers into an existing warband (sieges split one warband across several fronts). */
    public static List<SoldierEntity> spawnInto(ServerLevel level, UUID warband, NpcFaction faction,
                                                List<SoldierRole> roles, BlockPos center, @Nullable Vec3 objective,
                                                @Nullable BlockPos siegeTarget, int tier) {
        List<SoldierEntity> out = new ArrayList<>();
        boolean tunnel = faction == NpcFaction.THE_SWARM && objective != null;
        if (tunnel) {
            // The Swarm tunnels in: it breaks the surface close to its objective instead of marching from afar.
            double dx = center.getX() - objective.x, dz = center.getZ() - objective.z;
            double len = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
            double near = Math.min(len, TUNNEL_DISTANCE);
            center = BlockPos.containing(objective.x + dx / len * near, center.getY(), objective.z + dz / len * near);
        }
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
            if (tunnel) emerge(level, ground);
            out.add(soldier);
        }
        return out;
    }
}
