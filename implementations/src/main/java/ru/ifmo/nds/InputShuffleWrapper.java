package ru.ifmo.nds;

import java.util.concurrent.ThreadLocalRandom;

public final class InputShuffleWrapper extends NonDominatedSorting {
    private NonDominatedSorting sorter;

    public InputShuffleWrapper(NonDominatedSorting sorter) {
        super(sorter.getMaximumPoints(), sorter.getMaximumDimension());
        this.sorter = sorter;
    }

    @Override
    public String getName() {
        return sorter.getName() + " with input shuffling";
    }

    @Override
    protected void closeImpl() {
        sorter.close();
        sorter = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        if (points.length <= 16) {
            sorter.sortChecked(points, ranks, maximalMeaningfulRank);
        } else {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int[] indices = this.indices;
            int n = points.length;
            for (int i = 1; i < n; ++i) {
                int j = random.nextInt(i + 1);
                if (i != j) {
                    double[] tmpPoint = points[i];
                    points[i] = points[j];
                    points[j] = tmpPoint;
                }
                indices[i] = j;
            }
            sorter.sortChecked(points, ranks, maximalMeaningfulRank);
            for (int i = n - 1; i > 0; --i) {
                int j = indices[i];
                if (i != j) {
                    double[] tmpPoint = points[i];
                    points[i] = points[j];
                    points[j] = tmpPoint;
                    int tmpRank = ranks[i];
                    ranks[i] = ranks[j];
                    ranks[j] = tmpRank;
                }
            }
        }
    }
}
