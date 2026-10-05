package com.warfront.item;

import com.warfront.block.WarStandardBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Sound the horn near your War Standard to call the next siege wave early (better rewards, sooner). */
public class WarHornItem extends Item {
    public WarHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);

        WarStandardBlockEntity standard = findStandard(server, player);
        if (standard == null) {
            player.displayClientMessage(Component.literal("No War Standard of yours within 32 blocks.")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!standard.startSiege(server)) {
            player.displayClientMessage(Component.literal("A siege is already underway!")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, 200);
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private static WarStandardBlockEntity findStandard(ServerLevel level, Player player) {
        ChunkPos center = player.chunkPosition();
        WarStandardBlockEntity best = null;
        double bestDist = 32 * 32;
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                if (!level.hasChunk(center.x + cx, center.z + cz)) continue;
                LevelChunk chunk = level.getChunk(center.x + cx, center.z + cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof WarStandardBlockEntity ws) || !ws.isDefender(player)) continue;
                    BlockPos p = be.getBlockPos();
                    double d = player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                    if (d < bestDist) {
                        bestDist = d;
                        best = ws;
                    }
                }
            }
        }
        return best;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use near your War Standard to call the next siege wave").withStyle(ChatFormatting.GRAY));
    }
}
