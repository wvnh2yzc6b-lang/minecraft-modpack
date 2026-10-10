package com.warfront.skill;

import com.warfront.Warfront;
import com.warfront.faction.Race;
import com.warfront.network.SkillStatePayload;
import com.warfront.registry.WFRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The player skill tree: Renown levels give points, points buy nodes (each needs one of its parents), learned
 * passives apply their attribute modifiers, and up to four learned active skills sit in the skill slots.
 */
public final class Skills {
    private Skills() {}

    /** A result for the player (and the tests). */
    public record Result(boolean ok, String message) {
        static Result ok(String m) {
            return new Result(true, m);
        }

        static Result fail(String m) {
            return new Result(false, m);
        }
    }

    public static SkillState state(Player p) {
        return p.getData(WFRegistry.SKILLS);
    }

    private static void save(Player p, SkillState s) {
        p.setData(WFRegistry.SKILLS, s);
    }

    /** Whether the player has learned a node. On the client this only knows the local player. */
    public static boolean has(Player p, String node) {
        if (p.level().isClientSide) return p.isLocalPlayer() && ClientSkills.has(node);
        return state(p).has(node);
    }

    @Nullable
    public static SkillNode learned(Player p, String node) {
        return has(p, node) ? SkillRegistry.node(node) : null;
    }

    /** A learned node's tuning number, or 0 when the node isn't learned. */
    public static double value(Player p, String node, String key) {
        SkillNode n = learned(p, node);
        return n == null ? 0 : n.value(key, 0);
    }

    // ------------------------------------------------------------------ Renown

    public static void gainRenown(Player p, int amount) {
        if (amount <= 0 || p.level().isClientSide) return;
        SkillState s = state(p);
        int before = s.level();
        s.renown += amount;
        save(p, s);
        int after = s.level();
        if (after > before) {
            com.warfront.alert.Alerts.toast(p, "renown_level", "Renown level " + after,
                    "+" + (after - before) + " skill point" + (after - before > 1 ? "s" : "") + ". Press K for the Skill Tree.");
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        sync(p);
    }

    public static void bonusPoint(Player p, String why) {
        SkillState s = state(p);
        s.bonusPoints++;
        save(p, s);
        p.displayClientMessage(Component.literal("+1 skill point: " + why).withStyle(ChatFormatting.GOLD), false);
        sync(p);
    }

    // ------------------------------------------------------------------ learning

    /** What a node costs this player: the first 3 nodes of a path their race favors cost nothing. */
    public static int costFor(Player p, SkillNode n) {
        SkillPath path = SkillRegistry.path(n.path());
        if (path != null && path.favors(Race.of(p))) {
            SkillState s = state(p);
            long inPath = s.unlocked.stream().map(SkillRegistry::node).filter(x -> x != null && x.path().equals(n.path())).count();
            if (inPath < 3) return 0;
        }
        return n.cost();
    }

    public static Result learn(Player p, String id) {
        SkillNode n = SkillRegistry.node(id);
        if (n == null) return Result.fail("No skill called " + id + ".");
        SkillState s = state(p);
        if (s.has(id)) return Result.fail(n.name() + " is already learned.");
        if (!n.isRoot() && n.parents().stream().noneMatch(s::has)) {
            List<String> names = new ArrayList<>();
            for (String par : n.parents()) {
                SkillNode pn = SkillRegistry.node(par);
                names.add(pn == null ? par : pn.name());
            }
            return Result.fail(n.name() + " needs " + String.join(" or ", names) + " first.");
        }
        int cost = costFor(p, n);
        if (s.points() < cost) return Result.fail(n.name() + " costs " + cost + " point" + (cost == 1 ? "" : "s") + "; you have " + s.points() + ".");
        s.unlocked.add(id);
        s.spent += cost;
        if (n.active()) {
            int free = s.slots.indexOf("");
            if (free >= 0) s.slots.set(free, id);
        }
        save(p, s);
        refresh(p);
        sync(p);
        return Result.ok("Learned " + n.name() + (cost == 0 ? " (free: your race's path)" : "") + ".");
    }

    /** Crystals a respec costs: more the more points are spent. */
    public static int respecCost(SkillState s) {
        return s.spent <= 0 ? 0 : 2 + s.spent;
    }

    public static Result respec(Player p, boolean free) {
        SkillState s = state(p);
        if (s.unlocked.isEmpty()) return Result.fail("Nothing to refund.");
        int cost = free || p.getAbilities().instabuild ? 0 : respecCost(s);
        if (cost > 0) {
            int have = p.getInventory().countItem(WFRegistry.MANA_CRYSTAL.get());
            if (have < cost) return Result.fail("A respec costs " + cost + " Mana Crystals; you carry " + have + ".");
            int left = cost;
            for (ItemStack st : p.getInventory().items) {
                if (left <= 0) break;
                if (st.is(WFRegistry.MANA_CRYSTAL.get())) {
                    int take = Math.min(left, st.getCount());
                    st.shrink(take);
                    left -= take;
                }
            }
        }
        int refunded = s.spent;
        s.unlocked.clear();
        s.spent = 0;
        for (int i = 0; i < SkillState.SLOTS; i++) s.slots.set(i, "");
        save(p, s);
        refresh(p);
        sync(p);
        return Result.ok("Refunded " + refunded + " points" + (cost > 0 ? " for " + cost + " Mana Crystals" : "") + ".");
    }

    public static Result setSlot(Player p, int slot, String id) {
        if (slot < 0 || slot >= SkillState.SLOTS) return Result.fail("No such slot.");
        SkillState s = state(p);
        if (!id.isEmpty()) {
            SkillNode n = SkillRegistry.node(id);
            if (n == null || !n.active()) return Result.fail("Only active skills go in a slot.");
            if (!s.has(id)) return Result.fail("Learn " + n.name() + " first.");
            int was = s.slots.indexOf(id);
            if (was >= 0) s.slots.set(was, s.slots.get(slot));   // swap
        }
        s.slots.set(slot, id);
        save(p, s);
        sync(p);
        return Result.ok(id.isEmpty() ? "Slot " + (slot + 1) + " cleared." : "Slot " + (slot + 1) + ": " + SkillRegistry.node(id).name() + ".");
    }

    // ------------------------------------------------------------------ attributes

    public static ResourceLocation modifierId(SkillNode n, int i) {
        return Warfront.id("skill/" + n.id() + (i > 0 ? "_" + i : ""));
    }

    private static Optional<Holder.Reference<Attribute>> attribute(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return Optional.empty();
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(Registries.ATTRIBUTE, rl));
    }

    private static AttributeModifier.Operation operation(String op) {
        return switch (op.toLowerCase(Locale.ROOT)) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    /** Re-applies every learned passive's attribute modifiers (after learning, a respec, login or respawn). */
    public static void refresh(Player p) {
        SkillState s = state(p);
        for (SkillNode n : SkillRegistry.allNodes()) {
            for (int i = 0; i < n.attributes().size(); i++) {
                SkillNode.AttributeBonus b = n.attributes().get(i);
                ResourceLocation id = modifierId(n, i);
                boolean on = s.has(n.id());
                attribute(b.attribute()).ifPresent(a -> {
                    AttributeInstance inst = p.getAttribute(a);
                    if (inst == null) return;
                    inst.removeModifier(id);
                    if (on) inst.addTransientModifier(new AttributeModifier(id, b.amount(), operation(b.operation())));
                });
            }
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    // ------------------------------------------------------------------ active skills

    /** Cooldown multiplier: Focus and Deep Well (without Iron's Spells), Arcane Surge. */
    public static double cooldownFactor(Player p) {
        double f = 1.0;
        if (!IronsBridge.loaded()) {
            f *= 1 - value(p, "focus", "skill_cooldown");
            f *= 1 - value(p, "deep_well", "skill_cooldown");
        }
        if (SkillEffects.active(p, "arcane_surge")) f *= 0.5;
        return f;
    }

    /** Charges of an active skill ready now. */
    public static int chargesReady(Player p, SkillNode n) {
        long left = state(p).readyAt.getOrDefault(n.id(), 0L) - p.level().getGameTime();
        if (left <= 0) return n.charges();
        long per = Math.max(1, Math.round(n.cooldown() * 20 * cooldownFactor(p)));
        return (int) Math.max(0, n.charges() - (left + per - 1) / per);
    }

    /** Ticks until the next charge (0 when one is ready). */
    public static long ticksLeft(Player p, SkillNode n) {
        if (chargesReady(p, n) > 0) return 0;
        long left = state(p).readyAt.getOrDefault(n.id(), 0L) - p.level().getGameTime();
        long per = Math.max(1, Math.round(n.cooldown() * 20 * cooldownFactor(p)));
        return Math.max(0, left - (long) (n.charges() - 1) * per);
    }

    /** Uses the skill in a slot (or, from tests, by node id). */
    public static Result use(Player p, int slot, float forward, float strafe) {
        if (slot < 0 || slot >= SkillState.SLOTS) return Result.fail("No such slot.");
        String id = state(p).slots.get(slot);
        if (id.isEmpty()) return Result.fail("Skill slot " + (slot + 1) + " is empty: equip an active skill in the Skill Tree (K).");
        return useNode(p, id, forward, strafe);
    }

    public static Result useNode(Player p, String id, float forward, float strafe) {
        SkillNode n = SkillRegistry.node(id);
        SkillState s = state(p);
        if (n == null || !n.active() || !s.has(id)) return Result.fail("You haven't learned that skill.");
        boolean free = id.equals("dash") && SkillEffects.active(p, "shadow_dance");
        if (!free && chargesReady(p, n) <= 0) {
            return Result.fail(n.name() + " is ready in " + (ticksLeft(p, n) + 19) / 20 + "s.");
        }
        String failed = SkillActives.activate(p, n, forward, strafe);
        if (failed != null) return Result.fail(failed);
        long now = p.level().getGameTime();
        long per = Math.max(1, Math.round(n.cooldown() * 20 * cooldownFactor(p)));
        boolean echo = has(p, "spell_echo") && p.getRandom().nextDouble() < value(p, "spell_echo", "chance");
        if (!free && !echo) s.readyAt.put(id, Math.max(now, s.readyAt.getOrDefault(id, 0L)) + per);
        save(p, s);
        sync(p);
        return Result.ok(n.name() + (echo ? " (Spell Echo: no cooldown!)" : "") + ".");
    }

    /** Takes {@code ticks} off every running cooldown (Mana Strike and Ley Tap without Iron's Spells). */
    public static void shortenCooldowns(Player p, long ticks) {
        SkillState s = state(p);
        long now = p.level().getGameTime();
        boolean changed = false;
        for (var e : s.readyAt.entrySet()) {
            if (e.getValue() > now && !e.getKey().equals("last_stand")) {
                e.setValue(e.getValue() - ticks);
                changed = true;
            }
        }
        if (changed) save(p, s);
    }

    public static void resetCooldowns(Player p) {
        SkillState s = state(p);
        s.readyAt.clear();
        save(p, s);
        sync(p);
    }

    // ------------------------------------------------------------------ test mode

    /**
     * {@code /wftest skills ...}: renown &lt;n&gt; (add), level &lt;n&gt; (set), points &lt;n&gt; (add bonus points),
     * unlock &lt;node|all|path&gt; (free, ignoring parents), reset (free respec), ready (end all cooldowns).
     */
    public static Result test(Player p, String what, String arg) {
        SkillState s = state(p);
        int n;
        try {
            n = arg.isEmpty() ? 0 : Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            n = -1;
        }
        switch (what) {
            case "renown" -> {
                gainRenown(p, Math.max(1, n <= 0 ? 100 : n));
                return Result.ok("Renown " + state(p).renown + " (level " + state(p).level() + ").");
            }
            case "level" -> {
                s.renown = Renown.total(Math.max(0, Math.min(Renown.MAX_LEVEL, n)));
                save(p, s);
                sync(p);
                return Result.ok("Renown level " + s.level() + ", " + s.points() + " points to spend.");
            }
            case "points" -> {
                s.bonusPoints += Math.max(1, n <= 0 ? 5 : n);
                save(p, s);
                sync(p);
                return Result.ok(s.points() + " points to spend.");
            }
            case "unlock" -> {
                List<SkillNode> add = new ArrayList<>();
                SkillPath path = SkillRegistry.path(arg);
                if (arg.equals("all")) SkillRegistry.allNodes().forEach(add::add);
                else if (path != null) add.addAll(path.nodes());
                else if (SkillRegistry.node(arg) != null) add.add(SkillRegistry.node(arg));
                else return Result.fail("unlock <node|path|all>");
                int count = 0;
                for (SkillNode node : add) {
                    if (s.has(node.id())) continue;
                    s.unlocked.add(node.id());
                    count++;
                    int free = s.slots.indexOf("");
                    if (node.active() && free >= 0) s.slots.set(free, node.id());
                }
                save(p, s);
                refresh(p);
                sync(p);
                return Result.ok("Unlocked " + count + " node" + (count == 1 ? "" : "s") + " (test, free).");
            }
            case "reset" -> {
                return respec(p, true);
            }
            case "ready" -> {
                resetCooldowns(p);
                return Result.ok("Every skill cooldown is over.");
            }
            default -> {
                return Result.fail("skills renown <n>|level <n>|points <n>|unlock <node|path|all>|reset|ready");
            }
        }
    }

    // ------------------------------------------------------------------ the army (Commander path)

    /** Extra army size from Logistics. */
    public static int armyBonus(Player p) {
        return (int) value(p, "logistics", "army");
    }

    /** War beasts a base of this level lets the player field, with Beastmaster, never above the beast limit. */
    public static int beastCap(Player p, int baseLevel) {
        int cap = com.warfront.world.BaseLevel.beastCap(baseLevel);
        return Math.min(com.warfront.config.WFConfig.BEAST_LIMIT.get(), cap + (int) value(p, "beastmaster", "beasts"));
    }

    /** Veteran Corps: units earn veterancy faster. */
    public static int unitXp(com.warfront.entity.SoldierEntity unit, int amount) {
        Player owner = unit.getOwner();
        if (owner == null) return amount;
        double bonus = value(owner, "veteran_corps", "xp");
        return bonus <= 0 ? amount : (int) Math.round(amount * (1 + bonus));
    }

    /** Steady Ranks: a unit near its commander holds out longer before routing. */
    public static boolean steadyRanks(com.warfront.entity.SoldierEntity unit) {
        Player owner = unit.getOwner();
        return owner != null && has(owner, "steady_ranks") && unit.distanceTo(owner) <= value(owner, "steady_ranks", "radius");
    }

    // ------------------------------------------------------------------ sync

    /** Sends the player their Renown, points, learned nodes and slot cooldowns. */
    public static void sync(Player p) {
        if (p instanceof ServerPlayer sp && sp.connection != null) PacketDistributor.sendToPlayer(sp, SkillStatePayload.of(sp));
    }
}
