package ru.ifmo.nds.dcns;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.ArraySorter;
import ru.ifmo.nds.util.MathEx;

public abstract class DCNSBase extends NonDominatedSorting {
    private double[][] points;
    private int[] next;
    private int[] firstIndex;
    private int[] ranks;
    private int[] parents;
    private int[] temp;

    DCNSBase(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        this.points = new double[maximumPoints][];
        this.next = new int[maximumPoints];
        this.firstIndex = new int[maximumPoints];
        this.ranks = new int[maximumPoints];
        this.parents = new int[maximumPoints];
        this.temp = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() {
        this.points = null;
        this.next = null;
        this.firstIndex = null;
        this.ranks = null;
        this.parents = null;
        this.temp = null;
    }

    final boolean checkIfDoesNotDominate(int targetFront, int pointIndex) {
        int index = firstIndex[targetFront];
        final double[] point = points[pointIndex];
        final int maxObj = point.length - 1;
        while (index != -1) {
            // cannot assume `points[index]` is lexicographically smaller than `point`
            // because the right part is processed front-first.
            if (DominanceHelper.strictlyDominatesAssumingNotEqual(points[index], point, maxObj)) {
                parents[pointIndex] = index;
                return false;
            }
            index = next[index];
        }
        return true;
    }

    abstract int findRank(int targetFrom, int targetUntil, int pointIndex);

    private void merge(int l, int m, int n) {
        int r = Math.min(n, m + m - l);
        int minTargetFrontToCompare = l - 1;
        int targetFrontUntil = l;
        while (targetFrontUntil < m && firstIndex[targetFrontUntil] != -1) {
            ++targetFrontUntil;
        }

        for (int insertedFront = m; insertedFront < r; ++insertedFront) {
            boolean isNotFirst = insertedFront != m;
            int insertedFrontStart = firstIndex[insertedFront];
            if (insertedFrontStart == -1) {
                break;
            }
            if (++minTargetFrontToCompare == targetFrontUntil) {
                if (insertedFront != targetFrontUntil) {
                    firstIndex[targetFrontUntil] = insertedFrontStart;
                    for (int i = insertedFrontStart; i != -1; i = next[i]) {
                        ranks[i] = targetFrontUntil;
                    }
                }
                ++targetFrontUntil;
            } else {
                // Find the ranks of the points from the current front, and write out the points in the `temp` array.
                int pointIdx = m;
                for (int index = insertedFrontStart; index != -1; index = next[index], ++pointIdx) {
                    if (index < m || index >= r) {
                        throw new AssertionError(m + " " + index + " " + r);
                    }
                    int myMinTargetFront = isNotFirst ? ranks[parents[index]] + 1 : minTargetFrontToCompare;
                    ranks[index] = myMinTargetFront == targetFrontUntil
                            ? myMinTargetFront
                            : findRank(myMinTargetFront, targetFrontUntil, index);
                    temp[pointIdx] = index;
                }
                // Integrate the solutions into the target fronts, starting from the last tested one.
                minTargetFrontToCompare = targetFrontUntil;
                while (--pointIdx >= m) {
                    int index = temp[pointIdx];
                    int rankPtr = ranks[index];
                    if (targetFrontUntil == rankPtr) {
                        ++targetFrontUntil;
                        next[index] = -1;
                    } else {
                        if (minTargetFrontToCompare > rankPtr) {
                            minTargetFrontToCompare = rankPtr;
                        }
                        next[index] = firstIndex[rankPtr];
                    }
                    firstIndex[rankPtr] = index;
                }
            }
        }
        if (targetFrontUntil < r) {
            firstIndex[targetFrontUntil] = -1;
        }
    }

    private void merge0(int n, int maxObj) {
        for (int r = 1; r < n; r += 2) {
            int l = r - 1;
            // We can assume, here and only here, that `l` is lexicographically smaller than `r`.
            if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[l], points[r], maxObj)) {
                parents[r] = l;
            } else {
                next[r] = l;
                firstIndex[l] = r;
                firstIndex[r] = -1;
                ranks[r] = l;
            }
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int oldN = points.length;
        int maxObj = points[0].length - 1;
        ArrayHelper.fillIdentity(indices, oldN);
        sorter.lexicographicalSort(points, indices, 0, oldN, maxObj + 1);

        int n = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        for (int i = 0; i < n; ++i) {
            this.ranks[i] = i;
            firstIndex[i] = i;
            next[i] = -1;
            parents[i] = -1;
        }

        int treeLevel = MathEx.log2up(n);
        // The first run of merging can be written with a much smaller leading constant.
        merge0(n, maxObj);
        // The rest of the runs use the generic implementation.
        for (int i = 1; i < treeLevel; i++) {
            int delta = 1 << i, delta2 = delta + delta;
            for (int r = delta; r < n; r += delta2) {
                merge(r - delta, r, n);
            }
        }

        for (int i = 0; i < oldN; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
