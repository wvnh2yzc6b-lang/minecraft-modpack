package com.warfront.item;

import com.warfront.army.Formation;
import com.warfront.army.Order;
import com.warfront.entity.SoldierEntity;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Right-click: cycle orders (Follow, Hold, Charge). Sneak + right-click: cycle formations.
 * Hold anchors the formation where you stand, facing where you look. Charge sends the army
 * at the spot you are looking at.
 */
public class CommanderBatonItem extends Item {
    public CommanderBatonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        Order order = Order.byOrdinal(player.getData(WFRegistry.ARMY_ORDER));
        Formation formation = Formation.byOrdinal(player.getData(WFRegistry.ARMY_FORMATION));
        boolean changingFormation = player.isShiftKeyDown();
        if (changingFormation) {
            formation = formation.next();
            player.setData(WFRegistry.ARMY_FORMATION, formation.ordinal());
        } else {
            order = order.nextForPlayer();
            player.setData(WFRegistry.ARMY_ORDER, order.ordinal());
        }

        float yaw = player.getYRot();
        Vec3 anchor = null;
        if (!changingFormation) {
            if (order == Order.HOLD) {
                float rad = yaw * Mth.DEG_TO_RAD;
                anchor = player.position().add(-Mth.sin(rad) * 3.0, 0, Mth.cos(rad) * 3.0);
            } else if (order == Order.CHARGE) {
                HitResult hit = player.pick(64.0, 1.0F, false);
                anchor = hit.getLocation();
            }
        }

        List<SoldierEntity> army = armyOf(player);
        for (SoldierEntity s : army) {
            s.command(order, formation, anchor, yaw);
        }

        Component msg = Component.literal(changingFormation ? "Formation: " + formation.title : order.title)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("  (" + army.size() + " soldiers, " + formation.title + ")")
                        .withStyle(ChatFormatting.GRAY));
        player.displayClientMessage(msg, true);
        level.playSound(null, player.blockPosition(),
                changingFormation ? SoundEvents.ARMOR_EQUIP_CHAIN.value() : SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.PLAYERS, 1.0F, order == Order.CHARGE ? 1.4F : 0.9F);
        if (order == Order.CHARGE && !changingFormation) {
            level.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 2.0F, 1.3F);
        }
        player.getCooldowns().addCooldown(this, 8);
        return InteractionResultHolder.consume(stack);
    }

    public static List<SoldierEntity> armyOf(Player player) {
        return player.level().getEntitiesOfClass(SoldierEntity.class, player.getBoundingBox().inflate(96),
                s -> s.isAlive() && s.isOwnedBy(player));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: Follow / Hold / Charge").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + right-click: change formation").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Commands every soldier you own within 96 blocks").withStyle(ChatFormatting.DARK_GRAY));
    }
}
