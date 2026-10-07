package com.warfront.fortress;

import com.warfront.faction.NpcFaction;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** One unique piece of gear per warlord, usable by any race, with a bonus tied to that faction's power. */
public final class WarlordGear {
    private WarlordGear() {}

    public static ItemStack item(NpcFaction f) {
        return new ItemStack(switch (f) {
            case MARAUDERS -> WFRegistry.SKULLSPLITTER.get();
            case BLACK_LEGION -> WFRegistry.LEGION_WARPLATE.get();
            case BURNING_HORDE -> WFRegistry.EMBERBRAND.get();
            case THE_SWARM -> WFRegistry.BROODFANG.get();
            case SILVERWOOD_REAVERS -> WFRegistry.THORNBOW.get();
            case IRONBEARD_CLAN -> WFRegistry.RUNEHAMMER.get();
            case FALLEN_HOST -> WFRegistry.FALLEN_HALO.get();
        });
    }

    private static Item.Properties props() {
        return new Item.Properties().rarity(Rarity.EPIC).fireResistant();
    }

    static void lore(List<Component> tooltip, String line) {
        tooltip.add(Component.literal(line).withStyle(ChatFormatting.GOLD));
    }

    /** Marauder warlord's axe: every hit weakens the target, like an orc's rage breaking a foe. */
    public static class Skullsplitter extends AxeItem {
        public Skullsplitter() {
            super(Tiers.NETHERITE, props().attributes(AxeItem.createAttributes(Tiers.NETHERITE, 6.0F, -3.0F)));
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), attacker);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            lore(tooltip, "Gorzak's axe. Every hit weakens the foe.");
        }
    }

    /** Burning Horde warlord's sword: sets foes ablaze and heals its bearer a little with each burning kill. */
    public static class Emberbrand extends SwordItem {
        public Emberbrand() {
            super(Tiers.DIAMOND, props().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.4F)));
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            target.igniteForSeconds(5F);
            if (target.isDeadOrDying()) attacker.heal(2F);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            lore(tooltip, "Azhrael's blade. Sets foes ablaze; kills feed you.");
        }
    }

    /** Swarm warlord's fang: a quick blade that poisons, like the Brood's spitters. */
    public static class Broodfang extends SwordItem {
        public Broodfang() {
            super(Tiers.DIAMOND, props().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 2, -1.8F)));
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), attacker);
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            lore(tooltip, "The Brood Mother's fang. Quick, and every cut poisons.");
        }
    }

    /** Ironbeard warlord's hammer: every fourth hit calls rune lightning down on the foe. */
    public static class Runehammer extends AxeItem {
        public Runehammer() {
            super(Tiers.NETHERITE, props().attributes(AxeItem.createAttributes(Tiers.NETHERITE, 7.0F, -3.2F)));
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            if (attacker.level() instanceof ServerLevel level && attacker.getRandom().nextInt(4) == 0) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(target.position());
                    if (attacker instanceof net.minecraft.server.level.ServerPlayer sp) bolt.setCause(sp);
                    level.addFreshEntity(bolt);
                }
            }
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            lore(tooltip, "Thane Brokk's hammer. Now and then it calls the lightning.");
        }
    }

    /** Silverwood warlord's bow: arrows fly true and hit much harder, like an elf's long shot. */
    public static class Thornbow extends BowItem {
        public Thornbow() {
            super(props().durability(768));
        }

        @Override
        protected void shootProjectile(LivingEntity shooter, net.minecraft.world.entity.projectile.Projectile projectile, int index,
                                       float velocity, float inaccuracy, float angle, @org.jetbrains.annotations.Nullable LivingEntity target) {
            if (projectile instanceof AbstractArrow arrow) arrow.setBaseDamage(arrow.getBaseDamage() + 2.0);
            super.shootProjectile(shooter, projectile, index, velocity * 1.15F, inaccuracy * 0.5F, angle, target);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            lore(tooltip, "Thalanor's bow. Arrows fly truer, faster and harder.");
        }
    }

    /** Black Legion warlord's plate: netherite-hard, and it shrugs off knockback. */
    public static class LegionWarplate extends ArmorItem {
        public LegionWarplate() {
            super(ArmorMaterials.NETHERITE, Type.CHESTPLATE, props().durability(Type.CHESTPLATE.getDurability(40)));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            lore(tooltip, "Lord-Marshal Varric's warplate. The line does not break.");
        }
    }

    /** Fallen Host warlord's halo: worn, it lets you fall like a feather and see in the dark. */
    public static class FallenHalo extends ArmorItem {
        public FallenHalo() {
            super(ArmorMaterials.GOLD, Type.HELMET, props().durability(Type.HELMET.getDurability(30)));
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (!level.isClientSide && entity instanceof Player p && p.getItemBySlot(EquipmentSlot.HEAD) == stack && p.tickCount % 40 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false));
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            lore(tooltip, "Seraphiel's cracked halo. Worn: slow falling and night vision.");
        }
    }
}
