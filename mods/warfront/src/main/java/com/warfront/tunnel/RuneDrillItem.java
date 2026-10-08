package com.warfront.tunnel;

import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The dwarves' Rune Drill: a pickaxe of diamond tier that bores a 3x3 face with every block it breaks, faster than a
 * pickaxe. Too heavy for anyone but a dwarf. Sneak to mine a single block. Repaired with Mana Crystals.
 */
public class RuneDrillItem extends PickaxeItem {
    public static final float DWARF_SPEED = 1.4F;

    public RuneDrillItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1.0F, -3.0F)));
    }

    public static boolean isDwarf(Player p) {
        String race = p.level().isClientSide ? ClientRaceState.get(p.getUUID()) : p.getData(WFRegistry.RACE);
        return Race.DWARF.id().equals(race);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(WFRegistry.MANA_CRYSTAL.get());
    }

    /**
     * Breaks the eight blocks around {@code center} on the face the player is mining ({@code face} is the side that
     * was hit). Skips air, unbreakable blocks, block entities, rune stone and anything the drill can't harvest.
     * Returns how many it broke (0 for a non-dwarf).
     */
    public static int bore(ServerLevel level, Player player, BlockPos center, Direction face, ItemStack drill) {
        if (!isDwarf(player) || !(drill.getItem() instanceof RuneDrillItem)) return 0;
        Direction.Axis axis = face.getAxis();
        int broken = 0;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos p = switch (axis) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                BlockState state = level.getBlockState(p);
                if (state.isAir() || state.hasBlockEntity() || state.is(Tunnels.RAIDER_UNBREAKABLE)) continue;
                if (state.getDestroySpeed(level, p) < 0 || !drill.isCorrectToolForDrops(state)) continue;
                if (!level.mayInteract(player, p)) continue;
                level.destroyBlock(p, !player.getAbilities().instabuild, player);
                broken++;
                if (!player.getAbilities().instabuild) drill.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                if (drill.isEmpty()) return broken;
            }
        }
        return broken;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Dwarf only. Bores a 3x3 face; sneak for one block.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Repair with Mana Crystals.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
