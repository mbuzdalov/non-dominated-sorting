package ru.ifmo.nds.bos;

import ru.ifmo.nds.NonDominatedSorting;

import java.util.Arrays;

/**
 * This is an implementation of the Best Order Sort algorithm, published as:
 * <pre>
 * {@literal @}inproceedings{Roy:2016:BOS:2908961.2931684,
 *     author = {Roy, Proteek Chandan and Islam, Md. Monirul and Deb, Kalyanmoy},
 *     title = {Best Order Sort: A New Algorithm to Non-dominated Sorting for Evolutionary Multi-objective Optimization},
 *     booktitle = {Proceedings of Genetic and Evolutionary Computation Conference Companion},
 *     year = {2016},
 *     pages = {1113--1120},
 *     doi = {10.1145/2908961.2931684},
 *     publisher = {ACM},
 *     langid = {english}
 * }
 * </pre>
 *
 * The implementation is taken at 2017, Jul 28 from
 * <a href="https://github.com/Proteek/Best-Order-Sort/">Proteek's GitHub</a>.
 * It is slightly adapted to interfaces and naming conventions,
 * and special case for two objectives is removed.
 *
 * @author Proteek Chandan Roy
 */
public class Proteek extends NonDominatedSorting {
    private int m1 = -1;
    private double[][] population;
    private int[][] allRank;
    private MergeSort mergesort;
    private int[] rank;
    private int totalFront = 0;
    private int n;
    private int m;
    private int[] lexOrder;

    public Proteek(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Best Order Sort (Proteek implementation)";
    }

    @Override
    protected void closeImpl() {}

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        if (points[0].length == 0) {
            Arrays.fill(ranks, 0);
        } else {
            initialize(points);
            bestSort(n, m1);
            System.arraycopy(this.rank, 0, ranks, 0, n);
        }
    }

    private static class LinkedList {
        Node start = null;

        void addStart(int val) {
            start = new Node(val, start);
        }
    }
    private static class Node {
        final int data;
        final Node link;

        Node(int d, Node n) {
            data = d;
            link = n;
        }
    }

    private void bestSort(int n, int m) {
        int total = 0, i2;
        boolean dominated;
        Node head;

        rank = new int[n];
        boolean[] set = new boolean[n];
        LinkedList[][] list = new LinkedList[m][n];
        allRank = new int[n][m];
        lexOrder = new int[n];

        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                list[j][i] = new LinkedList();
                allRank[i][j] = i;
            }
        }

        mergesort.setRank(allRank);
        sortingValues();

        for (int i = 0; i<n;i++) {
            for (int obj = 0; obj < m; obj++) {
                int s = allRank[i][obj];
                if (set[s]) {
                    list[obj][rank[s]].addStart(s);
                    continue;
                }
                set[s] = true;
                total++;
                i2 = 0;
                while (true) {
                    dominated = false;
                    head = list[obj][i2].start;
                    while (head != null) {
                        if(dominates(head.data, s)) {
                            dominated = true;
                            break;
                        }
                        head = head.link;
                    }
                    if (!dominated) {
                        list[obj][i2].addStart(s);
                        rank[s] = i2;
                        break;
                    } else {
                        if (i2 < totalFront) {
                            i2++;
                        } else {
                            totalFront++;
                            rank[s] = totalFront;
                            list[obj][rank[s]].addStart(s);
                            break;
                        }
                    }
                }
            }
            if (total == n) {
                break;
            }
        }
        totalFront++;
    }

    private boolean dominates(int p1, int  p2) {
        boolean equal = true;
        for(int i = 0; i < m; i++) {
            if (population[p1][i] > population[p2][i]) {
                return false;
            } else if (equal && population[p1][i] < population[p2][i]) {
                equal = false;
            }
        }
        return !equal;
    }

    private void sortingValues() {
        mergesort.sort();
        allRank = mergesort.getRank();
        for (int j = 1; j < n; j++) {
            lexOrder[allRank[j][0]] = j;
        }
        mergesort.setLexOrder(lexOrder);
        for (int j = 1; j < m1; j++) {
            mergesort.sortSpecific(j);
        }
        allRank = mergesort.getRank();
    }

    private void initialize(double[][] population2) {
        population = population2;
        mergesort = new MergeSort();
        n = population.length;
        m = population[0].length;
        mergesort.setPopulation(population);
        m1 = m; //change this value to m1 = log(n) when m is very high
    }

    private static class MergeSort {
        int[] helper;
        double[][] population;
        int n;
        int obj;
        boolean check;
        int[][] rank;
        int[] lexOrder;


        void setRank(int[][] rank) {
            this.rank = rank;
        }

        int[][] getRank() {
            return rank;
        }

        void setPopulation(double[][] pop) {
            this.population = pop;
            this.n = population.length;
            helper = new int[n];
        }

        void setLexOrder(int[] order) {
            this.lexOrder = order;
        }

        void sort() {
            this.obj = 0;
            n = population.length;
            mergeSort(0, n-1);
        }

        void mergeSort(int low, int high) {
            if (low < high) {
                int middle = low + (high - low) / 2;
                mergeSort(low, middle);
                mergeSort(middle + 1, high);
                merge(low, middle, high);
            }
        }

        void merge(int low, int middle, int high) {
            for (int i = low; i <= high; i++) {
                helper[i] = rank[i][obj];
            }

            int i = low;
            int j = middle + 1;
            int k = low;
            while (i <= middle && j <= high) {
                if (population[helper[i]][obj] < population[helper[j]][obj]) {
                    rank[k][obj] = helper[i];
                    i++;
                } else if(population[helper[i]][obj] > population[helper[j]][obj]) {
                    rank[k][obj] = helper[j];
                    j++;
                } else {
                    check = lexicographicDominate(helper[i],helper[j]);
                    if (check) {
                        rank[k][obj] = helper[i];
                        i++;
                    } else {
                        rank[k][obj] = helper[j];
                        j++;
                    }
                }
                k++;
            }
            while (i <= middle) {
                rank[k][obj] = helper[i];
                k++;
                i++;
            }
            while (j <= high) {
                rank[k][obj] = helper[j];
                k++;
                j++;
            }

        }
        void sortSpecific(int obj) {
            this.obj = obj;
            n = population.length;
            mergeSortSpecific(0, n - 1);
        }

        void mergeSortSpecific(int low, int high) {
            if (low < high) {
                int middle = low + (high - low) / 2;
                mergeSortSpecific(low, middle);
                mergeSortSpecific(middle + 1, high);
                mergeSpecific(low, middle, high);
            }

        }

        void mergeSpecific(int low, int middle, int high) {
            for (int i = low; i <= high; i++) {
                helper[i] = rank[i][obj];
            }

            int i = low;
            int j = middle + 1;
            int k = low;

            while (i <= middle && j <= high) {
                if (population[helper[i]][obj] < population[helper[j]][obj]) {
                    rank[k][obj] = helper[i];
                    i++;
                } else if(population[helper[i]][obj] > population[helper[j]][obj]) {
                    rank[k][obj] = helper[j];
                    j++;
                } else {
                    if(lexOrder[helper[i]] < lexOrder[helper[j]]) {
                        rank[k][obj] = helper[i];
                        i++;
                    } else {
                        rank[k][obj] = helper[j];
                        j++;
                    }
                }
                k++;
            }
            while(i <= middle) {
                rank[k][obj] = helper[i];
                k++;
                i++;
            }
            while(j <= high) {
                rank[k][obj] = helper[j];
                k++;
                j++;
            }

        }

        boolean lexicographicDominate(int p1, int p2) {
            for(int i = 0; i < population[0].length; i++) {
                if (population[p1][i] != population[p2][i]) {
                    return population[p1][i] < population[p2][i];
                }
            }
            return true;
        }
    }
}
