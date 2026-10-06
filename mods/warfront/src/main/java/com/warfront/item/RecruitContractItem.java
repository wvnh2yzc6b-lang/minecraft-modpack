package com.warfront.item;

import com.warfront.army.Formation;
import com.warfront.army.Order;
import com.warfront.army.SoldierRole;
import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Race;
import com.warfront.mana.Mana;
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
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Signing a contract recruits a soldier of your race into your army. */
public class RecruitContractItem extends Item {
    private final SoldierRole role;

    public RecruitContractItem(SoldierRole role, Properties properties) {
        super(properties);
        this.role = role;
    }

    public SoldierRole getRole() {
        return role;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        int max = WFConfig.MAX_ARMY_SIZE.get();
        int count = level.getEntitiesOfClass(SoldierEntity.class, new AABB(player.blockPosition()).inflate(256),
                s -> s.isAlive() && s.isOwnedBy(player)).size();
        if (count >= max) {
            player.displayClientMessage(Component.literal("Your army is at full strength (" + max + ").")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        Race race = Race.byId(player.getData(WFRegistry.RACE));
        if (race == null) race = Race.HUMAN;
        int cost = role.manaCost(race);
        if (!Mana.trySpend(player, cost)) {
            player.displayClientMessage(Component.literal("Not enough mana to summon a " + role.displayName()
                    + ": need " + cost + ", have " + (int) Mana.get(player) + ".").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.fail(stack);
        }

        SoldierEntity soldier = WFRegistry.SOLDIER.get().create(level);
        if (soldier == null) {
            Mana.add(player, cost);
            return InteractionResultHolder.fail(stack);
        }
        float rad = player.getYRot() * Mth.DEG_TO_RAD;
        soldier.moveTo(player.getX() - Mth.sin(rad) * 2.0, player.getY(), player.getZ() + Mth.cos(rad) * 2.0,
                player.getYRot() + 180F, 0F);
        soldier.setupAsRecruit(player, role, race);

        Order order = Order.byOrdinal(player.getData(WFRegistry.ARMY_ORDER));
        Formation formation = Formation.byOrdinal(player.getData(WFRegistry.ARMY_FORMATION));
        boolean posted = role.posted();
        if (posted) {
            level.addFreshEntity(soldier);   // join the level first so a builder can survey around it
            soldier.assignPost(soldier.position(), player.getYRot());
            if (role == SoldierRole.FARMER) {
                soldier.getWorkItems().addItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 8));
            }
        } else if (order == Order.FOLLOW) {
            soldier.command(order, formation, null, player.getYRot());
        } else {
            soldier.command(Order.HOLD, formation, soldier.position(), player.getYRot());
        }
        if (!posted) level.addFreshEntity(soldier);

        level.playSound(null, soldier.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1F, 1F);
        player.displayClientMessage(Component.literal("A " + soldier.getRace().displayName() + " "
                + role.displayName() + " answers your summons. (" + (count + 1) + "/" + max + ", -" + cost + " mana)"
                + (posted ? " It works here; sneak + right-click it to move its post." : ""))
                .withStyle(ChatFormatting.GREEN), true);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(role.description).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Costs " + role.manaCost + " mana (scaled by your race)")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Right-click to recruit").withStyle(ChatFormatting.DARK_GRAY));
    }
}
