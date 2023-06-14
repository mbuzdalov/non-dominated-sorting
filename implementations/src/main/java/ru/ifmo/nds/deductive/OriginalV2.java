package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class OriginalV2 extends NonDominatedSorting {
    public OriginalV2(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Deductive Sort, original version 2";
    }

    @Override
    protected void closeImpl() {}

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int n = points.length;
        final int dim = points[0].length;

        Arrays.fill(ranks, 0);

        int currRank = 0, nRanked = 0;
        while (nRanked < n && currRank <= maximalMeaningfulRank) {
            for (int i = 0; i < n; ++i) {
                if (ranks[i] == currRank) {
                    final double[] currP = points[i];
                    for (int j = i; ++j < n; ) {
                        if (ranks[j] == currRank) {
                            int comparison = DominanceHelper.dominanceComparison(currP, points[j], dim);
                            if (comparison <= 0) {
                                ranks[j] = currRank - comparison;
                            } else {
                                ranks[i] = currRank + 1;
                                --nRanked;
                                break;
                            }
                        }
                    }
                    ++nRanked;
                }
            }
            ++currRank;
        }
    }
}
