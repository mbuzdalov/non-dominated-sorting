package ru.ifmo.nds.deductive;

import ru.ifmo.nds.util.DominanceHelper;

public final class OptimisticComparator {
    private int left, right, comparison;

    public boolean run(double[][] points) {
        final int n = points.length;
        final int d = points[0].length;

        for (left = 0; left < n; ++left) {
            if (innerLoop(points, n, d)) {
                return true;
            }
        }

        return false;
    }

    public int getComparisonResult() {
        return comparison;
    }

    public int getLeftIndex() {
        return left;
    }

    public int getRightIndex() {
        return right;
    }

    private boolean innerLoop(double[][] points, int n, int d) {
        double[] leftPoint = points[left];
        right = left;
        while (++right < n) {
            comparison = DominanceHelper.dominanceComparison(leftPoint, points[right], d);
            if (comparison != 0) {
                return true;
            }
        }
        return false;
    }
}
