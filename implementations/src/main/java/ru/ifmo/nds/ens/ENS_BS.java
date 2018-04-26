package ru.ifmo.nds.ens;

import ru.ifmo.nds.util.ArrayHelper;

public class ENS_BS extends ENSBase {
    public ENS_BS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
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
                prevRank = currRank = findRankByBinarySearch(points, curr, -1, maxRank);
                prev = curr;
            }
            maxRank = setRank(index, ranks, currRank, maxRank, maximalMeaningfulRank);
        }
    }

    @Override
    public String getName() {
        return "ENS-BS";
    }
}
