package ru.ifmo.nds.ens;

public class ENS_BS extends ENSBase {
    public ENS_BS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    void sortCheckedImpl(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        int maxRank = -1;
        for (int i = 0; i < n; ++i) {
            int index = indices[i];
            double[] point = points[index];
            int leftRank = -1, rightRank = maxRank + 1;
            while (rightRank - leftRank > 1) {
                int currRank = (leftRank + rightRank) >>> 1;
                if (frontDominates(currRank, points, point)) {
                    leftRank = currRank;
                } else {
                    rightRank = currRank;
                }
            }
            maxRank = setRank(index, ranks, rightRank, maxRank);
        }
    }

    @Override
    public String getName() {
        return "ENS-BS";
    }
}
