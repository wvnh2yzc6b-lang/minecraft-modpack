package com.warfront.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.warfront.army.SoldierRole;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Race;
import com.warfront.test.TestActions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * {@code /wftest}: the Test Panel as commands. Operators only (cheats on in singleplayer); every subcommand but
 * {@code on} also needs test mode on. Each one runs the same {@link TestActions} method as its panel button.
 */
public final class TestCommands {
    private TestCommands() {}

    private static final SuggestionProvider<CommandSourceStack> RACES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Race.values()).map(Race::id), b);
    private static final SuggestionProvider<CommandSourceStack> ROLES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(SoldierRole.values()).filter(r -> !r.retired()).map(SoldierRole::id), b);
    private static final SuggestionProvider<CommandSourceStack> SIDES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of("friendly"),
                    Arrays.stream(NpcFaction.values()).map(f -> f.name().toLowerCase(Locale.ROOT))), b);
    private static final SuggestionProvider<CommandSourceStack> FACTIONS = (ctx, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(NpcFaction.values()).map(f -> f.name().toLowerCase(Locale.ROOT)), b);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("wftest").requires(s -> s.hasPermission(2))
                .executes(TestCommands::help)
                .then(Commands.literal("on").executes(ctx -> toggle(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> toggle(ctx, false)))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("race", StringArgumentType.word()).suggests(RACES)
                                .then(Commands.argument("role", StringArgumentType.word()).suggests(ROLES)
                                        .executes(ctx -> run(ctx, "spawn", str(ctx, "race"), str(ctx, "role")))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 10))
                                                .executes(ctx -> run(ctx, "spawn", str(ctx, "race"), str(ctx, "role"), num(ctx, "count")))
                                                .then(Commands.argument("side", StringArgumentType.word()).suggests(SIDES)
                                                        .executes(ctx -> run(ctx, "spawn", str(ctx, "race"), str(ctx, "role"),
                                                                num(ctx, "count"), str(ctx, "side")))
                                                        .then(Commands.argument("rank", IntegerArgumentType.integer(0, 4))
                                                                .executes(ctx -> run(ctx, "spawn", str(ctx, "race"), str(ctx, "role"),
                                                                        num(ctx, "count"), str(ctx, "side"), num(ctx, "rank")))))))))
                .then(Commands.literal("army")
                        .then(simple("heal", "army", "heal"))
                        .then(simple("kill", "army", "kill"))
                        .then(simple("dismiss", "army", "dismiss"))
                        .then(simple("revive", "army", "revive")))
                .then(Commands.literal("base")
                        .then(Commands.literal("level").then(Commands.argument("level", IntegerArgumentType.integer(0, 5))
                                .executes(ctx -> run(ctx, "base", "level", num(ctx, "level")))))
                        .then(simple("fill", "base", "fill"))
                        .then(Commands.literal("infinite")
                                .then(simple("on", "base", "infinite", "on"))
                                .then(simple("off", "base", "infinite", "off")))
                        .then(simple("starter", "base", "starter"))
                        .then(simple("bounties", "base", "bounties"))
                        .then(simple("merchant", "base", "merchant"))
                        .then(Commands.literal("camp").executes(ctx -> run(ctx, "base", "camp"))
                                .then(simple("rescue", "base", "camp", "rescue")))
                        .then(Commands.literal("food")
                                .then(simple("fill", "base", "food", "fill"))
                                .then(simple("empty", "base", "food", "empty"))))
                .then(Commands.literal("siege")
                        .then(Commands.literal("start").executes(ctx -> run(ctx, "siege", "start"))
                                .then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTIONS)
                                        .executes(ctx -> run(ctx, "siege", "start", str(ctx, "faction")))))
                        .then(Commands.literal("wave").then(Commands.argument("wave", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> run(ctx, "siege", "wave", num(ctx, "wave")))))
                        .then(simple("pause", "siege", "pause"))
                        .then(simple("resume", "siege", "resume"))
                        .then(simple("end", "siege", "end"))
                        .then(Commands.literal("outpost").executes(ctx -> run(ctx, "siege", "outpost"))
                                .then(simple("destroy", "siege", "outpost", "destroy"))))
                .then(Commands.literal("player")
                        .then(Commands.literal("race").then(Commands.argument("race", StringArgumentType.word()).suggests(RACES)
                                .executes(ctx -> run(ctx, "player", "race", str(ctx, "race")))))
                        .then(simple("fill", "player", "fill"))
                        .then(simple("day", "player", "day"))
                        .then(simple("night", "player", "night"))
                        .then(simple("clear", "player", "clear"))
                        .then(simple("god", "player", "god"))
                        .then(simple("kit", "player", "kit")))
                .then(Commands.literal("difficulty").then(Commands.argument("preset", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(com.warfront.war.WarState.Preset.values())
                                .map(com.warfront.war.WarState.Preset::id), b))
                        .executes(ctx -> run(ctx, "difficulty", str(ctx, "preset")))))
                .then(Commands.literal("raid")
                        .then(simple("warn", "raid", "warn"))
                        .then(simple("siege", "raid", "siege"))
                        .then(simple("grace", "raid", "grace"))
                        .then(simple("recall", "raid", "recall")))
                .then(Commands.literal("war")
                        .then(simple("status", "war", "status"))
                        .then(Commands.literal("meter").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTIONS)
                                .then(Commands.argument("n", IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> run(ctx, "war", "meter", str(ctx, "faction"), num(ctx, "n"))))))
                        .then(Commands.literal("map").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTIONS)
                                .executes(ctx -> run(ctx, "war", "map", str(ctx, "faction")))))
                        .then(Commands.literal("tp").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTIONS)
                                .executes(ctx -> run(ctx, "war", "tp", str(ctx, "faction")))))
                        .then(Commands.literal("place").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTIONS)
                                .executes(ctx -> run(ctx, "war", "place", str(ctx, "faction")))))
                        .then(simple("reset", "war", "reset"))
                        .then(Commands.literal("beaten").then(Commands.argument("which", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Stream.concat(Stream.of("all", "none"),
                                        Arrays.stream(NpcFaction.values()).map(f -> f.name().toLowerCase(Locale.ROOT))), b))
                                .executes(ctx -> run(ctx, "war", "beaten", str(ctx, "which"))))))
                .then(Commands.literal("advisor")
                        .then(Commands.literal("step").then(Commands.argument("step", IntegerArgumentType.integer(0, 7))
                                .executes(ctx -> run(ctx, "advisor", "step", num(ctx, "step")))))
                        .then(simple("respawn", "advisor", "respawn"))
                        .then(Commands.literal("disguise").then(Commands.argument("race", StringArgumentType.word()).suggests(RACES)
                                .executes(ctx -> run(ctx, "advisor", "disguise", str(ctx, "race")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> simple(String literal, String... args) {
        return Commands.literal(literal).executes(ctx -> run(ctx, args));
    }

    private static String str(CommandContext<CommandSourceStack> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    private static String num(CommandContext<CommandSourceStack> ctx, String name) {
        return Integer.toString(IntegerArgumentType.getInteger(ctx, name));
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx, boolean on) throws CommandSyntaxException {
        TestActions.Result r = TestActions.setTestMode(ctx.getSource().getPlayerOrException(), on);
        ctx.getSource().sendSuccess(() -> Component.literal(r.message()).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int run(CommandContext<CommandSourceStack> ctx, String... args) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        if (!TestActions.isOn(p)) {
            ctx.getSource().sendFailure(Component.literal("Test mode is off. Turn it on with /wftest on."));
            return 0;
        }
        TestActions.Result r = TestActions.run(p, new ArrayList<>(List.of(args)));
        if (r.ok()) ctx.getSource().sendSuccess(() -> Component.literal(r.message()).withStyle(ChatFormatting.GREEN), false);
        else ctx.getSource().sendFailure(Component.literal(r.message()));
        return r.ok() ? 1 : 0;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        String[] lines = {
                "== Warfront test mode ==",
                "/wftest on|off  then press F8 for the Test Panel",
                "/wftest spawn <race> <role> [count] [friendly|faction] [rank 0-4]",
                "/wftest army heal|kill|dismiss|revive",
                "/wftest base level <0-5>|fill|infinite on|off|starter|food fill|empty|bounties|camp [rescue]|merchant",
                "/wftest siege start [faction]|wave <n>|pause|resume|end|outpost [destroy]",
                "/wftest player race <race>|fill|day|night|clear|god|kit",
                "/wftest difficulty <easy|normal|hard|warlord>",
                "/wftest raid warn|siege|grace|recall",
                "/wftest war status|meter <faction> <n>|map <faction>|beaten <faction|all|none>|tp <faction>|place <faction>|reset",
                "/wftest advisor step <0-7>|respawn|disguise <race>"
        };
        for (String l : lines) ctx.getSource().sendSuccess(() -> Component.literal(l).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }
}
