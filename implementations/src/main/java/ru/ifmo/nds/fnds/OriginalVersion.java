package ru.ifmo.nds.fnds;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;
import static ru.ifmo.nds.util.DominanceHelper.*;

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
    protected void closeImpl() {
        queue = null;
        howManyIDominate = null;
        howManyDominateMe = null;
        whoIDominate = null;
    }

    private void pushToDominateList(int good, int bad) {
        ++howManyDominateMe[bad];
        int howMany = howManyIDominate[good];
        whoIDominate[good][howMany] = bad;
        howManyIDominate[good] = ++howMany;
    }

    private void comparePointWithOthers(int index, double[][] points, int from, int until) {
        double[] pi = points[index];
        int dim = pi.length;
        for (int j = from; j < until; ++j) {
            int comp = dominanceComparison(pi, points[j], dim);
            switch (comp) {
                case -1:
                    pushToDominateList(index, j);
                    break;
                case +1:
                    pushToDominateList(j, index);
                    break;
            }
        }
    }

    private void comparePoints(double[][] points, int n) {
        for (int i = 0; i < n; ++i) {
            comparePointWithOthers(i, points, i + 1, n);
        }
    }

    private int enqueueZeroRanks(int n, int[] ranks) {
        int qHead = 0;
        for (int i = 0; i < n; ++i) {
            if (howManyDominateMe[i] == 0) {
                ranks[i] = 0;
                queue[qHead] = i;
                ++qHead;
            }
        }
        return qHead;
    }

    private int decreaseWhomIDominate(int index, int[] ranks, int qHead, int maximalMeaningfulRank) {
        int[] iDominate = whoIDominate[index];
        int nextRank = ranks[index] + 1;
        for (int pos = howManyIDominate[index] - 1; pos >= 0; --pos) {
            int next = iDominate[pos];
            if (--howManyDominateMe[next] == 0) {
                ranks[next] = nextRank;
                if (nextRank < maximalMeaningfulRank) {
                    queue[qHead] = next;
                    ++qHead;
                }
            }
        }
        return qHead;
    }

    private void markNotRankedAsMeaningless(int n, int[] ranks, int maximalMeaningfulRank) {
        for (int i = 0; i < n; ++i) {
            if (ranks[i] == -1) {
                ranks[i] = maximalMeaningfulRank + 1;
            }
        }
    }

    private void assignRanks(int[] ranks, int n, int maximalMeaningfulRank) {
        int qHead = enqueueZeroRanks(n, ranks);
        int qTail = 0;
        while (qHead > qTail) {
            int curr = queue[qTail];
            ++qTail;
            qHead = decreaseWhomIDominate(curr, ranks, qHead, maximalMeaningfulRank);
        }
        markNotRankedAsMeaningless(n, ranks, maximalMeaningfulRank);
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        Arrays.fill(howManyDominateMe, 0);
        Arrays.fill(howManyIDominate, 0);
        comparePoints(points, n);
        Arrays.fill(ranks, -1);
        assignRanks(ranks, n, maximalMeaningfulRank);
    }
}
