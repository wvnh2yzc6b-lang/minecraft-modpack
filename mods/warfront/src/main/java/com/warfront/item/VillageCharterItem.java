package com.warfront.item;

import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Race;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Village Charter, a human's starting deed. Used inside a village it claims that village for the player; its
 * villagers can then be hired as workers with a Mana Shard (farmers become Farmhands, the rest Masons).
 */
public class VillageCharterItem extends Item {
    /** How far from the claimed spot a villager still belongs to the village. */
    public static final int VILLAGE_RANGE = 96;

    public VillageCharterItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static BlockPos claimed(Player p) {
        long v = p.getData(WFRegistry.CHARTER);
        return v == Long.MIN_VALUE ? null : BlockPos.of(v);
    }

    public static boolean inVillage(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid();
    }

    /** Claims the village the player stands in; false if they're not in one (or aren't human). */
    public static boolean claim(ServerPlayer p) {
        if (Race.of(p) != Race.HUMAN || !inVillage(p.serverLevel(), p.blockPosition())) return false;
        claimAt(p, p.blockPosition());
        return true;
    }

    public static void claimAt(Player p, BlockPos at) {
        p.setData(WFRegistry.CHARTER, at.asLong());
        p.level().playSound(null, at, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
        if (Race.of(p) != Race.HUMAN) {
            p.displayClientMessage(Component.literal("Only a human lord can hold a village charter.").withStyle(ChatFormatting.RED), true);
        } else if (claim(p)) {
            p.displayClientMessage(Component.literal("This village is yours. Hire its villagers with a Mana Shard.")
                    .withStyle(ChatFormatting.GOLD), false);
        } else {
            p.displayClientMessage(Component.literal("Use the charter inside a village.").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * Hires a villager of the player's claimed village: it becomes a human worker owned by the player (a Farmhand if it
     * farmed, a Mason otherwise), keeps its name, and costs one Mana Shard. Returns the worker, or null.
     */
    @Nullable
    public static SoldierEntity hire(Player p, Villager villager, ItemStack shard) {
        BlockPos home = claimed(p);
        if (home == null || Race.of(p) != Race.HUMAN || villager.isBaby() || !shard.is(WFRegistry.MANA_SHARD.get())
                || !villager.blockPosition().closerThan(home, VILLAGE_RANGE) || !(villager.level() instanceof ServerLevel level)) {
            return null;
        }
        SoldierEntity worker = WFRegistry.SOLDIER.get().create(level);
        if (worker == null) return null;
        SoldierRole role = villager.getVillagerData().getProfession() == VillagerProfession.FARMER
                || villager.getVillagerData().getProfession() == VillagerProfession.NONE ? SoldierRole.FARMER : SoldierRole.BUILDER;
        worker.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), 0F);
        worker.setupAsRecruit(p, role, Race.HUMAN);
        if (villager.hasCustomName()) worker.setCustomName(villager.getCustomName());
        level.addFreshEntity(worker);
        worker.assignPost(worker.position(), villager.getYRot());
        villager.discard();
        if (!p.getAbilities().instabuild) shard.shrink(1);
        level.playSound(null, worker.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return worker;
    }

    /** Humans are welcome traders: each villager they meet thinks well of them for good (vanilla gossip). */
    public static void befriend(Player p, Villager villager) {
        if (villager.getGossips().getReputation(p.getUUID(), t -> t == GossipType.MAJOR_POSITIVE) < 20) {
            villager.getGossips().add(p.getUUID(), GossipType.MAJOR_POSITIVE, 4);
        }
    }

    /** A new human's start: next to the nearest village, charter in hand. False if no village is near. */
    public static boolean sendToVillage(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ItemStack charter = new ItemStack(WFRegistry.VILLAGE_CHARTER.get());
        if (!p.getInventory().add(charter)) p.drop(charter, false);
        if (!level.dimensionType().hasSkyLight()) return false;
        BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, p.blockPosition(), 100, false);
        if (village == null) return false;
        BlockPos stand = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, village);
        p.teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, p.getYRot(), 0F);
        p.setRespawnPosition(level.dimension(), stand, p.getYRot(), true, false);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Humans: use inside a village to claim it.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Then hire its villagers with a Mana Shard.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
