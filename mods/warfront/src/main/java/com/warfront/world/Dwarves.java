package com.warfront.world;

import com.warfront.Warfront;
import com.warfront.combat.Resolve;
import com.warfront.faction.Race;
import com.warfront.registry.WFRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Dwarf identity: deep miners (more Mana Crystals from ore, stone cut 20% faster, fewer shards from Manabloom) and the
 * player side of Hold the Line (Resolve each second, the Oath on a crouch, the shockwave when it ends).
 */
@EventBusSubscriber(modid = Warfront.MODID)
public final class Dwarves {
    public static final float STONE_SPEED = 1.2F;
    public static final float ORE_BONUS_CHANCE = 0.5F;
    public static final float BLOOM_SHARDS = 0.8F;

    private Dwarves() {}

    public static boolean isDwarf(Entity e) {
        return Race.of(e) == Race.DWARF;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getState().is(BlockTags.BASE_STONE_OVERWORLD) && isDwarf(event.getEntity())) {
            event.setNewSpeed(event.getNewSpeed() * STONE_SPEED);
        }
    }

    /** Dwarves pull more crystals out of Mana Ore and fewer shards out of Manabloom. */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player p) || !isDwarf(p)) return;
        BlockState state = event.getState();
        if (state.is(WFRegistry.MANA_ORE.get()) || state.is(WFRegistry.DEEPSLATE_MANA_ORE.get())) {
            boolean crystals = event.getDrops().stream().anyMatch(i -> i.getItem().is(WFRegistry.MANA_CRYSTAL.get()));
            if (crystals && event.getLevel().random.nextFloat() < ORE_BONUS_CHANCE) {
                ItemEntity first = event.getDrops().get(0);
                event.getDrops().add(new ItemEntity(event.getLevel(), first.getX(), first.getY(), first.getZ(),
                        new ItemStack(WFRegistry.MANA_CRYSTAL.get())));
            }
        } else if (state.is(WFRegistry.MANABLOOM.get())) {
            for (ItemEntity drop : event.getDrops()) {
                ItemStack s = drop.getItem();
                if (s.is(WFRegistry.MANA_SHARD.get())) s.setCount(Math.max(1, Math.round(s.getCount() * BLOOM_SHARDS)));
            }
        }
    }

    /** A dwarf player at full Resolve swears the Oath by crouching. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !p.isShiftKeyDown() || !isDwarf(p)) return;
        if (Resolve.current(p) >= Resolve.MAX) Resolve.swear(p);
    }

    @SubscribeEvent
    public static void onEffectEnds(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired event) {
        var inst = event.getEffectInstance();
        if (inst != null && inst.is(WFRegistry.OATH_OF_STONE) && !event.getEntity().level().isClientSide) {
            Resolve.shockwave(event.getEntity());
        }
    }

    /** Once a second, from the server tick. */
    public static void secondTick(ServerPlayer p) {
        if (!isDwarf(p)) return;
        int before = Resolve.current(p);
        Resolve.tickSecond(p);
        int after = Resolve.current(p);
        if (after != before) p.displayClientMessage(Resolve.bar(after), true);
    }
}
