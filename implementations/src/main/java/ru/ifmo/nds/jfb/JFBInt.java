package ru.ifmo.nds.jfb;

import ru.ifmo.nds.util.RankQueryStructureInt;

public class JFBInt extends JFBBase {
    private RankQueryStructureInt rankQuery;
    private int[] compressedOrdinates;

    public JFBInt(RankQueryStructureInt rankQueryStructure,
                  int maximumDimension,
                  int allowedThreads,
                  HybridAlgorithmWrapper hybridWrapper) {
        super(rankQueryStructure.maximumPoints(),
                maximumDimension,
                rankQueryStructure.supportsMultipleThreads() ? allowedThreads : 1,
                hybridWrapper,
                "ordinate compression, data structure = " + rankQueryStructure.getName());
        compressedOrdinates = new int[rankQueryStructure.maximumPoints()];
        this.rankQuery = rankQueryStructure;
    }

    @Override
    protected void closeImpl() {
        super.closeImpl();
        rankQuery = null;
        compressedOrdinates = null;
    }

    @Override
    protected void postTransposePointHook(int newN) {
        sorter.compressCoordinates(transposedPoints[1], indices, compressedOrdinates, 0, newN);
    }

    @Override
    protected int sweepA(int from, int until) {
        int[] local = compressedOrdinates;
        RankQueryStructureInt.RangeHandle rankQuery = this.rankQuery.createHandle(from, from, until, indices, local);
        int minOverflow = until;
        for (int i = from; i < until; ++i) {
            int curr = indices[i];
            int currY = local[curr];
            int result = Math.max(ranks[curr], rankQuery.getMaximumWithKeyAtMost(currY, ranks[curr]) + 1);
            ranks[curr] = result;
            if (result <= maximalMeaningfulRank) {
                rankQuery = rankQuery.put(currY, result);
            } else if (minOverflow > i) {
                minOverflow = i;
            }
        }
        return kickOutOverflowedRanks(minOverflow, until);
    }

    @Override
    protected int sweepB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int tempFrom) {
        int[] local = compressedOrdinates;
        RankQueryStructureInt.RangeHandle rankQuery = this.rankQuery.createHandle(tempFrom, goodFrom, goodUntil, indices, local);
        int goodI = goodFrom;
        int minOverflow = weakUntil;
        for (int weakI = weakFrom; weakI < weakUntil; ++weakI) {
            int weakCurr = indices[weakI];
            while (goodI < goodUntil && indices[goodI] < weakCurr) {
                int goodCurr = indices[goodI++];
                rankQuery = rankQuery.put(local[goodCurr], ranks[goodCurr]);
            }
            int result = Math.max(ranks[weakCurr],
                    rankQuery.getMaximumWithKeyAtMost(local[weakCurr], ranks[weakCurr]) + 1);
            ranks[weakCurr] = result;
            if (minOverflow > weakI && result > maximalMeaningfulRank) {
                minOverflow = weakI;
            }
        }
        return kickOutOverflowedRanks(minOverflow, weakUntil);
    }
}
