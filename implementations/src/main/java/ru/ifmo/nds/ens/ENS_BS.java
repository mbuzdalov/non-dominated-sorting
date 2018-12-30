package ru.ifmo.nds.ens;

public final class ENS_BS extends ENSBase {
    public ENS_BS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    final int findRank(double[][] points, double[] point, int maxRank) {
        int leftRank = -1, rightRank = maxRank + 1;
        while (rightRank - leftRank > 1) {
            int currRank = (leftRank + rightRank) >>> 1;
            if (frontDominates(currRank, points, point)) {
                leftRank = currRank;
            } else {
                rightRank = currRank;
            }
        }
        return rightRank;
    }

    @Override
    public String getName() {
        return "ENS-BS";
    }
}
