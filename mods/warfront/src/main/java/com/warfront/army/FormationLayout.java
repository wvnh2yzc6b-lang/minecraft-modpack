package com.warfront.army;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Pure geometry: where soldier number {@code index} of {@code count} stands in a formation.
 * Offsets are local: x is to the side, z is backwards from the front rank.
 */
public final class FormationLayout {
    private FormationLayout() {}

    /**
     * @param meleeCount how many of the (rank-sorted) soldiers are front-line troops; used by SQUARE
     */
    public static Vec3 slot(Formation formation, int index, int count, int meleeCount, Vec3 anchor, float yaw) {
        double[] local = local(formation, index, count, meleeCount);
        float rad = yaw * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(rad), fz = Mth.cos(rad);   // forward
        double rx = -Mth.cos(rad), rz = -Mth.sin(rad);  // side
        double side = local[0], back = local[1];
        return new Vec3(anchor.x + rx * side - fx * back, anchor.y, anchor.z + rz * side - fz * back);
    }

    static double[] local(Formation formation, int i, int n, int meleeCount) {
        switch (formation) {
            case SHIELD_WALL:
                return grid(i, n, Math.min(n, 10), 1.05, 1.4, false);
            case WEDGE: {
                int row = 0, start = 0;
                while (start + (2 * row + 1) <= i) {
                    start += 2 * row + 1;
                    row++;
                }
                int k = i - start;
                int rowSize = Math.min(2 * row + 1, n - start);
                return new double[]{(k - (rowSize - 1) / 2.0) * 1.4, row * 1.4};
            }
            case SQUARE: {
                int ring = Math.max(1, meleeCount);
                double radius = Math.max(2.0, ring * 1.3 / (2 * Math.PI));
                if (i < meleeCount) {
                    double a = 2 * Math.PI * i / ring;
                    return new double[]{Math.cos(a) * radius, Math.sin(a) * radius};
                }
                int inner = n - meleeCount;
                if (inner <= 1) return new double[]{0, 0};
                double a = 2 * Math.PI * (i - meleeCount) / inner;
                double r = radius * 0.45;
                return new double[]{Math.cos(a) * r, Math.sin(a) * r};
            }
            case SKIRMISH:
                return grid(i, n, Math.min(n, 6), 3.2, 3.2, true);
            case LINE:
            default:
                return grid(i, n, Math.min(n, 8), 1.6, 1.8, false);
        }
    }

    private static double[] grid(int i, int n, int cols, double sx, double sz, boolean stagger) {
        cols = Math.max(1, cols);
        int row = i / cols;
        int col = i % cols;
        int rowSize = Math.min(cols, n - row * cols);
        double x = (col - (rowSize - 1) / 2.0) * sx;
        if (stagger && (row & 1) == 1) x += sx / 2;
        return new double[]{x, row * sz};
    }
}
