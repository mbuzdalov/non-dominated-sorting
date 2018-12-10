package ru.ifmo.nds.dcns;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.MathEx;

/**
 * This is an implementation of DCNS algorithms by Sumit Mishra.
 *
 * @author Sumit Mishra (idea and initial implementation)
 * @author Maxim Buzdalov (minor cleanup and adaptation to framework interfaces)
 */
public class SumitImplementation2016 extends NonDominatedSorting {
    private List<List<Integer>>[] arrSetNonDominatedFront;
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
        sortDCNS(solutions, useBinarySearch ? 1 : 0, useGammaHeuristic ? 1 : 0, ranks);
    }

    private static <T> List<T>[] createArrayOfArrayLists(int arraySize) {
        @SuppressWarnings("unchecked") List<T>[] rv = new ArrayList[arraySize];
        for (int i = 0; i < arraySize; ++i) {
            rv[i] = new ArrayList<>();
        }
        return rv;
    }

    private void sortDCNS(Solution[] population, int searchType, int spaceReq, int[] outputRanks) {
        int n = population.length;
        arrSetNonDominatedFront = createArrayOfArrayLists(n);
        int[] Q0 = preSortDCNS(population);

        for(int i = 0; i < n; i++) {
            ArrayList<Integer> ndf = new ArrayList<>();
            ndf.add(Q0[i]);
            arrSetNonDominatedFront[i].add(ndf);
        }
        
        int treeLevel = MathEx.log2up(n);
        
        if (searchType == 0 && spaceReq == 0) { // DCNS-SS
            for (int i = 0; i < treeLevel; i++) {
                int x = ((n - 1) >>> (i + 1)) + 1;
                for (int j = 0; j < x; j++) {
                    int a = j << (i + 1);
                    int b = a + (1 << i);
                    if (b < n) {
                        Merge_SS(arrSetNonDominatedFront[a], arrSetNonDominatedFront[b], population);
                    }
                }
            }
        } else if (searchType == 1 && spaceReq == 0) { // DCNS-BS
            for (int i = 0; i < treeLevel; i++) {
                int x = ((n - 1) >>> (i + 1)) + 1;
                for (int j = 0; j < x; j++) {
                    int a = j << (i + 1);
                    int b = a + (1 << i);
                    if (b < n) {
                        Merge_BS(arrSetNonDominatedFront[a], arrSetNonDominatedFront[b], population);
                    }
                }
            }
        } else if (searchType == 0 && spaceReq == 1) { // DCNS-SSS
            for (int i = 0; i < treeLevel; i++) {
                int x = ((n - 1) >>> (i + 1)) + 1;
                for (int j = 0; j < x; j++) {
                    int a = j << (i + 1);
                    int b = a + (1 << i);
                    if (b < n) {
                        Merge_SSS(arrSetNonDominatedFront[a], arrSetNonDominatedFront[b], population);
                    }
                }
            }
        } else {  // searchType == 1 && spaceReq == 1, // DCNS-SSS
            for (int i = 0; i < treeLevel; i++) {
                int x = ((n - 1) >>> (i + 1)) + 1;
                for (int j = 0; j < x; j++) {
                    int a = j << (i + 1);
                    int b = a + (1 << i);
                    if (b < n) {
                        Merge_BSS(arrSetNonDominatedFront[a], arrSetNonDominatedFront[b], population);
                    }
                }
            }
        }   

        List<List<Integer>> ranks = arrSetNonDominatedFront[0];
        for (int i = 0, num = ranks.size(); i < num; ++i) {
            for (int x : ranks.get(i)) {
                outputRanks[x] = i;
            }
            ranks.get(i).clear();
        }
        arrSetNonDominatedFront = null;
    }
    
    private static void Merge_SS(List<List<Integer>> targetFronts, List<List<Integer>> sourceFronts, Solution[] population) {
        int alpha = -1;
        int q = 0;
        while (q < sourceFronts.size()) {
            alpha = Insert_Front_SS(targetFronts, sourceFronts.get(q), alpha + 1, population);
            if (alpha == targetFronts.size() - 1) {
                q++;
                break;
            }
            q++;
        } 
        /* Add remaining fronts without comparisons */
        while (q < sourceFronts.size()) {
            targetFronts.add(sourceFronts.get(q));
            q++;
        }
    }
    
    private static void Merge_BS(List<List<Integer>> targetFronts, List<List<Integer>> sourceFronts, Solution[] population) {
        int alpha = -1;
        int q = 0;
        while (q < sourceFronts.size()) {
            alpha = Insert_Front_BS(targetFronts, sourceFronts.get(q), alpha + 1, population);
            if (alpha == targetFronts.size() - 1) {
                q++;
                break;
            }
            q++;
        } 
        /* Add remaining fronts without comparisons */
        while (q < sourceFronts.size()) {
            targetFronts.add(sourceFronts.get(q));
            q++;
        }
    }
    
    private void Merge_SSS(List<List<Integer>> targetFronts, List<List<Integer>> sourceFronts, Solution[] population) {
        int alpha = -1;
        int q = 0;
        while (q < sourceFronts.size()) {
            alpha = Insert_Front_SSS(targetFronts, sourceFronts.get(q), alpha + 1, population);
            if (alpha == targetFronts.size() - 1) {
                q++;
                break;
            }
            q++;
        } 
        /* Add remaining fronts without comparisons */
        while (q < sourceFronts.size()) {
            targetFronts.add(sourceFronts.get(q));
            q++;
        }
    }
    
    private void Merge_BSS(List<List<Integer>> targetFronts, List<List<Integer>> sourceFronts, Solution[] population) {
        int alpha = -1;
        int q = 0;
        while (q < sourceFronts.size()) {
            alpha = Insert_Front_BSS(targetFronts, sourceFronts.get(q), alpha + 1, population);
            if (alpha == targetFronts.size() - 1) {
                q++;
                break;
            }
            q++;
        } 
        /* Add remaining fronts without comparisons */
        while (q < sourceFronts.size()) {
            targetFronts.add(sourceFronts.get(q));
            q++;
        }
    }
    
    
    private static int Insert_Front_SS(List<List<Integer>> targetFronts, List<Integer> theFront, int alpha, Solution[] population) {
        int P = targetFronts.size();
        int hfi = P;
        for (int sol : theFront) {
            hfi = Insert_SS(population, targetFronts, sol, P, alpha, hfi);
        }
        return hfi;
    }
    
    private static int Insert_Front_BS(List<List<Integer>> targetFronts, List<Integer> theFront, int alpha, Solution[] population) {
        int P = targetFronts.size();
        int hfi = P;
        for (int sol : theFront) {
            hfi = Insert_BS(population, targetFronts, sol, P, alpha, hfi);
        }
        return hfi;
    }
    
    private int Insert_Front_SSS(List<List<Integer>> targetFronts, List<Integer> theFront, int alpha, Solution[] population) {
        int P = targetFronts.size();
        int hfi = P;
        gammaFrontIndex = -1;
        gammaNoSolution = -1;
        for (int sol : theFront) {
            hfi = Insert_SSS(population, targetFronts, sol, P, alpha, hfi);
        }
        return hfi;
    }

    private int Insert_Front_BSS(List<List<Integer>> targetFronts, List<Integer> theFront, int alpha, Solution[] population) {
        int P = targetFronts.size();
        int hfi = P;
        gammaFrontIndex = -1;
        gammaNoSolution = -1;
        for (int sol : theFront) {
            hfi = Insert_BSS(population, targetFronts, sol, P, alpha, hfi);
        }
        return hfi;
    }
    
    private static int Insert_SS(Solution[] population, List<List<Integer>> fronts, int sol, int P, int alpha, int hfi) {
        boolean isInserted = false;
        for (int p = alpha; p < P ; p++) {
            int count = 0;
            for (int u = fronts.get(p).size() - 1; u >= 0; u--) {
                int isdom = population[fronts.get(p).get(u)].dominates(population[sol]);
                if (isdom == 0) { //solution sol is non-dominated with the solution in the existing front
                    count++;
                } else if (isdom == 1) { //solution sol is dominated by the solution in the existing front
                    break;
                } else {
                    throw new AssertionError();
                }
            }
            if (count == fronts.get(p).size()) {
                fronts.get(p).add(sol);
                isInserted = true;
                if (p < hfi) {
                    hfi = p;
                }
                break;
            }  
        }
        if (!isInserted) {
            if (fronts.size() == P) {
                ArrayList<Integer> ndf = new ArrayList<>();
                ndf.add(sol);
                fronts.add(ndf);
            } else {
                fronts.get(P).add(sol);
            }  
        }
        return hfi;
    }
     
    private static int Insert_BS(Solution[] population, List<List<Integer>> fronts, int sol, int P, int alpha, int hfi) {
        int min = alpha;
        int max = P - 1;
        int mid = (min + max) / 2;
        int count;
        while (true) {
            count = 0;
            for (int u = fronts.get(mid).size() - 1; u >= 0; u--) {
                int isdom = population[fronts.get(mid).get(u)].dominates(population[sol]);
                if (isdom == 0) { //solution sol is non-dominated with the solution in the existing front
                    count++;
                } else if (isdom == 1) { //solution sol is dominated by the solution in the existing front
                    break;
                } else {
                    throw new AssertionError();
                }
            }
            if (count == fronts.get(mid).size()) {
                if (mid == min) {
                    fronts.get(mid).add(sol);
                    if(mid < hfi) {
                        hfi = mid;
                    }
                    break;
                } else {
                    max = mid;
                    mid = (min + max) / 2;
                }  
            } else {
                if (min == P - 1) {
                    if (fronts.size() == P) {
                        ArrayList<Integer> ndf = new ArrayList<>();
                        ndf.add(sol);
                        fronts.add(ndf);
                    } else {
                        fronts.get(P).add(sol);
                    }  
                    break;
                } else {
                    min = mid + 1;
                    mid = (min + max) / 2;
                }
            }
        }
        return hfi;
    } 
    
    private int Insert_SSS(Solution[] population, List<List<Integer>> fronts, int sol, int P, int alpha, int hfi) {
        boolean isInserted = false;
        for (int p = alpha; p < P ; p++) {
            int count = 0;
            int sizeOfFront = fronts.get(p).size();
            if (p == gammaFrontIndex) {
                sizeOfFront = gammaNoSolution;
            }
            for (int u = sizeOfFront - 1; u >= 0; u--) {
                int isdom = population[fronts.get(p).get(u)].dominates(population[sol]);
                if (isdom == 0) { //solution sol is non-dominated with the solution in the existing front
                    count++;
                } else if (isdom == 1) { //solution sol is dominated by the solution in the existing front
                    break;
                } else {
                    throw new AssertionError();
                }
            }
            if (count == sizeOfFront) {
                fronts.get(p).add(sol);
                if (p != gammaFrontIndex) {
                    gammaFrontIndex = p;
                    gammaNoSolution = count;
                }
                isInserted = true;
                if (p < hfi) {
                    hfi = p;
                }
                break;
            }  
        }
        if (!isInserted) {
            if (fronts.size() == P) {
                ArrayList<Integer> ndf = new ArrayList<>();
                ndf.add(sol);
                fronts.add(ndf);
            } else {
                fronts.get(P).add(sol);
            }  
        }
        return hfi;
    }

    private int Insert_BSS(Solution[] population, List<List<Integer>> fronts, int sol, int P, int alpha, int hfi) {
        int min = alpha;
        int max = P - 1;
        int mid = (min + max) / 2;
        int count;
        while (true) {
            count = 0;
            int sizeOfFront = fronts.get(mid).size();
            if (mid == gammaFrontIndex) {
                sizeOfFront = gammaNoSolution;
            }
            for (int u = sizeOfFront - 1; u >= 0; u--) {
                int isdom = population[fronts.get(mid).get(u)].dominates(population[sol]);
                if (isdom == 0) { //solution sol is non-dominated with the solution in the existing front
                    count++;
                } else if (isdom == 1) { //solution sol is dominated by the solution in the existing front
                    break;
                } else {
                    throw new AssertionError();
                }
            }
            if (count == sizeOfFront) {
                if (mid == min) {
                    fronts.get(mid).add(sol);
                    if (mid != gammaFrontIndex) {
                        gammaFrontIndex = mid;
                        gammaNoSolution = count;
                    }
                    if (mid < hfi) {
                        hfi = mid;
                    }
                    break;
                } else {
                    max = mid;
                    mid = (min + max) / 2;
                }  
            } else {
                if (min == P - 1) {
                    if (fronts.size() == P) {
                        ArrayList<Integer> ndf = new ArrayList<>();
                        ndf.add(sol);
                        fronts.add(ndf);
                    } else {
                        fronts.get(P).add(sol);
                    }  
                    break;
                } else {
                    min = mid + 1;
                    mid = (min + max) / 2;
                }
            }
        }
        return hfi;
    }

    static class Solution {
        private final int id;
        private double[] objectives;

        Solution(int id) {
            this.id = id;
        }

        // MB: this is an inverse domination comparator: 1 if this < sol, -1 if this > sol.
        int dominates(Solution sol) {
            boolean flag1 = false;
            boolean flag2 = false;
            double[] thisObj = this.objectives;
            double[] thatObj = sol.objectives;
            int noObjectives = thisObj.length;
            for (int i = 0; i < noObjectives; i++) {
                double l = thisObj[i], r = thatObj[i];
                if (l < r) {
                    flag1 = true;
                } else if (l > r) {
                    flag2 = true;
                }
            }
            if (flag1 && !flag2) {
                return 1;
            } else if (!flag1 && flag2) {
                return -1;
            } else {
                return 0;
            }
        }

        // MB: this is an inverse lexicographical comparator: 1 if this < sol, -1 if this > sol.
        int isSmall(Solution sol) {
            double[] thisObj = this.objectives;
            double[] thatObj = sol.objectives;
            int noObjectives = thisObj.length;
            for (int i = 0; i < noObjectives; i++) {
                double l = thisObj[i], r = thatObj[i];
                if (l < r) {
                    return 1;
                } else if (l > r) {
                    return -1;
                }
            }
            return 0;
        }
    }

    // To heapify a subtree rooted with node i which is
    // an index in arr[]. n is size of heap
    private static void heapifyFirstObjective(int[] arr, int heapSize, int i, Solution[] population)
    {
        int largest = i;  // Initialize largest as root
        int l = 2 * i + 1;  // left = 2*i + 1
        int r = 2 * i + 2;  // right = 2*i + 2

        // If left child is larger than root
        if (l < heapSize && population[arr[l]].isSmall(population[arr[largest]]) == -1)
            largest = l;

        // If right child is larger than largest so far
        if (r < heapSize && population[arr[r]].isSmall(population[arr[largest]]) == -1)
            largest = r;

        // If largest is not root
        if (largest != i) {
            int swap = arr[i];
            arr[i] = arr[largest];
            arr[largest] = swap;

            // Recursively heapify the affected sub-tree
            heapifyFirstObjective(arr, heapSize, largest, population);
        }
    }

    private static int[] preSortDCNS(Solution[] population) {
        int n = population.length;
        int[] Q0 = new int[n];
        for(int i = 0; i < n; i++) {
            Q0[i] = population[i].id;
        }

        /*-- Sort based on first objective --*/
        // Build heap (rearrange array)
        for (int i = n / 2 - 1; i >= 0; i--)
            heapifyFirstObjective(Q0, n, i, population);

        // One by one extract an element from heap
        for (int i = n - 1; i >= 0; i--) {
            // Move current root to end
            int temp = Q0[0];
            Q0[0] = Q0[i];
            Q0[i] = temp;

            // call max heapify on the reduced heap
            heapifyFirstObjective(Q0, i, 0, population);
        }
        /*-- Sort based on first objective --*/
        return Q0;
    }
}
