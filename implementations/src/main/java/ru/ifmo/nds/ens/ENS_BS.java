package ru.ifmo.nds.ens;

import ru.ifmo.nds.util.ArrayHelper;

public class ENS_BS extends ENSBase {
    public ENS_BS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    void sortCheckedImpl(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int n = ranks.length;
        int i0 = indices[0];
        double[] prev = points[i0];
        setRank(i0, ranks, 0, -1, maximalMeaningfulRank);
        int prevRank = 0;
        int maxRank = 0;
        final int len = prev.length;
        for (int i = 1; i < n; ++i) {
            int index = indices[i];
            double[] curr = points[index];
            int currRank;
            if (ArrayHelper.equal(prev, curr, len)) {
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
