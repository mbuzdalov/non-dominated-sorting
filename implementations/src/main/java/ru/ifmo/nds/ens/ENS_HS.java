package ru.ifmo.nds.ens;

import java.util.Arrays;

import ru.ifmo.nds.util.ArrayHelper;

public class ENS_HS extends ENSBase {
    private final int[] weights;
    private int weightSum;

    public ENS_HS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        weights = new int[maximumPoints];
    }

    private int findRank(double[][] points, double[] point, int maxRank) {
        if (point.length == 2) {
            return findRankByBinarySearch(points, point, -1, maxRank);
        }
        int totalWork = 0;
        int remainingN = weightSum;
        int remainingRanks = maxRank + 1;
        int logRemainingRanks = 31 - Integer.numberOfLeadingZeros(remainingRanks);
        for (int rank = 0; rank <= maxRank; ++rank) {
            int query = frontDominatesWithWork(rank, points, point);
            if (query < 0) {
                return rank;
            }
            totalWork += query;
            remainingN -= weights[rank];
            --remainingRanks;
            if (remainingRanks < (1 << logRemainingRanks)) {
                --logRemainingRanks;
            }
            if ((long) (totalWork) * remainingRanks > (long) (remainingN) * logRemainingRanks) {
                return findRankByBinarySearch(points, point, rank, maxRank);
            }
        }
        return maxRank + 1;
    }

    @Override
    void sortCheckedImpl(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        int maxRank = -1;
        double[] prev = null;
        int prevRank = -1;
        for (int i = 0; i < n; ++i) {
            int index = indices[i];
            double[] curr = points[indices[i]];
            int currRank;
            if (prev != null && ArrayHelper.equal(prev, curr)) {
                currRank = prevRank;
            } else {
                prevRank = currRank = findRank(points, curr, maxRank);
                prev = curr;
                if (currRank <= maximalMeaningfulRank) {
                    ++weights[currRank];
                    ++weightSum;
                }
            }
            maxRank = setRank(index, ranks, currRank, maxRank, maximalMeaningfulRank);
        }
        weightSum = 0;
        Arrays.fill(weights, 0, maxRank + 1, 0);
    }

    @Override
    public String getName() {
        return "ENS-HS";
    }
}
