package com.warfront.finale;

import com.warfront.Warfront;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.NpcFaction;
import com.warfront.registry.WFRegistry;
import com.warfront.test.TestActions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Frozen Field: the battlefield of the two brothers' first war, sealed out of time under a still, colorless sky.
 * Armies of all seven races stand there as stone statues, mid-fight, around the hidden warlord's keep. Reached by
 * using the Sealed Map on a War Standard; your army marches through with you. Leaving (the Sealed Map again) or
 * falling returns you home.
 */
public final class FrozenField {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, Warfront.id("frozen_field"));
    /** The snow surface: blocks stand on y=43, so feet are at y=44. */
    public static final int GROUND = 44;
    public static final BlockPos THRONE = new BlockPos(0, GROUND, 0);
    public static final BlockPos ARRIVAL = new BlockPos(0, GROUND, 44);
    public static final int KEEP = 10;
    private static final String RETURN_POS = "warfront_field_return";
    private static final String RETURN_DIM = "warfront_field_return_dim";

    private FrozenField() {}

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(KEY);
    }

    public static boolean isField(Level level) {
        return level.dimension() == KEY;
    }

    /** The statues: fourteen soldiers of the seven races, frozen in a ring around the keep. */
    public static List<BlockPos> statues() {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            double a = i * Math.PI * 2 / 14 + 0.2;
            double r = 20 + (i % 3) * 4;
            out.add(new BlockPos((int) Math.round(Math.cos(a) * r), GROUND, (int) Math.round(Math.sin(a) * r)));
        }
        return out;
    }

    /** The faction a statue belongs to, by its place in the ring. */
    public static NpcFaction statueFaction(int i) {
        return NpcFaction.values()[i % NpcFaction.values().length];
    }

    /** Builds the battlefield once: the keep and throne at the center, statues, broken engines and banners. */
    public static void ensureBuilt(ServerLevel level) {
        if (level.getBlockState(THRONE.below()).is(Blocks.CRYING_OBSIDIAN)) return;
        BlockState wall = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState trim = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        BlockState floor = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -KEEP; x <= KEEP; x++) {
            for (int z = -KEEP; z <= KEEP; z++) {
                boolean edge = Math.abs(x) == KEEP || Math.abs(z) == KEEP;
                level.setBlock(new BlockPos(x, GROUND - 1, z), floor, 2);
                for (int y = 0; y < 9; y++) {
                    BlockPos p = new BlockPos(x, GROUND + y, z);
                    boolean gate = z == KEEP && Math.abs(x) <= 2 && y < 5;
                    boolean merlon = y == 8 && (x + z) % 2 != 0;
                    level.setBlock(p, edge && !gate && !(y == 8 && merlon) ? (y == 7 ? trim : wall) : air, 2);
                }
            }
        }
        // Corner towers.
        for (int[] c : new int[][]{{-KEEP, -KEEP}, {KEEP, -KEEP}, {-KEEP, KEEP}, {KEEP, KEEP}}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int y = 0; y < 12; y++) level.setBlock(new BlockPos(c[0] + dx, GROUND + y, c[1] + dz), wall, 2);
                }
            }
        }
        // The throne: a raised dais of crying obsidian under a cracked crown of gold.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.setBlock(THRONE.offset(dx, -1, dz), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 2);
        }
        level.setBlock(THRONE.offset(0, 0, -2), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
        level.setBlock(THRONE.offset(0, 1, -2), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
        level.setBlock(THRONE.offset(0, 2, -2), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        level.setBlock(THRONE.offset(-1, 0, -2), Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState(), 2);
        level.setBlock(THRONE.offset(1, 0, -2), Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState(), 2);
        // The statues, mid-fight, in each race's stone.
        List<BlockPos> statues = statues();
        for (int i = 0; i < statues.size(); i++) {
            BlockPos s = statues.get(i);
            BlockState stone = statueStone(statueFaction(i));
            level.setBlock(s.below(), Blocks.STONE_BRICKS.defaultBlockState(), 2);
            level.setBlock(s, stone, 2);
            level.setBlock(s.above(), stone, 2);
            level.setBlock(s.above(2), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
            Direction arm = Direction.from2DDataValue(i % 4);
            level.setBlock(s.above().relative(arm), Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);   // a raised weapon
        }
        // Broken siege engines and fallen banners between them.
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + 0.5;
            BlockPos e = new BlockPos((int) (Math.cos(a) * 32), GROUND, (int) (Math.sin(a) * 32));
            level.setBlock(e, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), 2);
            level.setBlock(e.east(), Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
            level.setBlock(e.east().above(), Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
            level.setBlock(e.north(), Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), 2);
            level.setBlock(e.south(2), Blocks.GRAY_BANNER.defaultBlockState(), 2);
        }
        level.setBlock(ARRIVAL.below(), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);
    }

    static BlockState statueStone(NpcFaction f) {
        return switch (f) {
            case MARAUDERS -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case BLACK_LEGION -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case BURNING_HORDE -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case THE_SWARM -> Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            case SILVERWOOD_REAVERS -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case IRONBEARD_CLAN -> Blocks.STONE.defaultBlockState();
            case FALLEN_HOST -> Blocks.SMOOTH_STONE.defaultBlockState();
        };
    }

    /** Steps through: the player and their army within 32 blocks go to the Frozen Field. False if it can't open. */
    public static boolean enter(ServerPlayer player) {
        ServerLevel field = level(player.server);
        if (field == null || isField(player.level())) return false;
        ensureBuilt(field);
        CompoundTag d = player.getPersistentData();
        d.putLong(RETURN_POS, player.blockPosition().asLong());
        d.putString(RETURN_DIM, player.level().dimension().location().toString());
        List<SoldierEntity> army = TestActions.owned(player);
        ServerLevel from = player.serverLevel();
        from.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 80, 1, 1, 1, 0.1);
        from.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6F, 0.6F);
        Vec3 at = Vec3.atBottomCenterOf(ARRIVAL);
        player.teleportTo(field, at.x, at.y, at.z, 180F, 0F);
        moveArmy(army, field, at);
        player.displayClientMessage(Component.literal("The Frozen Field. The air does not move.").withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC), false);
        return true;
    }

    /** Leaves the Frozen Field: back where the player came from, with their army. */
    public static boolean leave(ServerPlayer player) {
        if (!isField(player.level())) return false;
        CompoundTag d = player.getPersistentData();
        ServerLevel home = player.server.getLevel(ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(d.contains(RETURN_DIM) ? d.getString(RETURN_DIM) : "minecraft:overworld")));
        if (home == null) home = player.server.overworld();
        BlockPos back = d.contains(RETURN_POS) ? BlockPos.of(d.getLong(RETURN_POS)) : home.getSharedSpawnPos();
        List<SoldierEntity> army = TestActions.owned(player);
        Vec3 at = Vec3.atBottomCenterOf(back);
        player.teleportTo(home, at.x, at.y, at.z, player.getYRot(), 0F);
        moveArmy(army, home, at);
        return true;
    }

    static void moveArmy(List<SoldierEntity> army, ServerLevel to, Vec3 at) {
        int i = 0;
        for (SoldierEntity s : army) {
            double a = i++ * 0.7;
            Vec3 p = at.add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
            s.changeDimension(new DimensionTransition(to, p, Vec3.ZERO, s.getYRot(), 0F, DimensionTransition.DO_NOTHING));
        }
    }

    /** Units left in the Field without their commander (who fell or left) follow them home. */
    public static void tick(MinecraftServer server) {
        ServerLevel field = level(server);
        if (field == null) return;
        for (SoldierEntity s : field.getEntities(WFRegistry.SOLDIER.get(), s -> s.getOwnerUUID() != null)) {
            ServerPlayer owner = server.getPlayerList().getPlayer(s.getOwnerUUID());
            if (owner == null || owner.level() == field || !owner.isAlive()) continue;
            Entity moved = s.changeDimension(new DimensionTransition(owner.serverLevel(), owner.position(), Vec3.ZERO,
                    s.getYRot(), 0F, DimensionTransition.DO_NOTHING));
            if (moved == null) s.discard();
        }
        HiddenWarlord.tick(field);
    }
}
