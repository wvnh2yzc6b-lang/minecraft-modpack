package com.warfront.tunnel;

import com.warfront.Warfront;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Dwarf tunnels: the Rune Drill's 3x3 bore and weight, and rune stone that only dwarves cut quickly. */
@EventBusSubscriber(modid = Warfront.MODID)
public final class Tunnels {
    /** Blocks raiders never hack through (rune stone); other mods can add theirs. */
    public static final TagKey<Block> RAIDER_UNBREAKABLE = TagKey.create(Registries.BLOCK, Warfront.id("raider_unbreakable"));
    /** How much faster a dwarf with a pickaxe or the drill cuts rune stone, and how much slower everyone else does. */
    public static final float DWARF_RUNE_SPEED = 6F;
    public static final float OTHER_RUNE_SPEED = 0.25F;

    private Tunnels() {}

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        ItemStack held = p.getMainHandItem();
        boolean drill = held.getItem() instanceof RuneDrillItem;
        boolean dwarf = RuneDrillItem.isDwarf(p);
        float speed = event.getNewSpeed();
        if (drill && !dwarf) {
            speed *= 0.1F;
            if (!p.level().isClientSide && p.tickCount % 20 == 0) {
                p.displayClientMessage(Component.literal("The Rune Drill is too heavy for you. Only dwarves can work it.")
                        .withStyle(ChatFormatting.RED), true);
            }
        } else if (drill) {
            speed *= RuneDrillItem.DWARF_SPEED;
        }
        if (event.getState().is(RAIDER_UNBREAKABLE)) {
            speed *= dwarf && (drill || held.is(ItemTags.PICKAXES)) ? DWARF_RUNE_SPEED : OTHER_RUNE_SPEED;
        }
        event.setNewSpeed(speed);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player p = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level) || p.isShiftKeyDown()) return;
        ItemStack held = p.getMainHandItem();
        if (!(held.getItem() instanceof RuneDrillItem) || !RuneDrillItem.isDwarf(p)) return;
        if (!held.isCorrectToolForDrops(event.getState())) return;
        Vec3 eye = p.getEyePosition();
        Vec3 to = eye.add(p.getLookAngle().scale(p.blockInteractionRange() + 1));
        BlockHitResult hit = level.clip(new ClipContext(eye, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        Direction face = hit.getType() == HitResult.Type.BLOCK ? hit.getDirection() : Direction.getNearest(p.getLookAngle().x, p.getLookAngle().y, p.getLookAngle().z).getOpposite();
        RuneDrillItem.bore(level, p, event.getPos(), face, held);
    }

    /** Gives the dwarf tunnel kit (Test Panel and /wftest). */
    public static void kit(Player p) {
        give(p, new ItemStack(WFRegistry.RUNE_DRILL.get()));
        give(p, new ItemStack(WFRegistry.RUNE_STONE_ITEM.get(), 64));
        give(p, new ItemStack(WFRegistry.SPIKE_FLOOR_ITEM.get(), 16));
        give(p, new ItemStack(WFRegistry.RUNE_MINE_ITEM.get(), 8));
        give(p, new ItemStack(WFRegistry.FLAME_VENT_ITEM.get(), 4));
    }

    private static void give(Player p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }
}
