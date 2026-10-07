package com.warfront.flight;

import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Angel wings. By day under open sky an angel wearing them flies freely (a little slower than creative); at night,
 * underground or under a roof the wings only glide like an elytra. Losing the sky mid-air drops you into the glide.
 */
public class AngelWingsItem extends ElytraItem {
    public static final float FLY_SPEED = 0.035F;
    private static final String GRANTED = "warfront_wings_flight";

    public AngelWingsItem(Properties properties) {
        super(properties);
    }

    public static boolean isAngel(Player player) {
        String race = player.level().isClientSide ? ClientRaceState.get(player.getUUID()) : player.getData(WFRegistry.RACE);
        return Race.ANGEL.id().equals(race);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return isFlyEnabled(stack) && entity instanceof Player player && isAngel(player);
    }

    /** Daylight, open sky above and no thunderstorm: free flight. */
    public static boolean flightAllowed(boolean day, boolean sky, boolean thunder) {
        return day && sky && !thunder;
    }

    public static boolean flightAllowed(Player p) {
        Level level = p.level();
        return flightAllowed(level.isDay(), level.canSeeSky(BlockPos.containing(p.getEyePosition())), level.isThundering());
    }

    /** Server, every few ticks: grant or take away free flight. */
    public static void update(Player p) {
        boolean wearing = p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof AngelWingsItem && isAngel(p);
        boolean granted = p.getPersistentData().getBoolean(GRANTED);
        boolean other = p.isCreative() || p.isSpectator() || p.getData(WFRegistry.GOD_MODE);
        boolean allow = wearing && flightAllowed(p);
        if (allow && !granted && !other) {
            p.getAbilities().mayfly = true;
            p.getAbilities().setFlyingSpeed(FLY_SPEED);
            p.getPersistentData().putBoolean(GRANTED, true);
            p.onUpdateAbilities();
        } else if (!allow && granted) {
            p.getPersistentData().putBoolean(GRANTED, false);
            p.getAbilities().setFlyingSpeed(0.05F);
            if (!other) {
                boolean wasFlying = p.getAbilities().flying;
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                // Never a straight fall: the wings catch you in a glide.
                if (wasFlying && wearing && !p.onGround()) p.startFallFlying();
            }
            p.onUpdateAbilities();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Angel only. By day under open sky: fly freely.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("At night or under a roof they only glide.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
