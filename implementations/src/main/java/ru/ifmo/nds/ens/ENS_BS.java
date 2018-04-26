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
            int rank = findRankByBinarySearch(points, points[index], -1, maxRank);
            maxRank = setRank(index, ranks, rank, maxRank, maximalMeaningfulRank);
        }
    }

    @Override
    public String getName() {
        return "ENS-BS";
    }
}
