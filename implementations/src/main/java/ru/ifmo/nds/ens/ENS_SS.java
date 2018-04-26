package ru.ifmo.nds.ens;

import ru.ifmo.nds.util.ArrayHelper;

public class ENS_SS extends ENSBase {
    public ENS_SS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    private int findRank(double[][] points, double[] point, int maxRank) {
        int currRank = 0;
        while (currRank <= maxRank) {
            if (frontDominates(currRank, points, point)) {
                ++currRank;
            } else {
                break;
            }
        }
        return currRank;
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
            }
            maxRank = setRank(index, ranks, currRank, maxRank, maximalMeaningfulRank);
        }
    }

    @Override
    public String getName() {
        return "ENS-SS";
    }
}
