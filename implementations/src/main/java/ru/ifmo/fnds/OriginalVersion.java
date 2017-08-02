package ru.ifmo.fnds;

import java.util.Arrays;

import ru.ifmo.NonDominatedSorting;

/**
 * This is the implementation of the fast non-dominated sorting algorithm
 * following the original description, as in the following paper:
 *
 * <pre>
 * {@literal @}article{ nsga-ii,
 *     author      = {Kalyanmoy Deb and Amrit Pratap and Sameer Agarwal and T. Meyarivan},
 *     title       = {A Fast and Elitist Multi-Objective Genetic Algorithm: {NSGA}-{II}},
 *     journal     = {IEEE Transactions on Evolutionary Computation},
 *     year        = {2002},
 *     volume      = {6},
 *     number      = {2},
 *     pages       = {182-197},
 *     publisher   = {IEEE Press},
 *     langid      = {english}
 * }
 * </pre>
 *
 * The running time complexity is O(N^2 M), the memory complexity is O(N^2).
 *
 * @author Kalyanmoy Deb (algorithm)
 * @author Amrit Pratap (algorithm)
 * @author Sameer Agarwal (algorithm)
 * @author T. Meyarivan (algorithm)
 *
 * @author Maxim Buzdalov (implementation)
 */
public class OriginalVersion extends NonDominatedSorting {
    private int[] queue;
    private int[] howManyIDominate;
    private int[] howManyDominateMe;
    private int[][] whoIDominate;

    public OriginalVersion(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);

        queue = new int[maximumPoints];
        howManyIDominate = new int[maximumPoints];
        howManyDominateMe = new int[maximumPoints];
        whoIDominate = new int[maximumPoints][maximumPoints - 1];
    }

    @Override
    public String getName() {
        return "Fast Non-Dominated Sorting (original version)";
    }

    @Override
    protected void closeImpl() throws Exception {
        queue = null;
        howManyIDominate = null;
        howManyDominateMe = null;
        whoIDominate = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        int dim = points[0].length;

        Arrays.fill(howManyDominateMe, 0);
        Arrays.fill(howManyIDominate, 0);

        /*
         * Part 1: Counting who dominates who, making lists.
         */
        for (int i = 0; i < n; ++i) {
            double[] pi = points[i];
            for (int j = i + 1; j < n; ++j) {
                double[] pj = points[j];
                boolean iWeaklyDominatesJ = true;
                boolean jWeaklyDominatesI = true;
                for (int k = 0; k < dim; ++k) {
                    if (pi[k] < pj[k]) {
                        jWeaklyDominatesI = false;
                        if (!iWeaklyDominatesJ) {
                            break;
                        }
                    } else if (pi[k] > pj[k]) {
                        iWeaklyDominatesJ = false;
                        if (!jWeaklyDominatesI) {
                            break;
                        }
                    }
                }
                if (iWeaklyDominatesJ && !jWeaklyDominatesI) {
                    ++howManyDominateMe[j];
                    whoIDominate[i][howManyIDominate[i]++] = j;
                } else if (jWeaklyDominatesI && !iWeaklyDominatesJ) {
                    ++howManyDominateMe[i];
                    whoIDominate[j][howManyIDominate[j]++] = i;
                }
            }
        }

        /*
         * Part 2: Assigning ranks using breadth-first search.
         */
        Arrays.fill(ranks, 0);
        int qHead = 0, qTail = 0;
        for (int i = 0; i < n; ++i) {
            if (howManyDominateMe[i] == 0) {
                queue[qHead++] = i;
            }
        }
        while (qHead > qTail) {
            int curr = queue[qTail++];
            int[] iDominate = whoIDominate[curr];
            int nextRank = ranks[curr] + 1;
            for (int pos = howManyIDominate[curr] - 1; pos >= 0; --pos) {
                int next = iDominate[pos];
                if (--howManyDominateMe[next] == 0) {
                    ranks[next] = nextRank;
                    queue[qHead++] = next;
                }
            }
        }
    }
}
