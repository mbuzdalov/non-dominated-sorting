package ru.ifmo.nds.dcns;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.nds.NonDominatedSorting;

/**
 * This is an implementation of DCNS algorithms by Sumit Mishra.
 *
 * @author Sumit Mishra (idea and initial implementation)
 * @author Maxim Buzdalov (minor cleanup and adaptation to framework interfaces)
 */
public class SumitImplementation2016 extends NonDominatedSorting {
    private List<ArrayList<Integer>>[] arrSetNonDominatedFront;
    private Gamma gamma;
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
        gamma = null;
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
        
        int treeLevel = (int) Math.ceil(Math.log10(n) / Math.log10(2));
        
        if (searchType == 0 && spaceReq == 0) { // DCNS-SS
            for (int i = 0; i < treeLevel; i++) {
                int x = (int) Math.ceil(n / (Math.pow(2, i + 1)));
                for (int j = 0; j < x; j++) {
                    int a = (int) Math.pow(2, i + 1) * j;
                    int b = a + (int) Math.pow(2, i);
                    if (b < n) {
                        Merge_SS(a, b, population);
                    }
                }
            }
        } else if (searchType == 1 && spaceReq == 0) { // DCNS-BS
            for (int i = 0; i < treeLevel; i++) {
                int x = (int) Math.ceil(n / (Math.pow(2, i + 1)));
                for (int j = 0; j < x; j++) {
                    int a = (int) Math.pow(2, i + 1) * j;
                    int b = a + (int) Math.pow(2, i);
                    if (b < n) {
                        Merge_BS(a, b, population);
                    }
                }
            }
        } else if (searchType == 0 && spaceReq == 1) { // DCNS-SSS
            for (int i = 0; i < treeLevel; i++) {
                int x = (int) Math.ceil(n / (Math.pow(2, i + 1)));
                for (int j = 0; j < x; j++) {
                    int a = (int) Math.pow(2, i + 1) * j;
                    int b = a + (int) Math.pow(2, i);
                    if (b < n) {
                        Merge_SSS(a, b, population);
                    }
                }
            }
        } else {  // searchType == 1 && spaceReq == 1, // DCNS-SSS
            for (int i = 0; i < treeLevel; i++) {
                int x = (int) Math.ceil(n / (Math.pow(2, i + 1)));
                for (int j = 0; j < x; j++) {
                    int a = (int) Math.pow(2, i + 1) * j;
                    int b = a + (int) Math.pow(2, i);
                    if (b < n) {
                        Merge_BSS(a, b, population);
                    }
                }
            }
        }   

        // Hell knows how it shall work...
        List<ArrayList<Integer>> ranks = arrSetNonDominatedFront[0];
        for (int i = 0, num = ranks.size(); i < num; ++i) {
            for (int x : ranks.get(i)) {
                outputRanks[x] = i;
            }
            ranks.get(i).clear();
        }
        arrSetNonDominatedFront = null;
    }
    
    private void Merge_SS(int i, int j, Solution[] population) {
        int alpha = -1;
        int q = 0;
        while (q < arrSetNonDominatedFront[j].size()) {
            alpha = Insert_Front_SS(i, j, q, alpha + 1, population);
            if (alpha == arrSetNonDominatedFront[i].size() - 1) {
                q++;
                break;
            }
            q++;
        } 
        /* Add remaining fronts without comparisons */
        while (q < arrSetNonDominatedFront[j].size()) {
            arrSetNonDominatedFront[i].add(new ArrayList<>(arrSetNonDominatedFront[j].get(q)));
            q++;
        }
    }
    
    private void Merge_BS(int i, int j, Solution[] population) {
        int alpha = -1;
        int q = 0;
        while (q < arrSetNonDominatedFront[j].size()) {
            alpha = Insert_Front_BS(i, j, q, alpha + 1, population);
            if (alpha == arrSetNonDominatedFront[i].size() - 1) {
                q++;
                break;
            }
            q++;
        } 
        /* Add remaining fronts without comparisons */
        while (q < arrSetNonDominatedFront[j].size()) {
            arrSetNonDominatedFront[i].add(new ArrayList<>(arrSetNonDominatedFront[j].get(q)));
            q++;
        }
    }
    
    private void Merge_SSS(int i, int j, Solution[] population) {
        int alpha = -1;
        int q = 0;
        while (q < arrSetNonDominatedFront[j].size()) {
            alpha = Insert_Front_SSS(i, j, q, alpha + 1, population);
            if (alpha == arrSetNonDominatedFront[i].size() - 1) {
                q++;
                break;
            }
            q++;
        } 
        /* Add remaining fronts without comparisons */
        while (q < arrSetNonDominatedFront[j].size()) {
            arrSetNonDominatedFront[i].add(new ArrayList<>(arrSetNonDominatedFront[j].get(q)));
            q++;
        }
    }
    
    private void Merge_BSS(int i, int j, Solution[] population) {
        int alpha = -1;
        int q = 0;
        while (q < arrSetNonDominatedFront[j].size()) {
            alpha = Insert_Front_BSS(i, j, q, alpha + 1, population);
            if (alpha == arrSetNonDominatedFront[i].size() - 1) {
                q++;
                break;
            }
            q++;
        } 
        /* Add remaining fronts without comparisons */
        while (q < arrSetNonDominatedFront[j].size()) {
            arrSetNonDominatedFront[i].add(new ArrayList<>(arrSetNonDominatedFront[j].get(q)));
            q++;
        }
    }
    
    
    private int Insert_Front_SS(int i, int j, int q, int alpha, Solution[] population) {
        int P = arrSetNonDominatedFront[i].size();
        int hfi = P;  //Because indexing starts from 0 in Java
        for (int u = 0; u < arrSetNonDominatedFront[j].get(q).size(); u++) {
            int sol = arrSetNonDominatedFront[j].get(q).get(u);
            hfi = Insert_SS(population, i, sol, P, alpha, hfi);
        }
        return hfi;
    }
    
    private int Insert_Front_BS(int i, int j, int q, int alpha, Solution[] population) {
        int P = arrSetNonDominatedFront[i].size();
        int hfi = P;  //Because indexing starts from 0 in Java
        for (int u = 0; u < arrSetNonDominatedFront[j].get(q).size(); u++) {
            int sol = arrSetNonDominatedFront[j].get(q).get(u);
            hfi = Insert_BS(population, i, sol, P, alpha, hfi);
        }
        return hfi;
    }
    
    private int Insert_Front_SSS(int i, int j, int q, int alpha, Solution[] population) {
        int P = arrSetNonDominatedFront[i].size();
        int hfi = P;  //Because indexing starts from 0 in Java
        gamma = new Gamma(-1,-1);
        for (int u = 0; u < arrSetNonDominatedFront[j].get(q).size(); u++) {
            int sol = arrSetNonDominatedFront[j].get(q).get(u);
            hfi = Insert_SSS(population, i, sol, P, alpha, hfi);
        }
        return hfi;
    }

    private int Insert_Front_BSS(int i, int j, int q, int alpha, Solution[] population) {
        int P = arrSetNonDominatedFront[i].size();
        int hfi = P;  //Because indexing starts from 0 in Java
        gamma = new Gamma(-1,-1);
        for (int u = 0; u < arrSetNonDominatedFront[j].get(q).size(); u++) {
            int sol = arrSetNonDominatedFront[j].get(q).get(u);
            hfi = Insert_BSS(population, i, sol, P, alpha, hfi);
        }
        return hfi;
    }
    
    private int Insert_SS(Solution[] population, int i, int sol, int P, int alpha, int hfi) {
        boolean isInserted = false;
        for (int p = alpha; p < P ; p++) {
            int count = 0;
            for (int u = arrSetNonDominatedFront[i].get(p).size() - 1; u >= 0; u--) {
                int isdom = population[arrSetNonDominatedFront[i].get(p).get(u)].dominates(population[sol]);
                if (isdom == 0) { //solution sol is non-dominated with the solution in the existing front
                    count++;
                } else if (isdom == 1) { //solution sol is dominated by the solution in the existing front
                    break;
                } else {
                    throw new AssertionError();
                }
            }
            if (count == arrSetNonDominatedFront[i].get(p).size()) {
                arrSetNonDominatedFront[i].get(p).add(sol);
                isInserted = true;
                if (p < hfi) {
                    hfi = p;
                }
                break;
            }  
        }
        if (!isInserted) {
            if (arrSetNonDominatedFront[i].size() == P) {
                ArrayList<Integer> ndf = new ArrayList<>();
                ndf.add(sol);
                arrSetNonDominatedFront[i].add(ndf);
            } else {
                arrSetNonDominatedFront[i].get(P).add(sol);
            }  
        }
        return hfi;
    }
     
    private int Insert_BS(Solution[] population, int i, int sol, int P, int alpha, int hfi) {
        int min = alpha;
        int max = P - 1;
        int mid = (min + max) / 2;
        int count;
        while (true) {
            count = 0;
            for (int u = arrSetNonDominatedFront[i].get(mid).size() - 1; u >= 0; u--) {
                int isdom = population[arrSetNonDominatedFront[i].get(mid).get(u)].dominates(population[sol]);
                if (isdom == 0) { //solution sol is non-dominated with the solution in the existing front
                    count++;
                } else if (isdom == 1) { //solution sol is dominated by the solution in the existing front
                    break;
                } else {
                    throw new AssertionError();
                }
            }
            if (count == arrSetNonDominatedFront[i].get(mid).size()) {
                if (mid == min) {
                    arrSetNonDominatedFront[i].get(mid).add(sol);
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
                    if (arrSetNonDominatedFront[i].size() == P) {
                        ArrayList<Integer> ndf = new ArrayList<>();
                        ndf.add(sol);
                        arrSetNonDominatedFront[i].add(ndf);
                    } else {
                        arrSetNonDominatedFront[i].get(P).add(sol);
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
    
    private int Insert_SSS(Solution[] population, int i, int sol, int P, int alpha, int hfi) {
        boolean isInserted = false;
        for (int p = alpha; p < P ; p++) {
            int count = 0;
            int sizeOfFront = arrSetNonDominatedFront[i].get(p).size();
            if (p == gamma.getFrontIndex()) {
                sizeOfFront = gamma.getNoSolution();
            }
            for (int u = sizeOfFront-1; u >= 0; u--) {
                int isdom = population[arrSetNonDominatedFront[i].get(p).get(u)].dominates(population[sol]);
                if (isdom == 0) { //solution sol is non-dominated with the solution in the existing front
                    count++;
                } else if (isdom == 1) { //solution sol is dominated by the solution in the existing front
                    break;
                } else {
                    throw new AssertionError();
                }
            }
            if (count == sizeOfFront) {
                arrSetNonDominatedFront[i].get(p).add(sol);
                if (p != gamma.getFrontIndex()) {
                    gamma.setFrontIndex(p);
                    gamma.setNoSolution(count);
                }
                isInserted = true;
                if (p < hfi) {
                    hfi = p;
                }
                break;
            }  
        }
        if (!isInserted) {
            if (arrSetNonDominatedFront[i].size() == P) {
                ArrayList<Integer> ndf = new ArrayList<>();
                ndf.add(sol);
                arrSetNonDominatedFront[i].add(ndf);
            } else {
                arrSetNonDominatedFront[i].get(P).add(sol);
            }  
        }
        return hfi;
    }

    private int Insert_BSS(Solution[] population, int i, int sol, int P, int alpha, int hfi) {
        int min = alpha;
        int max = P - 1;
        int mid = (min + max) / 2;
        int count;
        while (true) {
            count = 0;
            int sizeOfFront = arrSetNonDominatedFront[i].get(mid).size();
            if (mid == gamma.getFrontIndex()) {
                sizeOfFront = gamma.getNoSolution();
            }
            for (int u = sizeOfFront-1; u >= 0; u--) {
                int isdom = population[arrSetNonDominatedFront[i].get(mid).get(u)].dominates(population[sol]);
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
                    arrSetNonDominatedFront[i].get(mid).add(sol);
                    if (mid != gamma.getFrontIndex()) {
                        gamma.setFrontIndex(mid);
                        gamma.setNoSolution(count);
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
                    if (arrSetNonDominatedFront[i].size() == P) {
                        ArrayList<Integer> ndf = new ArrayList<>();
                        ndf.add(sol);
                        arrSetNonDominatedFront[i].add(ndf);
                    } else {
                        arrSetNonDominatedFront[i].get(P).add(sol);
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
            int noObjectives = sol.objectives.length;
            for (int i = 0; i < noObjectives; i++) {
                if (this.objectives[i] < sol.objectives[i]) {
                    flag1 = true;
                } else if (this.objectives[i] > sol.objectives[i]) {
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
            int noObjectives = objectives.length;
            for (int i = 0; i < noObjectives; i++) {
                if(this.objectives[i] < sol.objectives[i]) {
                    return 1;
                } else if (this.objectives[i] > sol.objectives[i]) {
                    return -1;
                }
            }
            return 0;
        }
    }

    // To heapify a subtree rooted with node i which is
    // an index in arr[]. n is size of heap
    private void heapifyFirstObjective(int arr[], int heapSize, int i, Solution population[])
    {
        int largest = i;  // Initialize largest as root
        int l = 2 * i + 1;  // left = 2*i + 1
        int r = 2 * i + 2;  // right = 2*i + 2

        // If left child is larger than root
        /*if (l < heapSize && population[arr[l]].isBig(population[arr[largest]]) == 1)
            largest = l;*/
        if (l < heapSize && population[arr[l]].isSmall(population[arr[largest]]) == -1)
            largest = l;

        // If right child is larger than largest so far
        /*if (r < heapSize && population[arr[r]].isBig(population[arr[largest]]) == 1)
            largest = r;*/
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

    private int[] preSortDCNS(Solution population[]) {
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

    private static class Gamma { // Used to reduce the number of dominance comparisons by taking constant space
        private int frontIndex;
        private int noSolution;

        Gamma(int frontIndex, int noSolution) {
            this.frontIndex = frontIndex;
            this.noSolution = noSolution;
        }

        int getFrontIndex() {
            return frontIndex;
        }

        int getNoSolution() {
            return noSolution;
        }

        void setFrontIndex(int frontIndex) {
            this.frontIndex = frontIndex;
        }

        void setNoSolution(int noSolution) {
            this.noSolution = noSolution;
        }
    }
}
