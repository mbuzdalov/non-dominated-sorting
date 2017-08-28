package ru.ifmo.nds.fnds;

import static ru.ifmo.nds.util.DominanceHelper.*;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;

public class LinearMemory extends NonDominatedSorting {
    private int[] howManyDominateMe;
    private int[] candidates;

    public LinearMemory(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);

        howManyDominateMe = new int[maximumPoints];
        candidates = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "Fast Non-Dominated Sorting (with linear memory)";
    }

    @Override
    protected void closeImpl() throws Exception {
        howManyDominateMe = null;
        candidates = null;
    }

    private void comparePointWithOthers(int index, double[][] points, int from, int until) {
        double[] pi = points[index];
        for (int j = from; j < until; ++j) {
            int comp = dominanceComparison(pi, points[j], HAS_LESS_MASK | HAS_GREATER_MASK);
            switch (comp) {
                case HAS_LESS_MASK:
                    ++howManyDominateMe[j];
                    break;
                case HAS_GREATER_MASK:
                    ++howManyDominateMe[index];
                    break;
            }
        }
    }

    private void compareAllPoints(double[][] points, int count) {
        for (int i = 0; i < count; ++i) {
            comparePointWithOthers(i, points, i + 1, count);
        }
    }

    private int moveNonDominatedForward(int count) {
        int current = 0;
        for (int i = 0; i < count; ++i) {
            if (howManyDominateMe[i] == 0) {
                ArrayHelper.swap(candidates, i, current);
                ++current;
            }
        }
        return current;
    }

    private void assignRankToRange(int[] ranks, int rank, int from, int until) {
        for (int i = from; i < until; ++i) {
            ranks[candidates[i]] = rank;
        }
    }

    private int punchNextPointsBySinglePoint(int index, double[][] points, int nextRankRight, int n) {
        double[] cp = points[candidates[index]];
        for (int nextIndex = nextRankRight; nextIndex < n; ++nextIndex) {
            int nextPoint = candidates[nextIndex];
            double[] np = points[nextPoint];

            if (strictlyDominates(cp, np)) {
                if (--howManyDominateMe[nextPoint] == 0) {
                    ArrayHelper.swap(candidates, nextIndex, nextRankRight);
                    ++nextRankRight;
                }
            }
        }
        return nextRankRight;
    }

    private int punchNextPoints(double[][] points, int currentRankLeft, int currentRankRight, int n) {
        int nextRankRight = currentRankRight;
        for (int currIndex = currentRankLeft; currIndex < currentRankRight && nextRankRight < n; ++currIndex) {
            nextRankRight = punchNextPointsBySinglePoint(currIndex, points, nextRankRight, n);
        }
        return nextRankRight;
    }

    private void assignRanks(double[][] points, int[] ranks, int numberOfRankZeroPoints, int n, int maximalMeaningfulRank) {
        int currentRankLeft = 0;
        int currentRankRight = numberOfRankZeroPoints;

        int currentRank = 0;
        while (currentRankLeft < n) {
            assignRankToRange(ranks, currentRank, currentRankLeft, currentRankRight);
            if (currentRank == maximalMeaningfulRank) {
                assignRankToRange(ranks, currentRank + 1, currentRankRight, n);
                return;
            }
            int nextRankRight = punchNextPoints(points, currentRankLeft, currentRankRight, n);
            currentRankLeft = currentRankRight;
            currentRankRight = nextRankRight;
            ++currentRank;
        }
    }

    private void cleanup(int n) {
        for (int i = 0; i < n; ++i) {
            howManyDominateMe[i] = 0;
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        compareAllPoints(points, n);
        ArrayHelper.fillIdentity(candidates, n);
        int rankZeroPoints = moveNonDominatedForward(n);
        assignRanks(points, ranks, rankZeroPoints, n, maximalMeaningfulRank);
        cleanup(n);
    }
}
