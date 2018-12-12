package ru.ifmo.nds.dcns;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.DoubleArraySorter;
import ru.ifmo.nds.util.MathEx;

import java.util.Arrays;

public final class AlternativeImplementation extends NonDominatedSorting {
    private final boolean useBinarySearch;
    private double[][] points;
    private int[] nextAndValue;
    private int[] firstIndex;
    private int[] ranks;

    public AlternativeImplementation(int maximumPoints, int maximumDimension, boolean useBinarySearch) {
        super(maximumPoints, maximumDimension);
        this.useBinarySearch = useBinarySearch;
        this.points = new double[maximumPoints][];
        this.nextAndValue = new int[maximumPoints * 2];
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
        this.nextAndValue = null;
        this.firstIndex = null;
        this.ranks = null;
    }

    private boolean checkIfDoesNotDominate(int targetFront, double[] point, int maxObj) {
        int index = firstIndex[targetFront];
//        if (maxObj == 1) {
//            // 2D case, enough to check the dominance with the first entry
//            return !DominanceHelper.strictlyDominatesAssumingNotSame(points[nextAndValue[index + 1]], point, maxObj);
//        }
        while (index != -1) {
            if (DominanceHelper.strictlyDominatesAssumingNotSame(points[nextAndValue[index + 1]], point, maxObj)) {
                return false;
            }
            index = nextAndValue[index];
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

    private void merge(int l, int m, int n, int maxObj) {
        int r = Math.min(n, m + m - l);
        int minTargetFrontToCompare = l - 1;
        int targetFrontUntil = m;
        while (l < m && firstIndex[targetFrontUntil - 1] == -1) {
            --targetFrontUntil;
        }

        for (int insertedFront = m; insertedFront < r; ++insertedFront) {
            int insertedFrontStart = firstIndex[insertedFront];
            if (insertedFrontStart == -1) {
                break;
            }
            if (++minTargetFrontToCompare == targetFrontUntil) {
                firstIndex[minTargetFrontToCompare] = insertedFrontStart;
                ++targetFrontUntil;
            } else {
                // Find ranks, put them into the `ranks` array, do not integrate the points into the target fronts.
                for (int rankIdx = l, index = insertedFrontStart; index != -1; index = nextAndValue[index], ++rankIdx) {
                    double[] point = points[nextAndValue[index + 1]];
                    int rankPtr = useBinarySearch
                            ? findRankBS(minTargetFrontToCompare, targetFrontUntil, point, maxObj)
                            : findRankSS(minTargetFrontToCompare, targetFrontUntil, point, maxObj);
                    ranks[rankIdx] = rankPtr;
                }
                // Integrate the solutions into the target fronts.
                for (int rankIdx = l, index = insertedFrontStart; index != -1; ++rankIdx) {
                    int next = nextAndValue[index];
                    int rankPtr = ranks[rankIdx];
                    if (targetFrontUntil < rankPtr) {
                        throw new AssertionError();
                    }
                    if (targetFrontUntil == rankPtr) {
                        // need to make the new front
                        ++targetFrontUntil;
                        nextAndValue[index] = -1;
                    } else {
                        nextAndValue[index] = firstIndex[rankPtr];
                    }
                    firstIndex[rankPtr] = index;
                    index = next;
                    if (rankIdx == l) {
                        minTargetFrontToCompare = rankPtr;
                    } else {
                        minTargetFrontToCompare = Math.min(minTargetFrontToCompare, rankPtr);
                    }
                }
            }
        }
        if (targetFrontUntil < r) {
            firstIndex[targetFrontUntil] = -1;
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int oldN = points.length;
        int maxObj = points[0].length - 1;
        ArrayHelper.fillIdentity(indices, oldN);
        sorter.lexicographicalSort(points, indices, 0, oldN, maxObj + 1);

        int n = DoubleArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        Arrays.fill(this.ranks, 0, n, -42);
        for (int i = 0; i < n; ++i) {
            firstIndex[i] = i * 2;
            nextAndValue[i * 2] = -1;
            nextAndValue[i * 2 + 1] = i;
        }

        int treeLevel = MathEx.log2up(n);
        for (int i = 0; i < treeLevel; i++) {
            int x = ((n - 1) >>> (i + 1)) + 1;
            for (int j = 0; j < x; j++) {
                int a = j << (i + 1);
                int b = a + (1 << i);
                if (b < n) {
                    merge(a, b, n, maxObj);
                }
            }
        }

        for (int r = 0; r < n; ++r) {
            int idx = firstIndex[r];
            if (idx == -1) {
                break;
            }
            while (idx != -1) {
                this.ranks[nextAndValue[idx + 1]] = r;
                idx = nextAndValue[idx];
            }
        }

        for (int i = 0; i < oldN; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
