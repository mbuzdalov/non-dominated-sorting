package ru.ifmo.nds.dcns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.MathEx;

/**
 * This is an implementation of DCNS algorithms by Sumit Mishra.
 *
 * @author Sumit Mishra (idea and initial implementation)
 * @author Maxim Buzdalov (minor cleanup and adaptation to framework interfaces)
 */
public class SumitImplementation2016 extends NonDominatedSorting {
    private List<List<Solution>>[] arrSetNonDominatedFront;
    private int gammaFrontIndex, gammaNoSolution;
    private final boolean useBinarySearch;
    private final boolean useGammaHeuristic;

    public SumitImplementation2016(int maximumPoints, int maximumDimension, boolean useBinarySearch, boolean useGammaHeuristic) {
        super(maximumPoints, maximumDimension);
        this.useBinarySearch = useBinarySearch;
        this.useGammaHeuristic = useGammaHeuristic;
    }

    @Override
    public String getName() {
        return "DCNS-" + (useBinarySearch ? 'B' : 'S') + "S" + (useGammaHeuristic ? "S" : "");
    }

    @Override
    protected void closeImpl() {
        arrSetNonDominatedFront = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        Solution[] solutions = new Solution[points.length];
        for (int i = 0; i < solutions.length; ++i) {
            solutions[i] = new Solution(i);
            solutions[i].objectives = points[i];
        }
        sortDCNS(solutions, ranks);
    }

    private static <T> List<T>[] createArrayOfArrayLists(int arraySize) {
        @SuppressWarnings("unchecked") List<T>[] rv = new ArrayList[arraySize];
        for (int i = 0; i < arraySize; ++i) {
            rv[i] = new ArrayList<>();
        }
        return rv;
    }

    private void sortDCNS(Solution[] population, int[] outputRanks) {
        int n = population.length;
        Arrays.sort(population);
        arrSetNonDominatedFront = createArrayOfArrayLists(n);

        for(int i = 0; i < n; i++) {
            List<Solution> ndf = new ArrayList<>(1);
            ndf.add(population[i]);
            arrSetNonDominatedFront[i].add(ndf);
        }
        
        int treeLevel = MathEx.log2up(n);

        if (useGammaHeuristic) {
            if (useBinarySearch) {
                for (int i = 0; i < treeLevel; i++) {
                    int x = ((n - 1) >>> (i + 1)) + 1;
                    for (int j = 0; j < x; j++) {
                        int a = j << (i + 1);
                        int b = a + (1 << i);
                        if (b < n) {
                            Merge_BSS(arrSetNonDominatedFront[a], arrSetNonDominatedFront[b]);
                        }
                    }
                }
            } else {
                for (int i = 0; i < treeLevel; i++) {
                    int x = ((n - 1) >>> (i + 1)) + 1;
                    for (int j = 0; j < x; j++) {
                        int a = j << (i + 1);
                        int b = a + (1 << i);
                        if (b < n) {
                            Merge_SSS(arrSetNonDominatedFront[a], arrSetNonDominatedFront[b]);
                        }
                    }
                }
            }
        } else {
            if (useBinarySearch) {
                for (int i = 0; i < treeLevel; i++) {
                    int x = ((n - 1) >>> (i + 1)) + 1;
                    for (int j = 0; j < x; j++) {
                        int a = j << (i + 1);
                        int b = a + (1 << i);
                        if (b < n) {
                            Merge_BS(arrSetNonDominatedFront[a], arrSetNonDominatedFront[b]);
                        }
                    }
                }
            } else {
                for (int i = 0; i < treeLevel; i++) {
                    int x = ((n - 1) >>> (i + 1)) + 1;
                    for (int j = 0; j < x; j++) {
                        int a = j << (i + 1);
                        int b = a + (1 << i);
                        if (b < n) {
                            Merge_SS(arrSetNonDominatedFront[a], arrSetNonDominatedFront[b]);
                        }
                    }
                }
            }
        }

        List<List<Solution>> ranks = arrSetNonDominatedFront[0];
        for (int i = 0, num = ranks.size(); i < num; ++i) {
            for (Solution x : ranks.get(i)) {
                outputRanks[x.id] = i;
            }
            ranks.get(i).clear();
        }
        arrSetNonDominatedFront = null;
    }

    // Implementation of the SS version

    private static void Merge_SS(List<List<Solution>> targetFronts, List<List<Solution>> sourceFronts) {
        int alpha = -1;
        for (List<Solution> front : sourceFronts) {
            if (++alpha == targetFronts.size()) {
                targetFronts.add(front);
            } else {
                alpha = Insert_Front_SS(targetFronts, front, alpha);
            }
        }
    }

    private static int Insert_Front_SS(List<List<Solution>> targetFronts, List<Solution> theFront, int alpha) {
        int P = targetFronts.size();
        int hfi = P;
        for (Solution sol : theFront) {
            hfi = Math.min(hfi, Insert_SS(targetFronts, sol, P, alpha));
        }
        return hfi;
    }

    private static int Insert_SS(List<List<Solution>> fronts, Solution sol, int P, int alpha) {
        for (int p = alpha; p < P; p++) {
            List<Solution> front = fronts.get(p);
            if (frontDoesNotDominate(front, sol, front.size() - 1)) {
                return insertInExistingFront(front, sol, p);
            }
        }
        return insertMaybeInNewFront(fronts, sol, P);
    }

    // Implementation of the BS version

    private static void Merge_BS(List<List<Solution>> targetFronts, List<List<Solution>> sourceFronts) {
        int alpha = -1;
        for (List<Solution> front : sourceFronts) {
            if (++alpha == targetFronts.size()) {
                targetFronts.add(front);
            } else {
                alpha = Insert_Front_BS(targetFronts, front, alpha);
            }
        }
    }

    private static int Insert_Front_BS(List<List<Solution>> targetFronts, List<Solution> theFront, int alpha) {
        int P = targetFronts.size();
        int hfi = P;
        for (Solution sol : theFront) {
            hfi = Math.min(hfi, Insert_BS(targetFronts, sol, P, alpha));
        }
        return hfi;
    }

    private static int Insert_BS(List<List<Solution>> fronts, Solution sol, int P, int alpha) {
        List<Solution> first = fronts.get(alpha);
        if (frontDoesNotDominate(first, sol, first.size() - 1)) {
            return insertInExistingFront(first, sol, alpha);
        }
        int min = alpha, max = P;
        while (max - min > 1) {
            int mid = (min + max) >>> 1;
            List<Solution> front = fronts.get(mid);
            if (frontDoesNotDominate(front, sol, front.size() - 1)) {
                max = mid;
            } else {
                min = mid;
            }
        }
        return insertMaybeInNewFront(fronts, sol, max);
    }

    // Implementation of the SSS version

    private void Merge_SSS(List<List<Solution>> targetFronts, List<List<Solution>> sourceFronts) {
        int alpha = -1;
        for (List<Solution> front : sourceFronts) {
            if (++alpha == targetFronts.size()) {
                targetFronts.add(front);
            } else {
                alpha = Insert_Front_SSS(targetFronts, front, alpha);
            }
        }
    }

    private int Insert_Front_SSS(List<List<Solution>> targetFronts, List<Solution> theFront, int alpha) {
        int P = targetFronts.size();
        int hfi = P;
        gammaFrontIndex = -1;
        gammaNoSolution = -1;
        for (Solution sol : theFront) {
            hfi = Math.min(hfi, Insert_SSS(targetFronts, sol, P, alpha));
        }
        return hfi;
    }

    private int Insert_SSS(List<List<Solution>> fronts, Solution sol, int P, int alpha) {
        for (int p = alpha; p < P ; p++) {
            List<Solution> front = fronts.get(p);
            int sizeOfFront = getFrontSizeSS(front, p);
            if (frontDoesNotDominate(front, sol, sizeOfFront - 1)) {
                return insertInExistingFrontSS(front, sol, p);
            }
        }
        return insertMaybeInNewFrontSS(fronts, sol, P);
    }

    // Implementation of the BSS version

    private void Merge_BSS(List<List<Solution>> targetFronts, List<List<Solution>> sourceFronts) {
        int alpha = -1;
        for (List<Solution> front : sourceFronts) {
            if (++alpha == targetFronts.size()) {
                targetFronts.add(front);
            } else {
                alpha = Insert_Front_BSS(targetFronts, front, alpha);
            }
        }
    }

    private int Insert_Front_BSS(List<List<Solution>> targetFronts, List<Solution> theFront, int alpha) {
        int P = targetFronts.size();
        int hfi = P;
        gammaFrontIndex = -1;
        gammaNoSolution = -1;
        for (Solution sol : theFront) {
            hfi = Math.min(hfi, Insert_BSS(targetFronts, sol, P, alpha));
        }
        return hfi;
    }
    
    private int Insert_BSS(List<List<Solution>> fronts, Solution sol, int P, int alpha) {
        List<Solution> first = fronts.get(alpha);
        int firstSize = getFrontSizeSS(first, alpha);
        if (frontDoesNotDominate(first, sol, firstSize - 1)) {
            return insertInExistingFrontSS(first, sol, alpha);
        }

        int min = alpha, max = P;
        while (max - min > 1) {
            int mid = (min + max) >>> 1;
            List<Solution> front = fronts.get(mid);
            int frontSize = getFrontSizeSS(front, mid);
            if (frontDoesNotDominate(front, sol, frontSize - 1)) {
                max = mid;
            } else {
                min = mid;
            }
        }
        return insertMaybeInNewFrontSS(fronts, sol, max);
    }

    // Various helper functions

    private static boolean frontDoesNotDominate(List<Solution> front, Solution sol, int startFrom) {
        double[] solArray = sol.objectives;
        int dim = solArray.length;
        for (int i = startFrom; i >= 0; --i) {
            if (DominanceHelper.strictlyDominates(front.get(i).objectives, solArray, dim)) {
                return false;
            }
        }
        return true;
    }

    private int getFrontSizeSS(List<Solution> front, int rank) {
        return rank == gammaFrontIndex ? gammaNoSolution : front.size();
    }

    private static int insertMaybeInNewFront(List<List<Solution>> fronts, Solution sol, int rank) {
        if (rank == fronts.size()) {
            List<Solution> newFront = new ArrayList<>(1);
            newFront.add(sol);
            fronts.add(newFront);
        } else {
            fronts.get(rank).add(sol);
        }
        return rank;
    }

    private int insertMaybeInNewFrontSS(List<List<Solution>> fronts, Solution sol, int rank) {
        insertMaybeInNewFront(fronts, sol, rank);
        if (rank != gammaFrontIndex) {
            gammaFrontIndex = rank;
            gammaNoSolution = fronts.get(rank).size();
        }
        return rank;
    }

    private static int insertInExistingFront(List<Solution> front, Solution sol, int rank) {
        front.add(sol);
        return rank;
    }

    private int insertInExistingFrontSS(List<Solution> front, Solution sol, int rank) {
        front.add(sol);
        if (rank != gammaFrontIndex) {
            gammaFrontIndex = rank;
            gammaNoSolution = front.size();
        }
        return rank;
    }

    static class Solution implements Comparable<Solution> {
        private final int id;
        private double[] objectives;

        Solution(int id) {
            this.id = id;
        }

        public int compareTo(Solution that) {
            double[] thatObjectives = that.objectives;
            int noObjectives = objectives.length;
            for (int i = 0; i < noObjectives; i++) {
                double l = objectives[i], r = thatObjectives[i];
                if (l < r) {
                    return -1;
                }
                if (l > r) {
                    return 1;
                }
            }
            return 0;
        }
    }
}
