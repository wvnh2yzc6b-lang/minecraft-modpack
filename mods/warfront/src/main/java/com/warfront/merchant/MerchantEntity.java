package com.warfront.merchant;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import com.warfront.registry.WFRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * The traveling merchant: a neutral caravan master who trades in Mana Crystals. His stock rotates each visit, and
 * a couple of trades grow with the base level of the player he visits. Strike him and he leaves at once, and skips
 * his next visit.
 */
public class MerchantEntity extends WanderingTrader {
    /** Base level of the host, set before the first trade list is drawn. */
    private int hostLevel = 1;

    public MerchantEntity(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
    }

    public void setHostLevel(int level) {
        hostLevel = Math.max(1, level);
        offers = null;   // redraw for this host
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        offers.addAll(drawOffers(random, hostLevel));
    }

    /** One visit's stock: rare materials and seeds for crystals, crystals for raid loot. */
    public static List<MerchantOffer> drawOffers(RandomSource random, int hostLevel) {
        Item crystal = WFRegistry.MANA_CRYSTAL.get();
        List<MerchantOffer> sells = new ArrayList<>();
        // Other races' building blocks.
        Item[] blocks = {Items.DEEPSLATE_BRICKS, Items.MUD_BRICKS, Items.PRISMARINE_BRICKS, Items.POLISHED_BLACKSTONE_BRICKS,
                Items.END_STONE_BRICKS, Items.QUARTZ_BRICKS, Items.RED_NETHER_BRICKS, Items.TUFF_BRICKS, Items.MOSSY_STONE_BRICKS};
        for (Item b : blocks) sells.add(sell(crystal, 1, b, 16, 8));
        sells.add(sell(crystal, 3, Items.ECHO_SHARD, 1, 4));
        sells.add(sell(crystal, 1, WFRegistry.MANABLOOM_SEEDS.get(), 8, 6));
        sells.add(sell(crystal, 2, Items.NAME_TAG, 1, 3));
        sells.add(sell(crystal, 2, Items.SADDLE, 1, 3));
        optional(sells, "aether:ambrosium_shard", crystal, 1, 8);
        optional(sells, "aether:zanite_gemstone", crystal, 2, 4);
        optional(sells, "irons_spellbooks:arcane_essence", crystal, 2, 4);
        optional(sells, "irons_spellbooks:common_ink", crystal, 2, 2);
        java.util.Collections.shuffle(sells, new java.util.Random(random.nextLong()));
        List<MerchantOffer> out = new ArrayList<>(sells.subList(0, Math.min(sells.size(), 5 + random.nextInt(2))));
        // Trades that grow with the base he visits.
        out.add(sell(crystal, 4, Items.DIAMOND, Math.max(1, hostLevel - 1), 3));
        if (hostLevel >= 4) out.add(sell(crystal, 12, Items.NETHERITE_SCRAP, 1, 2));
        // What he buys: raid loot.
        out.add(new MerchantOffer(new ItemCost(WFRegistry.WAR_MARK.get(), 8), new ItemStack(crystal, 1), 16, 2, 0.05F));
        out.add(new MerchantOffer(new ItemCost(Items.EMERALD, 4), new ItemStack(crystal, 1), 12, 2, 0.05F));
        return out;
    }

    private static MerchantOffer sell(ItemLike price, int count, ItemLike item, int amount, int uses) {
        return new MerchantOffer(new ItemCost(price, count), new ItemStack(item, amount), uses, 2, 0.05F);
    }

    private static void optional(List<MerchantOffer> sells, String id, Item crystal, int price, int amount) {
        ResourceLocation rl = ResourceLocation.parse(id);
        if (BuiltInRegistries.ITEM.containsKey(rl)) sells.add(sell(crystal, price, BuiltInRegistries.ITEM.get(rl), amount, 4));
    }

    /** Humans trade a fifth cheaper. */
    @Override
    public void setTradingPlayer(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player player) {
        super.setTradingPlayer(player);
        if (player != null) {
            boolean human = com.warfront.faction.Race.of(player) == com.warfront.faction.Race.HUMAN;
            for (MerchantOffer offer : getOffers()) priceFor(offer, human);
        }
    }

    /** Sets the offer's price for a human (20% off, at least one less where it can be) or anyone else; returns it. */
    public static int priceFor(MerchantOffer offer, boolean human) {
        offer.setSpecialPriceDiff(human ? -(int) Math.ceil(offer.getBaseCostA().getCount() * 0.2) : 0);
        return offer.getCostA().getCount();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof ServerPlayer p && isAlive()) {
            Caravan.insulted((ServerLevel) level(), this, p);
        }
        return hurt;
    }
}
