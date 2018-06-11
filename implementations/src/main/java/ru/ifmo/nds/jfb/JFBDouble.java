package ru.ifmo.nds.jfb;

import ru.ifmo.nds.util.RankQueryStructureDouble;

public class JFBDouble extends JFBBase {
    private RankQueryStructureDouble rankQuery;

    public JFBDouble(RankQueryStructureDouble rankQueryStructure,
                     int maximumDimension,
                     int allowedThreads,
                     HybridAlgorithmWrapper hybridWrapper) {
        super(rankQueryStructure.maximumPoints(),
                maximumDimension,
                rankQueryStructure.supportsMultipleThreads() ? allowedThreads : 1,
                hybridWrapper,
                "no ordinate compression, data structure = " + rankQueryStructure.getName());
        this.rankQuery = rankQueryStructure;
    }

    @Override
    protected void closeImpl() {
        super.closeImpl();
        rankQuery = null;
    }

    @Override
    protected int sweepA(int from, int until) {
        double[] local = transposedPoints[1];
        RankQueryStructureDouble.RangeHandle rankQuery = this.rankQuery.createHandle(from, from, until, indices, local);
        int minOverflow = until;
        for (int i = from; i < until; ++i) {
            int curr = indices[i];
            double currY = local[curr];
            int result = Math.max(ranks[curr], rankQuery.getMaximumWithKeyAtMost(currY, ranks[curr]) + 1);
            ranks[curr] = result;
            if (result <= maximalMeaningfulRank) {
                rankQuery = rankQuery.put(currY, result);
            } else if (minOverflow > i) {
                minOverflow = i;
            }
        }
        return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, until);
    }

    @Override
    protected int sweepB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int tempFrom) {
        double[] local = transposedPoints[1];
        RankQueryStructureDouble.RangeHandle rankQuery = this.rankQuery.createHandle(tempFrom, goodFrom, goodUntil, indices, local);
        int goodI = goodFrom;
        int minOverflow = weakUntil;
        for (int weakI = weakFrom; weakI < weakUntil; ++weakI) {
            int weakCurr = indices[weakI];
            while (goodI < goodUntil && indices[goodI] < weakCurr) {
                int goodCurr = indices[goodI];
                rankQuery = rankQuery.put(local[goodCurr], ranks[goodCurr]);
                ++goodI;
            }
            int result = Math.max(ranks[weakCurr],
                    rankQuery.getMaximumWithKeyAtMost(local[weakCurr], ranks[weakCurr]) + 1);
            ranks[weakCurr] = result;
            if (result > maximalMeaningfulRank && minOverflow > weakI) {
                minOverflow = weakI;
            }
        }
        return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, weakUntil);
    }
}
