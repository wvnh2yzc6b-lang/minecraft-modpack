package com.warfront.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.FactionData;
import com.warfront.faction.Race;
import com.warfront.item.CommanderBatonItem;
import com.warfront.registry.WFRegistry;
import com.warfront.world.GameEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class WFCommands {
    private WFCommands() {}

    private static final SuggestionProvider<CommandSourceStack> RACES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Race.values()).map(Race::id), b);
    private static final SuggestionProvider<CommandSourceStack> FACTIONS = (ctx, b) ->
            SharedSuggestionProvider.suggest(FactionData.get(ctx.getSource().getServer()).all().stream().map(f -> f.name), b);
    private static final SuggestionProvider<CommandSourceStack> COLORS = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(ChatFormatting.values()).filter(ChatFormatting::isColor)
                    .map(ChatFormatting::getName), b);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("warfront")
                .then(Commands.literal("race")
                        .executes(WFCommands::showRace)
                        .then(Commands.argument("race", StringArgumentType.word()).suggests(RACES)
                                .executes(WFCommands::setRace)))
                .then(Commands.literal("warband").requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            boolean ok = GameEvents.trySpawnWarband(ctx.getSource().getPlayerOrException());
                            if (!ok) ctx.getSource().sendFailure(Component.literal("Could not find a spot for a warband."));
                            return ok ? 1 : 0;
                        }))
                .then(Commands.literal("help").executes(WFCommands::help)));

        d.register(Commands.literal("army").executes(WFCommands::army));

        d.register(Commands.literal("faction")
                .executes(ctx -> info(ctx, null))
                .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.word())
                        .executes(WFCommands::create)))
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
                        .executes(WFCommands::invite)))
                .then(Commands.literal("join").then(Commands.argument("name", StringArgumentType.word()).suggests(FACTIONS)
                        .executes(WFCommands::join)))
                .then(Commands.literal("leave").executes(WFCommands::leave))
                .then(Commands.literal("info").executes(ctx -> info(ctx, null))
                        .then(Commands.argument("name", StringArgumentType.word()).suggests(FACTIONS)
                                .executes(ctx -> info(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list").executes(WFCommands::list))
                .then(Commands.literal("color").then(Commands.argument("color", StringArgumentType.word()).suggests(COLORS)
                        .executes(WFCommands::color)))
                .then(Commands.literal("war").then(Commands.argument("name", StringArgumentType.word()).suggests(FACTIONS)
                        .executes(WFCommands::war)))
                .then(Commands.literal("peace").then(Commands.argument("name", StringArgumentType.word()).suggests(FACTIONS)
                        .executes(WFCommands::peace)))
                .then(Commands.literal("ally").then(Commands.argument("name", StringArgumentType.word()).suggests(FACTIONS)
                        .executes(WFCommands::ally))));
    }

    // ---------------------------------------------------------------- race

    private static int showRace(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        Race race = Race.byId(p.getData(WFRegistry.RACE));
        if (race == null) {
            GameEvents.sendRacePrompt(p);
        } else {
            ok(ctx, Component.literal("You are of the " + race.displayName() + " race. " + race.description)
                    .withStyle(race.color));
        }
        return 1;
    }

    private static int setRace(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        Race race = Race.byId(StringArgumentType.getString(ctx, "race"));
        if (race == null) return fail(ctx, "Unknown race. Choose human, elf, dwarf or orc.");
        Race current = Race.byId(p.getData(WFRegistry.RACE));
        if (current != null && !ctx.getSource().hasPermission(2)) {
            return fail(ctx, "Your lineage is already set: " + current.displayName() + ". (An operator can change it.)");
        }
        p.setData(WFRegistry.RACE, race.id());
        race.apply(p);
        com.warfront.network.RaceSync.broadcast(p);
        p.setHealth(p.getMaxHealth());
        ok(ctx, Component.literal("You are now of the " + race.displayName() + " race. " + race.description)
                .withStyle(race.color, ChatFormatting.BOLD));
        if (current == null && race == Race.HIVE) {
            com.warfront.world.HiveAdaptation.sendToCave(p);
            ok(ctx, Component.literal("The Hive keeps to the deep places. Your home is here, under the earth: you are "
                    + "stronger below Y=" + com.warfront.world.HiveAdaptation.DEPTH + " and weaker in sunlight.")
                    .withStyle(race.color, ChatFormatting.ITALIC));
        }
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        String[] lines = {
                "== Warfront ==",
                "/warfront race <race>  choose human, elf, dwarf or orc",
                "/army  status of your army",
                "/faction create|invite|join|leave|info|list|color|war|peace|ally",
                "Commander's Baton: right-click Follow/Hold/Charge, sneak to change formation",
                "Right-click your soldier with armor or a weapon to equip it",
                "Place a War Standard to found a stronghold; defend it from sieges with towers",
                "War Horn: call the next siege wave early. Kill raiders for War Marks."
        };
        for (String l : lines) ok(ctx, Component.literal(l).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    // ---------------------------------------------------------------- army

    private static int army(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        List<SoldierEntity> army = CommanderBatonItem.armyOf(p);
        if (army.isEmpty()) return fail(ctx, "You command no soldiers nearby. Use recruit contracts to raise an army.");
        Map<SoldierRole, Integer> counts = new EnumMap<>(SoldierRole.class);
        float health = 0, morale = 0;
        for (SoldierEntity s : army) {
            counts.merge(s.getRole(), 1, Integer::sum);
            health += s.getHealth() / s.getMaxHealth();
            morale += s.getMorale();
        }
        SoldierEntity any = army.get(0);
        ok(ctx, Component.literal("Your army: " + army.size() + " soldiers. " + any.getOrder().title + ", "
                + any.getFormation().title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        ok(ctx, Component.literal(counts.entrySet().stream().map(e -> e.getValue() + "x " + com.warfront.army.UnitNames.of(any.getRace(), e.getKey()))
                .collect(Collectors.joining(", "))).withStyle(ChatFormatting.YELLOW));
        ok(ctx, Component.literal(String.format("Average health %.0f%%, morale %.0f", 100 * health / army.size(),
                morale / army.size())).withStyle(ChatFormatting.GRAY));
        return army.size();
    }

    // ---------------------------------------------------------------- factions

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        FactionData data = data(ctx);
        String name = StringArgumentType.getString(ctx, "name");
        if (!name.matches("[A-Za-z0-9_]{2,16}")) return fail(ctx, "Faction names are 2-16 letters, digits or _.");
        if (data.factionOf(p.getUUID()) != null) return fail(ctx, "Leave your current faction first.");
        if (data.byName(name) != null) return fail(ctx, "That faction already exists.");
        data.create(name, p.getUUID());
        broadcast(ctx.getSource().getServer(), Component.literal(p.getName().getString() + " founded the faction "
                + name.toLowerCase() + "!").withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static int invite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        FactionData data = data(ctx);
        FactionData.Faction f = data.factionOf(p.getUUID());
        if (f == null) return fail(ctx, "You are not in a faction.");
        if (!f.leader.equals(p.getUUID())) return fail(ctx, "Only the faction leader can invite.");
        f.invites.add(target.getUUID());
        data.setDirty();
        target.sendSystemMessage(Component.literal(p.getName().getString() + " invites you to join " + f.name
                + ". ").withStyle(ChatFormatting.YELLOW).append(Component.literal("[Join]").withStyle(s -> s
                .withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/faction join " + f.name)))));
        ok(ctx, Component.literal("Invited " + target.getName().getString() + ".").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int join(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        FactionData data = data(ctx);
        FactionData.Faction f = data.byName(StringArgumentType.getString(ctx, "name"));
        if (f == null) return fail(ctx, "No such faction.");
        if (!f.invites.contains(p.getUUID())) return fail(ctx, "You need an invitation from " + f.name + "'s leader.");
        data.join(f, p.getUUID());
        broadcast(ctx.getSource().getServer(), Component.literal(p.getName().getString() + " has sworn allegiance to "
                + f.name + ".").withStyle(f.color));
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        FactionData.Faction f = data(ctx).leave(p.getUUID());
        if (f == null) return fail(ctx, "You are not in a faction.");
        ok(ctx, Component.literal("You left " + f.name + ".").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        FactionData data = data(ctx);
        FactionData.Faction f = name != null ? data.byName(name)
                : data.factionOf(ctx.getSource().getPlayerOrException().getUUID());
        if (f == null) return fail(ctx, name != null ? "No such faction." : "You are not in a faction. /faction create <name>");
        MinecraftServer server = ctx.getSource().getServer();
        ok(ctx, Component.literal("== " + f.name + " ==").withStyle(f.color, ChatFormatting.BOLD));
        ok(ctx, Component.literal("Leader: " + playerName(server, f.leader) + "  Members: " + f.members.stream()
                .map(u -> playerName(server, u)).collect(Collectors.joining(", "))));
        ok(ctx, Component.literal("Allies: " + names(f.allies)).withStyle(ChatFormatting.GREEN));
        ok(ctx, Component.literal("At war with: " + names(f.enemies)).withStyle(ChatFormatting.RED));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        FactionData data = data(ctx);
        if (data.all().isEmpty()) return fail(ctx, "No factions yet. Found one with /faction create <name>.");
        for (FactionData.Faction f : data.all()) {
            ok(ctx, Component.literal(f.name + " (" + f.members.size() + " members)").withStyle(f.color));
        }
        return data.all().size();
    }

    private static int color(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FactionData.Faction f = leaderFaction(ctx);
        if (f == null) return 0;
        ChatFormatting c = ChatFormatting.getByName(StringArgumentType.getString(ctx, "color"));
        if (c == null || !c.isColor()) return fail(ctx, "Unknown color.");
        f.color = c;
        data(ctx).setDirty();
        ok(ctx, Component.literal("Faction color set.").withStyle(c));
        return 1;
    }

    private static int war(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FactionData.Faction f = leaderFaction(ctx);
        if (f == null) return 0;
        FactionData.Faction other = otherFaction(ctx, f);
        if (other == null) return 0;
        data(ctx).declareWar(f, other);
        broadcast(ctx.getSource().getServer(), Component.literal(f.name + " has declared WAR on " + other.name + "!")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        return 1;
    }

    private static int peace(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FactionData.Faction f = leaderFaction(ctx);
        if (f == null) return 0;
        FactionData.Faction other = otherFaction(ctx, f);
        if (other == null) return 0;
        data(ctx).makePeace(f, other);
        broadcast(ctx.getSource().getServer(), Component.literal(f.name + " and " + other.name + " are at peace.")
                .withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static int ally(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FactionData.Faction f = leaderFaction(ctx);
        if (f == null) return 0;
        FactionData.Faction other = otherFaction(ctx, f);
        if (other == null) return 0;
        if (data(ctx).proposeAlliance(f, other)) {
            broadcast(ctx.getSource().getServer(), Component.literal(f.name + " and " + other.name
                    + " have forged an alliance!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            ok(ctx, Component.literal("Alliance proposed. " + other.name + "'s leader must run /faction ally " + f.name)
                    .withStyle(ChatFormatting.YELLOW));
            ServerPlayer leader = ctx.getSource().getServer().getPlayerList().getPlayer(other.leader);
            if (leader != null) leader.sendSystemMessage(Component.literal(f.name + " proposes an alliance. Accept with "
                    + "/faction ally " + f.name).withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    // ---------------------------------------------------------------- helpers

    private static FactionData data(CommandContext<CommandSourceStack> ctx) {
        return FactionData.get(ctx.getSource().getServer());
    }

    private static FactionData.Faction leaderFaction(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        FactionData.Faction f = data(ctx).factionOf(p.getUUID());
        if (f == null) {
            fail(ctx, "You are not in a faction.");
            return null;
        }
        if (!f.leader.equals(p.getUUID())) {
            fail(ctx, "Only the faction leader can do that.");
            return null;
        }
        return f;
    }

    private static FactionData.Faction otherFaction(CommandContext<CommandSourceStack> ctx, FactionData.Faction self) {
        FactionData.Faction other = data(ctx).byName(StringArgumentType.getString(ctx, "name"));
        if (other == null) {
            fail(ctx, "No such faction.");
            return null;
        }
        if (other == self) {
            fail(ctx, "That is your own faction.");
            return null;
        }
        return other;
    }

    private static String names(java.util.Set<String> keys) {
        if (keys.isEmpty()) return "none";
        return keys.stream().map(k -> k.startsWith("f:") ? k.substring(2) : k).collect(Collectors.joining(", "));
    }

    private static String playerName(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) return p.getName().getString();
        return server.getProfileCache() != null
                ? server.getProfileCache().get(id).map(g -> g.getName()).orElse("?") : "?";
    }

    private static void broadcast(MinecraftServer server, Component msg) {
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    private static void ok(CommandContext<CommandSourceStack> ctx, Component msg) {
        ctx.getSource().sendSuccess(() -> msg, false);
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
