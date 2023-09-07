package ru.ifmo.nds.deductive;

import ru.ifmo.nds.util.DominanceHelper;

/**
 * <p>This is an optimistic dominance comparator that operates on two-dimensional arrays of points
 * and terminates once two points are found that dominate each other.</p>
 *
 * <p>The terminated state is stored in the fields of the class and can be retrieved by the subsequent algorithm
 * to recover and continue operations.</p>
 *
 * <p>Normally, in-method creation of an object of this class, invocation of {@link #hasDominatingPoints(double[][])}
 * and field accesses are well inlined and the object creation is subsequently eliminated. This is the intended usage
 * pattern of this class, although one can as well create one instance for all the tasks.</p>
 */
public final class OptimisticComparator {
    private int left, right, comparison;

    /**
     * Runs dominance comparisons for the specified points. Returns {@code true} if any dominance relation is found,
     * {@code false} otherwise. When {@code true} is returned, the dominance relation details can be retrieved
     * via {@link #getLeftIndex()}, {@link #getRightIndex()} and {@link #getComparisonResult()}.
     *
     * @param points the points to run dominance checks on.
     * @return {@code true} if any dominance relation is found, {@code false} otherwise.
     */
    public boolean hasDominatingPoints(double[][] points) {
        final int n = points.length;
        final int d = points[0].length;

        for (left = 0; left < n; ++left) {
            if (innerLoop(points, n, d)) {
                return true;
            }
        }

        return false;
    }

    /**
     * After an invocation of {@link #hasDominatingPoints(double[][])},
     * returns the dominance comparison result of the first found dominated pair:
     * <ul>
     *     <li>if -1, the point at {@link #getLeftIndex()} dominates the point at {@link #getRightIndex()};</li>
     *     <li>if +1, the point at {@link #getLeftIndex()} is dominated the point at {@link #getRightIndex()};</li>
     *     <li>if 0, no point dominated any other point.</li>
     * </ul>
     * @return the dominance comparison result of the first found dominated pair.
     */
    public int getComparisonResult() {
        return comparison;
    }

    /**
     * After an invocation of {@link #hasDominatingPoints(double[][])},
     * returns the index of the first of the two points found to be dominating each other.
     * If no two points dominated each other, returns the total number of points.
     *
     * @return the index of the first of the two points found to be dominating each other, or the number of points.
     */
    public int getLeftIndex() {
        return left;
    }

    /**
     * After an invocation of {@link #hasDominatingPoints(double[][])},
     * returns the index of the second of the two points found to be dominating each other.
     * If no two points dominated each other, returns the total number of points.
     *
     * @return the index of the second of the two points found to be dominating each other, or the number of points.
     */
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
