package com.warfront.entity.work;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** Chests and carried goods for workers. */
public final class WorkSites {
    /** How far from its post a worker looks for a storage chest. */
    public static final int STORAGE_RADIUS = 12;

    private WorkSites() {}

    /** The container at {@code pos}, joining double chests. */
    @Nullable
    public static Container containerAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, state, level, pos, true);
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container c && !(be instanceof net.minecraft.world.level.block.entity.HopperBlockEntity) ? c : null;
    }

    /** The nearest chest or barrel to {@code post} that satisfies {@code accept}. */
    @Nullable
    public static BlockPos findStorage(Level level, BlockPos post, Predicate<Container> accept) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(post.offset(-STORAGE_RADIUS, -4, -STORAGE_RADIUS),
                post.offset(STORAGE_RADIUS, 6, STORAGE_RADIUS))) {
            if (!level.isLoaded(p)) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)
                    && !(be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity)) continue;
            Container c = containerAt(level, p);
            if (c == null || !accept.test(c)) continue;
            double d = p.distSqr(post);
            if (d < bestDist) {
                bestDist = d;
                best = p.immutable();
            }
        }
        return best;
    }

    public static int count(Container c, Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    public static boolean hasSpace(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).isEmpty()) return true;
        return false;
    }

    /** Takes one {@code item} out of {@code c}; false if there was none. */
    public static boolean takeOne(Container c, Item item) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.is(item)) {
                s.shrink(1);
                c.setChanged();
                return true;
            }
        }
        return false;
    }

    /** Moves up to {@code max} of {@code item} from one container to another; returns how many moved. */
    public static int move(Container from, Container to, Item item, int max) {
        int moved = 0;
        for (int i = 0; i < from.getContainerSize() && moved < max; i++) {
            ItemStack s = from.getItem(i);
            if (!s.is(item)) continue;
            int want = Math.min(s.getCount(), max - moved);
            ItemStack rest = insert(to, s.copyWithCount(want));
            int done = want - rest.getCount();
            s.shrink(done);
            moved += done;
            if (!rest.isEmpty()) break;
        }
        from.setChanged();
        to.setChanged();
        return moved;
    }

    /** Inserts as much of {@code stack} as fits; returns what is left. */
    public static ItemStack insert(Container c, ItemStack stack) {
        if (c instanceof SimpleContainer simple) return simple.addItem(stack);
        ItemStack left = stack.copy();
        for (int i = 0; i < c.getContainerSize() && !left.isEmpty(); i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, left) && c.canPlaceItem(i, left)) {
                int add = Math.min(left.getCount(), Math.min(c.getMaxStackSize(), s.getMaxStackSize()) - s.getCount());
                if (add > 0) {
                    s.grow(add);
                    left.shrink(add);
                }
            }
        }
        for (int i = 0; i < c.getContainerSize() && !left.isEmpty(); i++) {
            if (c.getItem(i).isEmpty() && c.canPlaceItem(i, left)) {
                c.setItem(i, left.copy());
                left = ItemStack.EMPTY;
            }
        }
        c.setChanged();
        return left;
    }
}
