package com.warfront.fortress;

import com.warfront.Warfront;
import com.warfront.block.ManaWellBlockEntity;
import com.warfront.block.WarStandardBlockEntity;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Relation;
import com.warfront.mana.ManaNetwork;
import com.warfront.mana.ManaNodeBlockEntity;
import com.warfront.registry.WFRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Trophy banners: each beaten warlord's banner, planted within 8 blocks of your War Standard, gives your units in base
 * range a small lasting bonus: Marauders +5% damage, Black Legion +5% health, Burning Horde +2 armor, Swarm +5% speed,
 * Silverwood +10% knockback resistance, Ironbeard +1 armor toughness. The Fallen Host's banner speeds the base's mana.
 * One of each counts.
 */
public final class TrophyBanners {
    public static final double STANDARD_RANGE = 8;
    public static final double AURA_RANGE = 32;
    /** Mana the Fallen Host's banner adds to each well in reach every 10 seconds. */
    public static final float FALLEN_MANA = 5F;

    private TrophyBanners() {}

    /** The aura effects, built when the registry asks for them. */
    public static MobEffect aura(NpcFaction f) {
        MobEffect e = new MobEffect(MobEffectCategory.BENEFICIAL, f.color.getColor() == null ? 0xFFFFFF : f.color.getColor()) {};
        var id = Warfront.id("trophy_" + f.name().toLowerCase(Locale.ROOT));
        return switch (f) {
            case MARAUDERS -> e.addAttributeModifier(Attributes.ATTACK_DAMAGE, id, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case BLACK_LEGION -> e.addAttributeModifier(Attributes.MAX_HEALTH, id, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case BURNING_HORDE -> e.addAttributeModifier(Attributes.ARMOR, id, 2.0, AttributeModifier.Operation.ADD_VALUE);
            case THE_SWARM -> e.addAttributeModifier(Attributes.MOVEMENT_SPEED, id, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case SILVERWOOD_REAVERS -> e.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id, 0.1, AttributeModifier.Operation.ADD_VALUE);
            case IRONBEARD_CLAN -> e.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, id, 1.0, AttributeModifier.Operation.ADD_VALUE);
            case FALLEN_HOST -> e;   // the Fallen Host's gift is mana, not an aura
        };
    }

    public static ItemStack item(NpcFaction f) {
        ItemStack stack = new ItemStack(WFRegistry.TROPHY_BANNER_ITEM.get());
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(TrophyBannerBlock.FACTION, f.ordinal()));
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(Warlords.title(f) + "'s Banner").withStyle(f.color));
        return stack;
    }

    /** The War Standard within 8 blocks of a banner, if any. */
    @Nullable
    public static WarStandardBlockEntity standardNear(ServerLevel level, BlockPos banner) {
        for (BlockPos p : BlockPos.betweenClosed(banner.offset(-8, -4, -8), banner.offset(8, 4, 8))) {
            if (level.getBlockEntity(p) instanceof WarStandardBlockEntity s) return s;
        }
        return null;
    }

    /** Every 10 seconds: each planted banner refreshes its aura on the units of its standard's side. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (BlockPos pos : new ArrayList<>(TrophyBannerBlockEntity.loaded(level))) {
                if (!(level.getBlockEntity(pos) instanceof TrophyBannerBlockEntity banner)) continue;
                WarStandardBlockEntity standard = standardNear(level, pos);
                if (standard == null) continue;
                apply(level, pos, banner.faction(), standard.factionKey(server));
            }
        }
    }

    public static void apply(ServerLevel level, BlockPos banner, NpcFaction f, String key) {
        if (f == NpcFaction.FALLEN_HOST) {
            for (ManaNodeBlockEntity n : ManaNetwork.near(level, banner, 16)) {
                if (n instanceof ManaWellBlockEntity w && Factions.relation(level.getServer(), key, w.factionKey(level.getServer())) == Relation.ALLY) {
                    w.setMana(w.getMana() + FALLEN_MANA);
                }
            }
            return;
        }
        Holder<MobEffect> effect = WFRegistry.TROPHY_AURAS.get(f);
        if (effect == null) return;
        for (SoldierEntity s : level.getEntitiesOfClass(SoldierEntity.class, new AABB(banner).inflate(AURA_RANGE),
                s -> s.isAlive() && s.getOwnerUUID() != null && Factions.relation(level.getServer(), key, s.getFactionKey()) == Relation.ALLY)) {
            s.addEffect(new MobEffectInstance(effect, 260, 0, true, true));
        }
    }

    public static Map<NpcFaction, ?> all() {
        return WFRegistry.TROPHY_AURAS;
    }
}
