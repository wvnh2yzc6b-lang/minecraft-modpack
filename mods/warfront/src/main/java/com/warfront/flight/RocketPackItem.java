package com.warfront.flight;

import com.warfront.config.WFConfig;
import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The orc rocket pack. Worn in the chest slot; hold jump in the air to thrust: straight up when looking level or up,
 * forward and up when looking down while sprinting. Burns fuel fast (coal or charcoal, Mana Crystals last longer),
 * loud and smoky. Load it by dropping fuel onto it in your inventory, or sneak-use fuel while wearing it.
 */
public class RocketPackItem extends Item implements Equipable {
    private static final String FUEL = "warfront_fuel";
    public static final double LIFT = 0.11;
    public static final double PUSH = 0.09;
    public static final double MAX_SPEED = 1.3;
    public static final int MAX_FUEL_SECONDS = 120;

    public RocketPackItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    /** Orcs only. Works on both sides. */
    public static boolean isOrc(Player player) {
        String race = player.level().isClientSide ? ClientRaceState.get(player.getUUID()) : player.getData(WFRegistry.RACE);
        return Race.ORC.id().equals(race);
    }

    public static int fuel(ItemStack pack) {
        CustomData d = pack.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : d.copyTag().getInt(FUEL);
    }

    public static void setFuel(ItemStack pack, int ticks) {
        CompoundTag t = pack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        t.putInt(FUEL, Math.max(0, ticks));
        pack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** Ticks of thrust one item of fuel gives (0 if it isn't fuel). */
    public static int fuelValue(ItemStack fuel) {
        if (fuel.is(Items.COAL) || fuel.is(Items.CHARCOAL)) return WFConfig.ROCKET_COAL_SECONDS.get() * 20;
        if (fuel.is(WFRegistry.MANA_CRYSTAL.get())) return WFConfig.ROCKET_CRYSTAL_SECONDS.get() * 20;
        return 0;
    }

    /** Loads as much of {@code fuel} as fits; returns how many items went in. */
    public static int load(ItemStack pack, ItemStack fuel) {
        int value = fuelValue(fuel);
        if (value <= 0) return 0;
        int room = (MAX_FUEL_SECONDS * 20 - fuel(pack)) / value;
        int n = Math.max(0, Math.min(room, fuel.getCount()));
        if (n > 0) {
            setFuel(pack, fuel(pack) + n * value);
            fuel.shrink(n);
        }
        return n;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.PRIMARY || fuelValue(other) <= 0) return false;
        if (load(pack, other) > 0) player.playSound(SoundEvents.FLINTANDSTEEL_USE, 0.6F, 1.2F);
        return true;
    }

    public static boolean wearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof RocketPackItem;
    }

    /** Whether the player can thrust right now: an orc, wearing a fuelled pack, in the air. */
    public static boolean canThrust(Player player) {
        ItemStack pack = player.getItemBySlot(EquipmentSlot.CHEST);
        return pack.getItem() instanceof RocketPackItem && isOrc(player) && fuel(pack) > 0 && !player.onGround()
                && !player.isInWater() && !player.getAbilities().flying && !player.isFallFlying();
    }

    /** Thrust for one tick, on the side that moves the player. */
    public static void thrust(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 m = player.getDeltaMovement();
        Vec3 push = player.getXRot() > 20F && player.isSprinting()
                ? new Vec3(look.x * PUSH * 1.6, LIFT * 0.6, look.z * PUSH * 1.6)   // diving forward
                : new Vec3(look.x * PUSH * 0.4, LIFT, look.z * PUSH * 0.4);       // straight up
        m = m.add(push);
        if (m.length() > MAX_SPEED) m = m.normalize().scale(MAX_SPEED);
        player.setDeltaMovement(m);
        player.fallDistance = 0F;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Orc only. Hold jump in the air to thrust.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Fuel: " + fuel(stack) / 20 + "s").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Drop coal, charcoal or Mana Crystals on it to refuel.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
