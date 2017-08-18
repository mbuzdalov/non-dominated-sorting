package ru.ifmo.nds.rundb;

import ru.ifmo.nds.NonDominatedSorting;

import java.util.Arrays;
import java.util.Objects;

public final class Dataset {
    private final String id;
    private final double[][][] points;
    private final int[] actualRanks;

    public Dataset(String id, double[][][] points) {
        this.id = Objects.requireNonNull(id);
        this.points = points.clone();
        for (int i = 0; i < this.points.length; ++i) {
            this.points[i] = this.points[i].clone();
            for (int j = 0; j < this.points[i].length; ++j) {
                this.points[i][j] = this.points[i][j].clone();
            }
        }
        this.actualRanks = new int[points.length];
    }

    public String getId() {
        return id;
    }

    public int getNumberOfInstances() {
        return points.length;
    }

    public int getNumberOfPoints() {
        return points[0].length;
    }

    public int getDimension() {
        return points[0][0].length;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass() == this.getClass()) {
            Dataset that = (Dataset) obj;
            return id.equals(that.id) && Arrays.deepEquals(points, that.points);
        } else {
            return false;
        }
    }

    public int runAlgorithm(NonDominatedSorting sorting, int maximalMeaningfulRank) {
        int sumMaximumRanks = 0;
        for (double[][] points : this.points) {
            Arrays.fill(actualRanks, 239);
            sorting.sort(points, actualRanks, maximalMeaningfulRank);
            int maximumRank = -1;
            for (int r : actualRanks) {
                maximumRank = Math.max(maximumRank, r);
            }
            sumMaximumRanks += maximumRank;
        }
        return sumMaximumRanks;
    }
}
