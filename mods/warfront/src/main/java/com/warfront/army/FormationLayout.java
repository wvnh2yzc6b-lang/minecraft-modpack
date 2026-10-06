package com.warfront.army;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Pure geometry: where a soldier stands in a formation.
 * <p>
 * Ranked formations (line, shield wall, skirmish) put each kind of troop in its own ranks:
 * shields in front, then spears, swords and captains, then archers, then healers. Wedge and
 * square place troops by their rank order instead.
 * <p>
 * Offsets are local: x is to the side, z is backwards from the front rank.
 */
public final class FormationLayout {
    private FormationLayout() {}

    /** Which group of ranks a role stands in, front (0) to back (3). */
    public static int line(SoldierRole role) {
        return switch (role) {
            case SHIELDBEARER -> 0;
            case SPEARMAN, SWORDSMAN, CAPTAIN, CHAMPION -> 1;
            case ARCHER -> 2;
            case HEALER -> 3;
        };
    }

    /**
     * @param roles the roles of the whole group, sorted by {@link SoldierRole#rank}
     * @param index this soldier's index in {@code roles}
     */
    public static Vec3 slot(Formation formation, List<SoldierRole> roles, int index, Vec3 anchor, float yaw) {
        double[] local = local(formation, roles, index);
        float rad = yaw * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(rad), fz = Mth.cos(rad);   // forward
        double rx = -Mth.cos(rad), rz = -Mth.sin(rad);  // side
        double side = local[0], back = local[1];
        return new Vec3(anchor.x + rx * side - fx * back, anchor.y, anchor.z + rz * side - fz * back);
    }

    static double[] local(Formation formation, List<SoldierRole> roles, int index) {
        int n = roles.size();
        switch (formation) {
            case WEDGE: {
                int row = 0, start = 0;
                while (start + (2 * row + 1) <= index) {
                    start += 2 * row + 1;
                    row++;
                }
                int k = index - start;
                int rowSize = Math.min(2 * row + 1, n - start);
                return new double[]{(k - (rowSize - 1) / 2.0) * 1.4, row * 1.4};
            }
            case SQUARE: {
                int melee = 0;
                for (SoldierRole r : roles) if (r.melee) melee++;
                int ring = Math.max(1, melee);
                double radius = Math.max(2.0, ring * 1.3 / (2 * Math.PI));
                if (roles.get(index).melee) {
                    int k = 0;
                    for (int i = 0; i < index; i++) if (roles.get(i).melee) k++;
                    double a = 2 * Math.PI * k / ring;
                    return new double[]{Math.cos(a) * radius, Math.sin(a) * radius};
                }
                int inner = n - melee;
                if (inner <= 1) return new double[]{0, 0};
                int k = 0;
                for (int i = 0; i < index; i++) if (!roles.get(i).melee) k++;
                double a = 2 * Math.PI * k / inner;
                double r = radius * 0.45;
                return new double[]{Math.cos(a) * r, Math.sin(a) * r};
            }
            case SHIELD_WALL:
                return ranked(roles, index, 10, 1.05, 1.4, false);
            case SKIRMISH:
                return ranked(roles, index, 6, 3.2, 3.2, true);
            case LINE:
            default:
                return ranked(roles, index, 8, 1.6, 1.8, false);
        }
    }

    private static double[] ranked(List<SoldierRole> roles, int index, int cols, double sx, double sz, boolean stagger) {
        int myLine = line(roles.get(index));
        int[] lineSize = new int[4];
        int k = 0;
        for (int i = 0; i < roles.size(); i++) {
            int l = line(roles.get(i));
            if (l == myLine && i < index) k++;
            lineSize[l]++;
        }
        int rowsBefore = 0;
        for (int l = 0; l < myLine; l++) rowsBefore += (lineSize[l] + cols - 1) / cols;

        int rowInLine = k / cols;
        int col = k % cols;
        int rowSize = Math.min(cols, lineSize[myLine] - rowInLine * cols);
        int row = rowsBefore + rowInLine;
        double x = (col - (rowSize - 1) / 2.0) * sx;
        if (stagger && (row & 1) == 1) x += sx / 2;
        return new double[]{x, row * sz};
    }
}
