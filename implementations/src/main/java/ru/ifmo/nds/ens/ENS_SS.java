package ru.ifmo.nds.ens;

public final class ENS_SS extends ENSBase {
    public ENS_SS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    int findRank(double[][] points, double[] point, int maxRank) {
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
    public String getName() {
        return "ENS-SS";
    }
}
