package com.warfront.world;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.AbstractArrow;
import com.warfront.combat.Rage;
import com.warfront.combat.Souls;
import com.warfront.Warfront;
import com.warfront.command.WFCommands;
import com.warfront.config.WFConfig;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.faction.Relation;
import com.warfront.registry.WFRegistry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
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
        com.warfront.command.TestCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        com.warfront.mana.ManaNetwork.clear();
    }

    // ------------------------------------------------------------ players

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp) {
            com.warfront.network.RaceSync.broadcast(sp);
            com.warfront.network.RaceSync.sendAllTo(sp);
            com.warfront.test.TestActions.sync(sp);
            com.warfront.test.TestActions.reapplyGod(sp);
        }
        Race race = Race.byId(player.getData(WFRegistry.RACE));
        if (race != null) {
            race.apply(player);
        } else {
            sendRacePrompt(player);
        }
        if (WFConfig.STARTER_KIT.get() && !player.getData(WFRegistry.STARTER_KIT)) {
            player.setData(WFRegistry.STARTER_KIT, true);
            give(player, new ItemStack(WFRegistry.COMMANDER_BATON.get()));
            give(player, new ItemStack(WFRegistry.SUMMONING_ALTAR_ITEM.get()));
            give(player, new ItemStack(WFRegistry.MANA_BRAZIER_ITEM.get(), 4));
            give(player, new ItemStack(WFRegistry.MANA_WELL_ITEM.get()));
            give(player, new ItemStack(WFRegistry.MANA_SHARD.get(), 24));
            give(player, new ItemStack(WFRegistry.MANABLOOM_SEEDS.get(), 4));
            player.sendSystemMessage(Component.literal("You have been granted a Commander's Baton, a Summoning Altar with four Mana Braziers, "
                    + "a Mana Well and Mana Shards. Set the altar on a 3x3 floor of bricks or stone bricks with a brazier two blocks out on "
                    + "each diagonal, place the well within 16 blocks and fill it with shards, then right-click the altar to summon troops. "
                    + "Grow Manabloom and mine Mana Ore to keep your wells full; they also power your towers.")
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Race race = Race.byId(event.getEntity().getData(WFRegistry.RACE));
        if (race != null) race.apply(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer sp) com.warfront.test.TestActions.reapplyGod(sp);
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
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(r.description + "\n" + r.magicLine())))));
            line.append(Component.literal(" "));
        }
        player.sendSystemMessage(line);
        player.sendSystemMessage(Component.literal("Your race shapes you and every soldier you recruit. Choose wisely; "
                + "it is permanent.").withStyle(ChatFormatting.GRAY));
    }

    // ------------------------------------------------------------ combat

    /** Racial traits, then no friendly fire between allied soldiers, players and towers. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        MinecraftServer server = victim.level().getServer();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();

        Race victimRace = Race.of(victim);
        if (victimRace == Race.DEMON && source.is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            victim.clearFire();
            return;
        }
        if (source.is(DamageTypeTags.IS_FALL)
                && (victimRace == Race.ANGEL || victim instanceof SoldierEntity flier && flier.getBody().winged)) {
            event.setCanceled(true);
            return;
        }

        if (!WFConfig.FRIENDLY_FIRE.get()) {
            if (attacker != null && attacker != victim
                    && (attacker instanceof SoldierEntity || victim instanceof SoldierEntity)) {
                if (Factions.relation(attacker, victim) == Relation.ALLY) {
                    event.setCanceled(true);
                    return;
                }
            } else if (attacker == null && direct != null && direct.getPersistentData().contains(Factions.PROJECTILE_TAG)) {
                String key = direct.getPersistentData().getString(Factions.PROJECTILE_TAG);
                if (Factions.relation(server, key, Factions.keyOf(server, victim)) == Relation.ALLY) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        if (attacker instanceof LivingEntity living) {
            Race attackerRace = Race.of(living);
            if (attackerRace == Race.ANGEL && (victim.getType().is(EntityTypeTags.UNDEAD) || victimRace == Race.DEMON)) {
                event.setAmount(event.getAmount() * 1.5F);
            } else if (living instanceof SoldierEntity berserker && berserker.isBerserk()) {
                event.setAmount(event.getAmount() * 1.5F);
            } else if (attackerRace == Race.HIVE && living instanceof SoldierEntity hive) {
                long pack = hive.nearbyAllies(6, SoldierEntity.class).stream()
                        .filter(s -> s.getRace() == Race.HIVE).count();
                event.setAmount(event.getAmount() + Math.min(3.0F, pack * 0.5F));
            }
            if (attackerRace == Race.ELF && direct instanceof AbstractArrow) {
                // Elven archery: arrows hit harder, and a long shot marks the target for everyone to see.
                float bonus = 1.25F;
                if (living.distanceTo(victim) >= ELF_LONG_SHOT) {
                    bonus *= 1.25F;
                    victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), living);
                }
                event.setAmount(event.getAmount() * bonus);
            }
            if (attackerRace == Race.ORC && living != victim) Rage.gain(living, Rage.ON_HIT);
            if (attackerRace == Race.DEMON && direct == living && living != victim) {
                event.setAmount(event.getAmount() + Souls.burst(living, victim));
            }
        }
        if (victimRace == Race.ORC && attacker != null && attacker != victim) Rage.gain(victim, Rage.ON_HURT);
        if (Rage.isFrenzied(victim)) event.setAmount(event.getAmount() * 1.15F);
    }

    /** Demons take the soul of whatever they kill. */
    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (event.getSource().getEntity() instanceof LivingEntity killer && Race.of(killer) == Race.DEMON) {
            Souls.harvest(killer, victim);
        }
    }

    /** Blocks away an elf's arrow must fly to count as a long shot. */
    public static final double ELF_LONG_SHOT = 16.0;
    /** How much faster elven arrows fly. */
    public static final double ELF_ARROW_SPEED = 1.2;

    /** Hostile monsters treat soldiers as fair game, like villagers. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity owner
                && Race.of(owner) == Race.ELF && !arrow.getPersistentData().getBoolean("warfront_elf_shot")) {
            arrow.getPersistentData().putBoolean("warfront_elf_shot", true);
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(ELF_ARROW_SPEED));
        }
        if (event.getEntity() instanceof Monster monster && !(monster instanceof Creeper)) {
            monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, SoldierEntity.class, true));
        }
    }

    // ------------------------------------------------------------ wing flight

    /** Powered wing flight: thrust on the side that simulates the player, hunger on the server. */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.isLocalPlayer() && com.warfront.flight.ManaGliderItem.gliding(player)) {
            com.warfront.flight.ManaGliderItem.glide(player);
        }
        if (!player.isFallFlying() || !com.warfront.flight.WingFlight.hasWings(player)
                || com.warfront.flight.WingFlight.wearsWorkingElytra(player)) return;
        if (player.isLocalPlayer()) {
            com.warfront.flight.WingFlight.applyThrust(player);
        } else if (!player.level().isClientSide && !player.getAbilities().instabuild
                && player.getFoodData().getFoodLevel() > 6) {
            player.causeFoodExhaustion(com.warfront.flight.WingFlight.EXHAUSTION);
        }
    }

    /** A Mana Shard used while flying a Mana Glider burns for a burst of speed. */
    @SubscribeEvent
    public static void onRightClickItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!held.is(WFRegistry.MANA_SHARD.get()) || !com.warfront.flight.ManaGliderItem.gliding(player)) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (!player.level().isClientSide) com.warfront.flight.ManaGliderItem.boost(player, held);
    }

    // ------------------------------------------------------------ roaming warbands

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 60 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (Race.of(player) == Race.ANGEL && player.getHealth() < player.getMaxHealth()) player.heal(1F);
                if (Race.of(player) == Race.HIVE) HiveAdaptation.apply(player, 80);
            }
        }
        if (server.getTickCount() % 200 != 0 || !WFConfig.WARBANDS_ENABLED.get()) return;
        double chance = WFConfig.WARBAND_CHANCE.get() * 200.0 / WFConfig.WARBAND_INTERVAL.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.level().dimension() != Level.OVERWORLD && player.level().dimension() != Level.NETHER) continue;
            if (player.getRandom().nextDouble() >= chance) continue;
            trySpawnWarband(player);
        }
    }

    /** Finds a standable spot in the Nether near the player's height (the heightmap would hit the roof). */
    @org.jetbrains.annotations.Nullable
    private static BlockPos findNetherGround(ServerLevel level, int x, int y, int z) {
        for (int dy = 8; dy >= -8; dy--) {
            BlockPos p = new BlockPos(x, y + dy, z);
            if (level.getBlockState(p.below()).isSolid() && level.getBlockState(p).isAir()
                    && level.getBlockState(p.above()).isAir()) return p;
        }
        return null;
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
        BlockPos ground = level.dimension() == Level.NETHER
                ? findNetherGround(level, x, player.getBlockY(), z)
                : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (ground == null || !level.getFluidState(ground.below()).isEmpty()) return false;

        NpcFaction faction = NpcFaction.pick(level.getBiome(ground), level, player.getRandom());
        int tier = 1 + (int) (level.getCurrentDifficultyAt(player.blockPosition()).getEffectiveDifficulty() / 2);
        WarbandSpawner.spawn(level, faction, WarbandSpawner.raidComposition(player.getRandom(), faction), ground,
                player.position(), null, tier);
        player.sendSystemMessage(Component.literal(faction == NpcFaction.THE_SWARM
                ? "The ground trembles beneath you... a Swarm brood is tunneling toward you."
                : "War drums echo in the distance... a " + faction.displayName + " warband is marching on you.")
                .withStyle(faction.color, ChatFormatting.ITALIC));
        return true;
    }

}
