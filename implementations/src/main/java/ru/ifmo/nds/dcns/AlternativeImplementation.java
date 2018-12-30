package ru.ifmo.nds.dcns;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.DoubleArraySorter;
import ru.ifmo.nds.util.MathEx;

public final class AlternativeImplementation extends NonDominatedSorting {
    private final boolean useBinarySearch;
    private double[][] points;
    private int[] next;
    private int[] firstIndex;
    private int[] ranks;

    public AlternativeImplementation(int maximumPoints, int maximumDimension, boolean useBinarySearch) {
        super(maximumPoints, maximumDimension);
        this.useBinarySearch = useBinarySearch;
        this.points = new double[maximumPoints][];
        this.next = new int[maximumPoints];
        this.firstIndex = new int[maximumPoints];
        this.ranks = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return useBinarySearch ? "DCNS-BS-alt" : "DCNS-SS-alt";
    }

    @Override
    protected void closeImpl() {
        this.points = null;
        this.next = null;
        this.firstIndex = null;
        this.ranks = null;
    }

    private boolean checkIfDoesNotDominate(int targetFront, double[] point, int maxObj) {
        int index = firstIndex[targetFront];
        while (index != -1) {
            // cannot assume `points[index]` is lexicographically smaller than `point`
            // because the right part is processed front-first.
            if (DominanceHelper.strictlyDominatesAssumingNotEqual(points[index], point, maxObj)) {
                return false;
            }
            index = next[index];
        }
        return true;
    }

    private int findRankSS(int targetFrom, int targetUntil, double[] point, int maxObj) {
        for (int target = targetFrom; target < targetUntil; ++target) {
            if (checkIfDoesNotDominate(target, point, maxObj)) {
                return target;
            }
        }
        return targetUntil;
    }

    private int findRankBS(int targetFrom, int targetUntil, double[] point, int maxObj) {
        if (checkIfDoesNotDominate(targetFrom, point, maxObj)) {
            return targetFrom;
        }
        while (targetUntil - targetFrom > 1) {
            int mid = (targetFrom + targetUntil) >>> 1;
            if (checkIfDoesNotDominate(mid, point, maxObj)) {
                targetUntil = mid;
            } else {
                targetFrom = mid;
            }
        }
        return targetUntil;
    }

    private int findRank(int minTargetFrontToCompare, int targetFrontUntil, double[] point, int maxObj) {
        return useBinarySearch
                ? findRankBS(minTargetFrontToCompare, targetFrontUntil, point, maxObj)
                : findRankSS(minTargetFrontToCompare, targetFrontUntil, point, maxObj);
    }

    private void merge(int l, int m, int n, int maxObj) {
        int r = Math.min(n, m + m - l);
        int minTargetFrontToCompare = l - 1;
        int targetFrontUntil = l;
        while (targetFrontUntil < m && firstIndex[targetFrontUntil] != -1) {
            ++targetFrontUntil;
        }

        for (int insertedFront = m; insertedFront < r; ++insertedFront) {
            int insertedFrontStart = firstIndex[insertedFront];
            if (insertedFrontStart == -1) {
                break;
            }
            if (++minTargetFrontToCompare == targetFrontUntil) {
                firstIndex[targetFrontUntil] = insertedFrontStart;
                ++targetFrontUntil;
            } else {
                int rankIdx = l;
                // Find ranks&indices, put them into the `ranks` array.
                // We use `ranks` both for ranks and indices, and start `rankIdx` from `l`,
                // (what for) to increase linearity here (e.g. only one array is written sequentially),
                // (why we can) because the right part is not larger than the left part.
                for (int index = insertedFrontStart; index != -1; index = next[index], ++rankIdx) {
                    double[] point = points[index];
                    ranks[rankIdx] = findRank(minTargetFrontToCompare, targetFrontUntil, point, maxObj);
                    ranks[++rankIdx] = index;
                }
                // Integrate the solutions into the target fronts, starting from the last tested one.
                minTargetFrontToCompare = targetFrontUntil;
                while (--rankIdx >= l) {
                    int index = ranks[rankIdx];
                    int rankPtr = ranks[--rankIdx];
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
            if (!DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[l], points[r], maxObj)) {
                next[r] = l;
                firstIndex[l] = r;
                firstIndex[r] = -1;
            }
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int oldN = points.length;
        int maxObj = points[0].length - 1;
        ArrayHelper.fillIdentity(indices, oldN);
        sorter.lexicographicalSort(points, indices, 0, oldN, maxObj + 1);

        int n = DoubleArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        for (int i = 0; i < n; ++i) {
            firstIndex[i] = i;
            next[i] = -1;
        }

        int treeLevel = MathEx.log2up(n);
        // The first run of merging can be written with a much smaller leading constant.
        merge0(n, maxObj);
        // The rest of the runs use the generic implementation.
        for (int i = 1; i < treeLevel; i++) {
            int delta = 1 << i, delta2 = delta + delta;
            for (int r = delta; r < n; r += delta2) {
                merge(r - delta, r, n, maxObj);
            }
        }

        for (int r = 0; r < n; ++r) {
            int idx = firstIndex[r];
            if (idx == -1) {
                break;
            }
            while (idx != -1) {
                this.ranks[idx] = r;
                idx = next[idx];
            }
        }

        for (int i = 0; i < oldN; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
