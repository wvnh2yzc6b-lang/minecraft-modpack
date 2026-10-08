package com.warfront.client.ui;

import com.warfront.alert.Alerts;
import com.warfront.army.Formation;
import com.warfront.army.Order;
import com.warfront.combat.Rage;
import com.warfront.combat.Souls;
import com.warfront.config.WFClientConfig;
import com.warfront.faction.Race;
import com.warfront.network.ClientRaceState;
import com.warfront.network.HudPayload;
import com.warfront.registry.WFRegistry;
import com.warfront.world.HiveAdaptation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The combat HUD: a compact corner panel with the race power row, the army row and, during a siege at your War
 * Standard, the siege row. Hidden with F1 and when there's nothing to show. Position and size in the client config.
 */
public final class CombatHud {
    private static final int W = 150, ROW = 11;

    private CombatHud() {}

    private interface Row {
        void draw(GuiGraphics g, Font font, int x, int y);
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || !WFClientConfig.HUD_ENABLED.get()) return;
        Race race = Race.byId(ClientRaceState.get(p.getUUID()));
        if (race == null) return;
        int[] v = HudPayload.client;
        int accent = Alerts.accent(race);
        List<Row> rows = new ArrayList<>();
        powerRow(rows, p, race, v);
        if (v[HudPayload.TOTAL] > 0) {
            String order = Order.byOrdinal(v[HudPayload.ORDER]).title;
            String formation = Formation.byOrdinal(v[HudPayload.FORMATION]).title;
            rows.add((gg, f, x, y) -> WFTheme.text(gg, f, "Army " + v[HudPayload.FOLLOWING] + "/" + v[HudPayload.TOTAL]
                    + " · " + formation + " · " + order, x, y, WFTheme.TEXT));
            if (v[HudPayload.GUARD] + v[HudPayload.PATROL] > 0 || v[HudPayload.BEAST_CAP] > 0) {
                rows.add((gg, f, x, y) -> WFTheme.text(gg, f, v[HudPayload.GUARD] + " guard · " + v[HudPayload.PATROL]
                        + " patrol · beasts " + v[HudPayload.BEASTS] + "/" + v[HudPayload.BEAST_CAP], x, y, WFTheme.MUTED));
            }
        }
        if (v[HudPayload.WAVE] > 0) {
            rows.add((gg, f, x, y) -> WFTheme.text(gg, f, "Wave " + v[HudPayload.WAVE] + " · " + v[HudPayload.LEFT]
                    + " enemies left", x, y, 0xFFFF9A9A));
        }
        if (rows.isEmpty()) return;

        float scale = WFClientConfig.HUD_SCALE.get().floatValue();
        int h = 6 + rows.size() * ROW + 2;
        int sw = Math.round(g.guiWidth() / scale), sh = Math.round(g.guiHeight() / scale);
        int x, y;
        switch (WFClientConfig.HUD_CORNER.get()) {
            case TOP_LEFT -> { x = 4; y = 4; }
            case TOP_RIGHT -> { x = sw - W - 4; y = 4; }
            case BOTTOM_RIGHT -> { x = sw - W - 4; y = sh - h - 4; }
            default -> { x = 4; y = sh - h - 4; }
        }
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1F);
        WFTheme.panel(g, x, y, W, h, accent);
        for (int i = 0; i < rows.size(); i++) rows.get(i).draw(g, mc.font, x + 5, y + 6 + i * ROW);
        g.pose().popPose();
    }

    private static void powerRow(List<Row> rows, LocalPlayer p, Race race, int[] v) {
        switch (race) {
            case DEMON -> rows.add((g, f, x, y) -> {
                WFTheme.text(g, f, "Souls", x, y, 0xFFE0612A);
                int souls = v[HudPayload.SOULS];
                for (int i = 0; i < Souls.MAX; i++) {
                    int px = x + 32 + i * 9;
                    g.renderOutline(px, y, 7, 7, 0xFFE0612A);
                    if (i < souls) g.fill(px + 1, y + 1, px + 6, y + 6, souls >= Souls.MAX ? 0xFFFFC080 : 0xFFE0612A);
                }
            });
            case ORC -> rows.add((g, f, x, y) -> {
                int frenzy = v[HudPayload.FRENZY];
                if (frenzy > 0) {
                    boolean flash = (p.tickCount / 5) % 2 == 0;
                    WFTheme.text(g, f, "FRENZY " + (frenzy / 20 + 1) + "s", x, y, flash ? 0xFFFF6A3D : 0xFFB8231A);
                } else {
                    WFTheme.text(g, f, "Rage", x, y, 0xFFFF6A3D);
                    WFTheme.bar(g, x + 32, y + 2, W - 44, 4, v[HudPayload.RAGE] / (float) Rage.MAX, 0xB8231A);
                }
            });
            case DWARF -> rows.add((g, f, x, y) -> {
                int oath = v[HudPayload.OATH];
                int resolve = v[HudPayload.RESOLVE];
                if (oath > 0) {
                    WFTheme.text(g, f, "OATH OF STONE " + (oath / 20 + 1) + "s", x, y, 0xFFE0E6EE);
                } else if (resolve >= com.warfront.combat.Resolve.MAX) {
                    boolean flash = (p.tickCount / 6) % 2 == 0;
                    WFTheme.text(g, f, "Oath ready: crouch", x, y, flash ? 0xFFFFFFFF : 0xFFB8C0CA);
                } else {
                    WFTheme.text(g, f, "Resolve", x, y, 0xFFB8C0CA);
                    WFTheme.bar(g, x + 40, y + 2, W - 52, 4, resolve / (float) com.warfront.combat.Resolve.MAX, 0x9AA3AE);
                }
            });
            case HUMAN -> {
                int valor = v[HudPayload.VALOR];
                rows.add((g, f, x, y) -> {
                    if (valor >= com.warfront.combat.Valor.MAX) {
                        boolean flash = (p.tickCount / 6) % 2 == 0;
                        WFTheme.text(g, f, "Rally ready: press R", x, y, flash ? 0xFFFFE07A : 0xFFE2B55A);
                    } else {
                        WFTheme.text(g, f, "Valor", x, y, 0xFFE2B55A);
                        WFTheme.bar(g, x + 32, y + 2, W - 44, 4, valor / (float) com.warfront.combat.Valor.MAX, 0xE2B55A);
                    }
                });
                ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
                if (chest.is(WFRegistry.MANA_GLIDER.get())) rows.add((g, f, x, y) -> {
                    WFTheme.text(g, f, "Glider", x, y, 0xFF7FD8FF);
                    WFTheme.bar(g, x + 36, y + 2, W - 48, 4, 1F - chest.getDamageValue() / (float) Math.max(1, chest.getMaxDamage()), 0x7FD8FF);
                });
            }
            case ELF -> {
                int shards = p.getInventory().countItem(WFRegistry.MANA_SHARD.get());
                rows.add((g, f, x, y) -> WFTheme.text(g, f, "Mana shards " + shards, x, y, 0xFF8FE07A));
            }
            case ANGEL -> {
                int radiance = v[HudPayload.RADIANCE];
                rows.add((g, f, x, y) -> {
                    if (radiance >= com.warfront.combat.Radiance.MAX) {
                        boolean flash = (p.tickCount / 6) % 2 == 0;
                        WFTheme.text(g, f, "Judgment ready: press R", x, y, flash ? 0xFFFFFFFF : 0xFFF5D76E);
                    } else {
                        WFTheme.text(g, f, "Radiance", x, y, 0xFFF5D76E);
                        WFTheme.bar(g, x + 48, y + 2, W - 60, 4, radiance / (float) com.warfront.combat.Radiance.MAX, 0xFFF2C0);
                    }
                });
                boolean day = p.level().isDay();
                rows.add((g, f, x, y) -> WFTheme.text(g, f, day ? "Wings: flight (day)" : "Wings: glide only (night)", x, y,
                        day ? 0xFFF5D76E : 0xFF9AA3AE));
            }
            case HIVE -> {
                int swarm = v[HudPayload.SWARM];
                rows.add((g, f, x, y) -> {
                    int bx = x + 40, bw = W - 52;
                    boolean full = swarm >= com.warfront.combat.SwarmCall.MAX;
                    WFTheme.text(g, f, full ? "Deepmaw!" : "Swarm", x, y, full ? 0xFF7FFFF0 : 0xFF3FD8D0);
                    WFTheme.bar(g, bx, y + 2, bw, 4, swarm / (float) com.warfront.combat.SwarmCall.MAX, full ? 0x7FFFF0 : 0x1E8C88);
                    // The notch at 50: lit when a Brood Call is ready.
                    int nx = bx + bw / 2;
                    g.fill(nx, y, nx + 1, y + 8, swarm >= com.warfront.combat.SwarmCall.BROOD ? 0xFF7FFFF0 : 0xFF0E3A38);
                });
                boolean under = HiveAdaptation.underground(p);
                int sky = p.level().getBrightness(LightLayer.SKY, p.blockPosition());
                rows.add((g, f, x, y) -> WFTheme.text(g, f, (under ? "Underground: stronger" : "Surface") + " · sky " + sky, x, y,
                        under ? 0xFF3FD8D0 : 0xFF9AA3AE));
            }
            default -> {
            }
        }
    }
}
