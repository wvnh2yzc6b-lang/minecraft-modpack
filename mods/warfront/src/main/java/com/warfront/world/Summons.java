package com.warfront.world;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Race;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Units summoned by towers (Brood Nest swarmlings, a treant, Hellgate imps). They fight for the tower's owner, never
 * count toward the army caps, drop nothing, and vanish when their time runs out or their siege wave ends.
 */
public final class Summons {
    private static final String EXPIRES = "warfront_expires";
    private static final String WAVE = "warfront_wave_summon";
    private static final String SOURCE = "warfront_summon_source";

    private Summons() {}

    public static boolean isSummon(SoldierEntity s) {
        return s.getPersistentData().contains(EXPIRES) || s.getPersistentData().contains(WAVE);
    }

    /**
     * Summons a unit for {@code owner} at {@code at}. With {@code ticks} > 0 it vanishes after that long; with
     * {@code waveStandard} set it also vanishes when that War Standard's wave ends.
     */
    @Nullable
    public static SoldierEntity summon(ServerLevel level, UUID owner, Race race, SoldierRole role, Vec3 at, int ticks,
                                       @Nullable BlockPos waveStandard, BlockPos source) {
        SoldierEntity s = WFRegistry.SOLDIER.get().create(level);
        if (s == null) return null;
        s.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360F, 0F);
        s.setupAsSummon(owner, role, race);
        if (ticks > 0) s.getPersistentData().putLong(EXPIRES, level.getGameTime() + ticks);
        if (waveStandard != null) s.getPersistentData().putLong(WAVE, waveStandard.asLong());
        s.getPersistentData().putLong(SOURCE, source.asLong());
        level.addFreshEntity(s);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.5, at.z, 10, 0.3, 0.4, 0.3, 0.02);
        return s;
    }

    /** Living summons from this source block. */
    public static int countFrom(ServerLevel level, BlockPos source, double radius) {
        long key = source.asLong();
        return level.getEntitiesOfClass(SoldierEntity.class, new net.minecraft.world.phys.AABB(source).inflate(radius),
                s -> s.isAlive() && s.getPersistentData().getLong(SOURCE) == key && isSummon(s)).size();
    }

    /** Every second: summons whose time is up vanish. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            long now = level.getGameTime();
            for (SoldierEntity s : level.getEntities(WFRegistry.SOLDIER.get(), s -> s.getPersistentData().contains(EXPIRES))) {
                if (now >= s.getPersistentData().getLong(EXPIRES)) vanish(level, s);
            }
        }
    }

    /** A War Standard's wave ended: its wave summons vanish. */
    public static int waveEnded(ServerLevel level, BlockPos standard) {
        long key = standard.asLong();
        int n = 0;
        for (SoldierEntity s : level.getEntities(WFRegistry.SOLDIER.get(), s -> s.getPersistentData().contains(WAVE)
                && s.getPersistentData().getLong(WAVE) == key)) {
            vanish(level, s);
            n++;
        }
        return n;
    }

    public static void vanish(ServerLevel level, SoldierEntity s) {
        level.sendParticles(ParticleTypes.POOF, s.getX(), s.getY() + 0.5, s.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
        s.discard();
    }
}
