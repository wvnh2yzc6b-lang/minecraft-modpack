package com.warfront.world;

import com.warfront.Warfront;
import com.warfront.army.SoldierRole;
import com.warfront.command.WFCommands;
import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Warfront.MODID)
public final class GameEvents {
    private GameEvents() {}

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        WFCommands.register(event.getDispatcher());
    }

    // ------------------------------------------------------------ players

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        Race race = Race.byId(player.getData(WFRegistry.RACE));
        if (race != null) {
            race.apply(player);
        } else {
            sendRacePrompt(player);
        }
        if (WFConfig.STARTER_KIT.get() && !player.getData(WFRegistry.STARTER_KIT)) {
            player.setData(WFRegistry.STARTER_KIT, true);
            give(player, new ItemStack(WFRegistry.COMMANDER_BATON.get()));
            give(player, new ItemStack(WFRegistry.CONTRACTS.get(SoldierRole.SHIELDBEARER).get(), 2));
            give(player, new ItemStack(WFRegistry.CONTRACTS.get(SoldierRole.SWORDSMAN).get(), 1));
            give(player, new ItemStack(WFRegistry.CONTRACTS.get(SoldierRole.ARCHER).get(), 2));
            give(player, new ItemStack(WFRegistry.CONTRACTS.get(SoldierRole.HEALER).get(), 1));
            player.sendSystemMessage(Component.literal("You have been granted a Commander's Baton and recruit contracts. "
                    + "Raise an army, place a War Standard, and hold it against the hordes.")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Race race = Race.byId(event.getEntity().getData(WFRegistry.RACE));
        if (race != null) race.apply(event.getEntity());
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    public static void sendRacePrompt(Player player) {
        MutableComponent line = Component.literal("Choose your race: ").withStyle(ChatFormatting.YELLOW);
        for (Race r : Race.values()) {
            line.append(Component.literal("[" + r.displayName() + "]").withStyle(style -> style
                    .withColor(r.color).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warfront race " + r.id()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(r.description)))));
            line.append(Component.literal(" "));
        }
        player.sendSystemMessage(line);
        player.sendSystemMessage(Component.literal("Your race shapes you and every soldier you recruit. Choose wisely; "
                + "it is permanent.").withStyle(ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------ combat

    /** No friendly fire between allied soldiers, players and towers. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (WFConfig.FRIENDLY_FIRE.get()) return;
        Entity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        MinecraftServer server = victim.level().getServer();
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();

        if (attacker != null && attacker != victim && (attacker instanceof SoldierEntity || victim instanceof SoldierEntity)) {
            if (Factions.relation(attacker, victim) == Relation.ALLY) event.setCanceled(true);
            return;
        }
        if (attacker == null && direct != null && direct.getPersistentData().contains(Factions.PROJECTILE_TAG)) {
            String key = direct.getPersistentData().getString(Factions.PROJECTILE_TAG);
            if (Factions.relation(server, key, Factions.keyOf(server, victim)) == Relation.ALLY) event.setCanceled(true);
        }
    }

    /** Hostile monsters treat soldiers as fair game, like villagers. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof Monster monster && !(monster instanceof Creeper)) {
            monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, SoldierEntity.class, true));
        }
    }

    // ------------------------------------------------------------ roaming warbands

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 200 != 0 || !WFConfig.WARBANDS_ENABLED.get()) return;
        double chance = WFConfig.WARBAND_CHANCE.get() * 200.0 / WFConfig.WARBAND_INTERVAL.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.level().dimension() != Level.OVERWORLD) continue;
            if (player.getRandom().nextDouble() >= chance) continue;
            trySpawnWarband(player);
        }
    }

    public static boolean trySpawnWarband(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long nearby = level.getEntitiesOfClass(SoldierEntity.class, player.getBoundingBox().inflate(96),
                s -> s.getOwnerUUID() == null).size();
        if (nearby >= 16) return false;

        float angle = player.getRandom().nextFloat() * Mth.TWO_PI;
        double dist = 40 + player.getRandom().nextInt(8);
        int x = Mth.floor(player.getX() + Mth.cos(angle) * dist);
        int z = Mth.floor(player.getZ() + Mth.sin(angle) * dist);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return false;
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (!level.getFluidState(ground.below()).isEmpty()) return false;

        NpcFaction faction = NpcFaction.values()[player.getRandom().nextInt(NpcFaction.values().length)];
        int tier = 1 + (int) (level.getCurrentDifficultyAt(player.blockPosition()).getEffectiveDifficulty() / 2);
        WarbandSpawner.spawn(level, faction, WarbandSpawner.raidComposition(player.getRandom()), ground,
                player.position(), null, tier);
        player.sendSystemMessage(Component.literal("War drums echo in the distance... a " + faction.displayName
                + " warband is marching on you.").withStyle(faction.color, ChatFormatting.ITALIC));
        return true;
    }

}
