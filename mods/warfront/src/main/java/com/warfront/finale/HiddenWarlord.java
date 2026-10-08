package com.warfront.finale;

import com.warfront.alert.Alerts;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.faction.Factions;
import com.warfront.faction.NpcFaction;
import com.warfront.faction.Relation;
import com.warfront.fortress.Warlords;
import com.warfront.registry.WFRegistry;
import com.warfront.world.WarbandSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The hidden warlord, the advisor's brother, behind all seven wars. He waits on the throne of the Frozen Field. He
 * fights with every race's tricks in turn (the seven warlords' signature attacks), and at each quarter of his health
 * the statues of the old war wake to defend him. When he falls he dies: a short scene names his brother as the true
 * enemy, and he leaves his blade and the Key to the End. Stand-in look (worn knight, cracked crown) until designed.
 */
public final class HiddenWarlord {
    public static final String TAG = "warfront_hidden_warlord";
    public static final String NAME = "The Hidden Warlord";
    private static final String PHASE = "warfront_hw_phase";
    private static final String NEXT = "warfront_hw_next";
    private static final String CYCLE = "warfront_hw_cycle";
    private static final String WOKEN = "warfront_hw_woken";
    public static final int FIGHT_RANGE = 48;

    @Nullable private static ServerBossEvent bar;
    /** Dying words and the trip home, as (game time, action). */
    private record Beat(long at, Runnable action) {}

    private static final List<Beat> SCENE = new ArrayList<>();

    private HiddenWarlord() {}

    public static boolean isHim(Entity e) {
        return e instanceof SoldierEntity s && s.getPersistentData().getBoolean(TAG);
    }

    @Nullable
    public static SoldierEntity find(ServerLevel level) {
        for (SoldierEntity s : level.getEntities(WFRegistry.SOLDIER.get(), HiddenWarlord::isHim)) if (s.isAlive()) return s;
        return null;
    }

    /** Raises him on his throne. */
    public static SoldierEntity spawn(ServerLevel level, BlockPos at) {
        SoldierEntity w = WFRegistry.SOLDIER.get().create(level);
        if (w == null) throw new IllegalStateException("soldier entity failed to create");
        w.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
        w.setupAsRaider(NpcFaction.BLACK_LEGION, SoldierRole.CHAMPION, UUID.randomUUID(), null, null, 4);
        w.makeWarlord();
        w.makeCampaignWarlord(NAME);
        w.setCustomName(Component.literal(NAME).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        w.setPersistenceRequired();
        w.getPersistentData().putBoolean(TAG, true);
        w.getPersistentData().putInt(PHASE, 1);
        level.addFreshEntity(w);
        level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.6F);
        return w;
    }

    /** Players in the Field who have not yet beaten him. */
    private static List<ServerPlayer> challengers(ServerLevel field) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : field.players()) if (!p.getData(WFRegistry.HIDDEN_WARLORD_BEATEN) && !p.isSpectator()) out.add(p);
        return out;
    }

    /** Once a second from FrozenField.tick: raise him when a challenger arrives, run the fight, play the scene. */
    public static void tick(ServerLevel field) {
        long now = field.getGameTime();
        SCENE.removeIf(b -> {
            if (now < b.at()) return false;
            b.action().run();
            return true;
        });
        SoldierEntity w = find(field);
        List<ServerPlayer> challengers = challengers(field);
        if (w == null) {
            clearBar();
            if (!challengers.isEmpty() && SCENE.isEmpty()) {
                w = spawn(field, FrozenField.THRONE);
                for (ServerPlayer p : challengers) {
                    Alerts.banner(p, "hidden_warlord", NAME, "The one behind all seven wars.", 0xD4A017);
                    p.sendSystemMessage(Component.literal("\"So the champion comes at last. My brother chose well... and that is why you must fall.\"")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                }
            }
            return;
        }
        if (bar == null) bar = new ServerBossEvent(Component.literal(NAME).withStyle(ChatFormatting.GOLD),
                BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
        float frac = w.getHealth() / w.getMaxHealth();
        bar.setProgress(Mth.clamp(frac, 0F, 1F));
        for (ServerPlayer p : field.players()) {
            boolean near = p.distanceToSqr(w) < FIGHT_RANGE * FIGHT_RANGE;
            if (near && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            else if (!near && bar.getPlayers().contains(p)) bar.removePlayer(p);
        }
        int phase = frac > 0.75F ? 1 : frac > 0.5F ? 2 : frac > 0.25F ? 3 : 4;
        if (phase > w.getPersistentData().getInt(PHASE)) {
            w.getPersistentData().putInt(PHASE, phase);
            wakeStatues(field, w, 4);
            field.playSound(null, w.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.5F);
            bar.setColor(phase == 4 ? BossEvent.BossBarColor.PURPLE : phase == 3 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
        }
        if (now >= w.getPersistentData().getLong(NEXT)) {
            LivingEntity target = target(field, w);
            if (target != null) {
                // Every race's trick in turn.
                int cycle = w.getPersistentData().getInt(CYCLE);
                NpcFaction f = NpcFaction.values()[cycle % NpcFaction.values().length];
                w.getPersistentData().putInt(CYCLE, cycle + 1);
                Warlords.signature(field, w, f, target);
                w.getPersistentData().putLong(NEXT, now + 200 - phase * 30L);
            }
        }
    }

    @Nullable
    private static LivingEntity target(ServerLevel level, SoldierEntity w) {
        if (w.getTarget() != null && w.getTarget().isAlive()) return w.getTarget();
        return level.getEntitiesOfClass(LivingEntity.class, w.getBoundingBox().inflate(24),
                        e -> e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && Factions.relation(w, e) == Relation.ENEMY)
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(w), b.distanceToSqr(w))).orElse(null);
    }

    /** Wakes up to {@code n} more statues: the stone cracks and a soldier of its race steps out to defend him. */
    public static int wakeStatues(ServerLevel field, SoldierEntity w, int n) {
        int woken = w.getPersistentData().getInt(WOKEN);
        List<BlockPos> statues = FrozenField.statues();
        int done = 0;
        for (int i = woken; i < statues.size() && done < n; i++, done++) {
            BlockPos s = statues.get(i);
            NpcFaction f = FrozenField.statueFaction(i);
            for (int y = 0; y < 3; y++) {
                field.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, field.getBlockState(s.above(y))),
                        s.getX() + 0.5, s.getY() + y + 0.5, s.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
                field.setBlock(s.above(y), Blocks.AIR.defaultBlockState(), 3);
            }
            for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) field.setBlock(s.above().relative(d), Blocks.AIR.defaultBlockState(), 3);
            List<SoldierRole> role = List.of(i % 3 == 0 ? SoldierRole.SWORDSMAN : i % 3 == 1 ? SoldierRole.SPEARMAN : SoldierRole.ARCHER);
            for (SoldierEntity d : WarbandSpawner.spawnInto(field, w.getWarbandId(), f, role, s, w.position(), null, 3)) {
                d.setPersistenceRequired();
            }
            field.playSound(null, s, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        w.getPersistentData().putInt(WOKEN, woken + done);
        return done;
    }

    /** He falls: his last words, his blade and the Key to the End, and a quiet walk home a little later. */
    public static void died(SoldierEntity w) {
        if (!(w.level() instanceof ServerLevel field)) return;
        clearBar();
        w.spawnAtLocation(new ItemStack(WFRegistry.BROTHERS_BLADE.get()));
        w.spawnAtLocation(new ItemStack(WFRegistry.KEY_TO_THE_END.get()));
        UUID band = w.getWarbandId();
        if (band != null) {
            for (SoldierEntity s : field.getEntities(WFRegistry.SOLDIER.get(), s -> band.equals(s.getWarbandId()) && s != w)) {
                field.sendParticles(ParticleTypes.CLOUD, s.getX(), s.getY() + 1, s.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
                s.discard();   // the statues' soldiers crumble with him
            }
        }
        List<ServerPlayer> there = new ArrayList<>(field.players());
        for (ServerPlayer p : there) p.setData(WFRegistry.HIDDEN_WARLORD_BEATEN, true);
        long now = field.getGameTime();
        String[] lines = {
                "\"Enough... it is done. Listen, champion, while I still have breath.\"",
                "\"Your advisor... is my brother. We share one face, one blood, one oath we both broke.\"",
                "\"I set the seven against you to keep you from rising. Every war you won, you won for him.\"",
                "\"He wants the End. Take my blade. Take the key. Be ready when he shows his true face.\"",
                "The hidden warlord is still. Around you the frozen soldiers crumble to dust."};
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean last = i == lines.length - 1;
            SCENE.add(new Beat(now + 40L + i * 70L, () -> {
                for (ServerPlayer p : there) {
                    if (p.level() == field) p.sendSystemMessage(Component.literal(line).withStyle(last ? ChatFormatting.GRAY : ChatFormatting.GOLD,
                            ChatFormatting.ITALIC));
                }
            }));
        }
        SCENE.add(new Beat(now + 40L + lines.length * 70L + 600L, () -> {
            for (ServerPlayer p : there) if (p.level() == field) FrozenField.leave(p);
        }));
        for (ServerPlayer p : there) Alerts.banner(p, "hidden_warlord_down", NAME + " falls", "Hear his last words.", 0xD4A017);
    }

    private static void clearBar() {
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    public static void clearAll() {
        clearBar();
        SCENE.clear();
    }

    /** Where to find his things after a test: the drops near {@code at}. */
    public static int dropsNear(ServerLevel level, Vec3 at) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(at, at).inflate(4),
                e -> e.getItem().is(WFRegistry.BROTHERS_BLADE.get()) || e.getItem().is(WFRegistry.KEY_TO_THE_END.get())).size();
    }
}
