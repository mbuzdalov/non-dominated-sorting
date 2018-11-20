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
        int i0 = indices[0];
        double[] prev = points[i0];
        setRank(i0, ranks, 0, -1, maximalMeaningfulRank);
        int maxRank = 0;
        int prevRank = 0;
        for (int i = 1; i < n; ++i) {
            int index = indices[i];
            double[] curr = points[index];
            int currRank;
            if (ArrayHelper.equal(prev, curr)) {
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
