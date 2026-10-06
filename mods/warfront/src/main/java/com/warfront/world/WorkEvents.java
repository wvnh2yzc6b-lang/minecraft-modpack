package com.warfront.world;

import com.warfront.Warfront;
import com.warfront.army.SoldierRole;
import com.warfront.entity.SoldierEntity;
import com.warfront.entity.work.Blueprint;
import com.warfront.faction.Factions;
import com.warfront.faction.Relation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.function.Consumer;

/**
 * Keeps builders' blueprints in step with what their side builds on purpose: a block a friendly
 * player places becomes part of the plan, and one they break is dropped from it. Damage done by
 * enemies or explosions is not a player edit, so builders put it back.
 */
@EventBusSubscriber(modid = Warfront.MODID)
public final class WorkEvents {
    private WorkEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        forFriendlyBlueprints(level, pos, event.getPlayer(), bp -> bp.forget(pos));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player)) return;
        BlockPos pos = event.getPos();
        forFriendlyBlueprints(level, pos, player, bp -> bp.record(pos, event.getPlacedBlock()));
    }

    private static void forFriendlyBlueprints(ServerLevel level, BlockPos pos, Player player, Consumer<Blueprint> action) {
        int r = Blueprint.RADIUS + 2;
        for (SoldierEntity s : level.getEntitiesOfClass(SoldierEntity.class, new AABB(pos).inflate(r, Blueprint.ABOVE + 2, r),
                s -> s.getRole() == SoldierRole.BUILDER && s.getBlueprint() != null)) {
            Blueprint bp = s.getBlueprint();
            boolean friendly = s.isOwnedBy(player) || Factions.relation(s, player) == Relation.ALLY;
            if (friendly && bp.covers(pos)) action.accept(bp);
        }
    }
}
