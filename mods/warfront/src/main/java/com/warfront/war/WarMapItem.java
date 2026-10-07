package com.warfront.war;

import com.warfront.Warfront;
import com.warfront.faction.NpcFaction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;
import java.util.Locale;

/**
 * A War Map, dropped by the last raider of a faction's raid once its war meter is full. Use it to chart the way to
 * that faction's warlord fortress: it turns into a filled map with the fortress marked, like an explorer map.
 */
public class WarMapItem extends Item {
    public WarMapItem(Properties properties) {
        super(properties);
    }

    /** The structure tag listing a faction's fortress. */
    public static TagKey<Structure> fortressTag(NpcFaction f) {
        return TagKey.create(Registries.STRUCTURE, Warfront.id("fortress/" + f.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        NpcFaction f = Campaign.mapFaction(stack);
        if (f == null || !(level instanceof ServerLevel server)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        BlockPos found = server.findNearestMapStructure(fortressTag(f), player.blockPosition(), 100, false);
        if (found == null) {
            player.displayClientMessage(Component.literal("The " + f.displayName + " fortress isn't in this world"
                    + (level.dimension() == Level.OVERWORLD ? "." : ". Try the Overworld, the Nether or their homeland dimension."))
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        ItemStack map = MapItem.create(server, found.getX(), found.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(server, map);
        MapItemSavedData.addTargetDecoration(map, found, "+", MapDecorationTypes.RED_X);
        map.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("War Map: " + f.displayName).withStyle(f.color));
        player.displayClientMessage(Component.literal("The map marks the " + f.displayName + " warlord's fortress.")
                .withStyle(f.color), true);
        return InteractionResultHolder.sidedSuccess(map, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        NpcFaction f = Campaign.mapFaction(stack);
        tooltip.add(Component.literal(f == null ? "A blank war map." : "Leads to the " + f.displayName + " warlord's fortress.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Use it to chart the way.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
