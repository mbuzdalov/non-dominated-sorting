package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public class Original extends NonDominatedSorting {
    private boolean[] dominated;
    private boolean[] ranked;

    public Original(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        this.dominated = new boolean[maximumPoints];
        this.ranked = new boolean[maximumPoints];
    }

    @Override
    public String getName() {
        return "Deductive Sort, original version";
    }

    @Override
    protected void closeImpl() {
        dominated = null;
        ranked = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int n = points.length;
        final int dim = points[0].length;

        Arrays.fill(ranks, maximalMeaningfulRank + 1);
        Arrays.fill(ranked, 0, n, false);

        int currRank = 0, nRanked = 0;
        while (nRanked < n && currRank <= maximalMeaningfulRank) {
            Arrays.fill(dominated, 0, n, false);
            for (int i = 0; i < n; ++i) {
                if (ranked[i] || dominated[i]) {
                    continue;
                }
                final double[] currP = points[i];
                for (int j = i + 1; j < n; ++j) {
                    if (ranked[j] || dominated[j]) {
                        continue;
                    }
                    int comparison = DominanceHelper.dominanceComparison(currP, points[j], dim);
                    if (comparison < 0) {
                        dominated[j] = true;
                    } else if (comparison > 0) {
                        dominated[i] = true;
                        break;
                    }
                }
                if (!dominated[i]) {
                    ranks[i] = currRank;
                    ranked[i] = true;
                    ++nRanked;
                }
            }
            ++currRank;
        }
    }
}
