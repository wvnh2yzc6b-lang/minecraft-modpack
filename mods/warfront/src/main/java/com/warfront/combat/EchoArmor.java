package com.warfront.combat;

import com.warfront.Warfront;
import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.registry.WFRegistry;
import com.warfront.world.Homelands;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/**
 * Hive echo armor: a four-piece set for Hive players only, about as tough as diamond. In the dark or underground each
 * piece adds damage and toughness; the full set muffles your steps (sculk sensors and wardens don't hear you walk)
 * and gives night vision in the dark.
 */
public final class EchoArmor {
    private static final ResourceLocation DAMAGE_ID = Warfront.id("echo_dark_damage");
    private static final ResourceLocation TOUGHNESS_ID = Warfront.id("echo_dark_toughness");

    private EchoArmor() {}

    /** The armor pieces. */
    public static class Piece extends ArmorItem {
        public Piece(Holder<ArmorMaterial> material, Type type, Properties properties) {
            super(material, type, properties);
        }

        @Override
        public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
            return isHive(entity) && super.canEquip(stack, slot, entity);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!isHive(player)) {
                if (!level.isClientSide) player.displayClientMessage(Component.literal("Echo armor only fits the Hive.")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            return super.use(level, player, hand);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal("Hive only. In the dark: +1 damage, +1 toughness.").withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.literal("Full set: silent steps, night vision in the dark.").withStyle(ChatFormatting.GRAY));
        }
    }

    public static boolean isHive(LivingEntity e) {
        if (e instanceof Player p) {
            String race = p.level().isClientSide ? ClientRaceState.get(p.getUUID()) : p.getData(WFRegistry.RACE);
            return Race.HIVE.id().equals(race);
        }
        return Race.of(e) == Race.HIVE;
    }

    public static int pieces(LivingEntity e) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (e.getItemBySlot(slot).getItem() instanceof Piece) n++;
        }
        return n;
    }

    /** In the dark: no sky above and dim light, or anywhere in the Otherside. */
    public static boolean isDark(LivingEntity e) {
        BlockPos p = BlockPos.containing(e.getEyePosition());
        return Homelands.isOtherside(e.level()) || e.level().getBrightness(LightLayer.SKY, p) == 0
                || !e.level().canSeeSky(p) && e.level().getMaxLocalRawBrightness(p) < 7;
    }

    /** Sets the dark bonus: +1 damage and +1 toughness per piece while {@code dark}, nothing otherwise. */
    public static void apply(LivingEntity e, boolean dark) {
        int n = isHive(e) ? pieces(e) : 0;
        set(e.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, dark ? n : 0);
        set(e.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID, dark ? n : 0);
        if (dark && n == 4) {
            MobEffectInstance nv = e.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || nv.getDuration() < 220) e.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        }
    }

    private static void set(AttributeInstance attr, ResourceLocation id, double value) {
        if (attr == null) return;
        AttributeModifier old = attr.getModifier(id);
        if (old != null && old.amount() == value) return;
        attr.removeModifier(id);
        if (value != 0) attr.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
    }

    /** Once a second for every player. */
    public static void secondTick(Player p) {
        if (pieces(p) > 0 || p.getAttribute(Attributes.ARMOR_TOUGHNESS).getModifier(TOUGHNESS_ID) != null) apply(p, isDark(p));
    }

    /** Whether this vanilla game event from this entity should be muffled: the full set's quiet steps. */
    public static boolean muffles(LivingEntity e, Holder<GameEvent> event) {
        return pieces(e) == 4 && isHive(e) && (event.is(GameEvent.STEP.key()) || event.is(GameEvent.HIT_GROUND.key())
                || event.is(GameEvent.SWIM.key()) || event.is(GameEvent.SPLASH.key()));
    }
}
