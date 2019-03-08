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

    private int writeOutAndFindRanksWithoutParentChecking(int insertedFrontStart, int tempStart,
                                                          int minTargetFrontToCompare, int targetFrontUntil) {
        for (int index = insertedFrontStart; index != -1; index = next[index], ++tempStart) {
            ranks[index] = findRank(minTargetFrontToCompare, targetFrontUntil, index);
            temp[tempStart] = index;
        }
        return tempStart;
    }

    private int writeOutAndFindRanksWithParentChecking(int insertedFrontStart, int tempStart, int targetFrontUntil) {
        for (int index = insertedFrontStart; index != -1; index = next[index], ++tempStart) {
            int myMinTargetFront = ranks[parents[index]] + 1;
            ranks[index] = myMinTargetFront == targetFrontUntil
                    ? myMinTargetFront
                    : findRank(myMinTargetFront, targetFrontUntil, index);
            temp[tempStart] = index;
        }
        return tempStart;
    }

    private int putToFronts(int pointIdx, int m, int targetFrontUntil) {
        boolean frontMoved = false;
        boolean allToMoved = true;
        while (--pointIdx >= m) {
            int index = temp[pointIdx];
            int rankPtr = ranks[index];
            int diff = targetFrontUntil - rankPtr;
            if (diff == 0) {
                frontMoved = true;
                next[index] = -1;
                ++targetFrontUntil;
            } else {
                allToMoved &= frontMoved && diff == 1;
                next[index] = firstIndex[rankPtr];
            }
            firstIndex[rankPtr] = index;
        }
        return (frontMoved ? 1 : 0) ^ (allToMoved ? 2 : 0);
    }

    private void merge(int l, int m, int n) {
        int r = Math.min(n, m + m - l);
        int targetFrontUntil = l;
        while (targetFrontUntil < m && firstIndex[targetFrontUntil] != -1) {
            ++targetFrontUntil;
        }

        // First front insertion is slightly special
        int insertedFront = m, insertedFrontStart = firstIndex[insertedFront];
        int firstPointIdx = writeOutAndFindRanksWithoutParentChecking(insertedFrontStart, m, l, targetFrontUntil);
        int firstResult = putToFronts(firstPointIdx, m, targetFrontUntil);
        targetFrontUntil += firstResult & 1;
        ++insertedFront;

        // General insertion if no preliminary break
        if (firstResult <= 1) {
            while (insertedFront < r && (insertedFrontStart = firstIndex[insertedFront]) != -1) {
                int pointIdx = writeOutAndFindRanksWithParentChecking(insertedFrontStart, m, targetFrontUntil);
                int result = putToFronts(pointIdx, m, targetFrontUntil);
                targetFrontUntil += result & 1;
                ++insertedFront;
                if (result > 1) {
                    break;
                }
            }
        }

        // Degenerate case if some fronts need to be simply appended
        if (insertedFront == targetFrontUntil) {
            // Case 1: ranks need not be rewritten
            while (insertedFront < r && firstIndex[insertedFront] != -1) {
                ++insertedFront;
                ++targetFrontUntil;
            }
        } else {
            // Case 2: ranks need to be rewritten
            while (insertedFront < r && (insertedFrontStart = firstIndex[insertedFront]) != -1) {
                firstIndex[targetFrontUntil] = insertedFrontStart;
                for (int i = insertedFrontStart; i != -1; i = next[i]) {
                    ranks[i] = targetFrontUntil;
                }
                ++insertedFront;
                ++targetFrontUntil;
            }
        }

        // If there are fewer than N fronts, need to erase the next-to-last front pointer.
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
