package ru.ifmo.nds.rundb;

import ru.ifmo.nds.NonDominatedSorting;

import java.util.*;
import java.util.stream.Collectors;

public final class Dataset {
    private final String id;
    private final double[][][] points;
    private final int[][] actualRanks;
    private final int maxPoints, maxDimension;

    public Dataset(String id, double[][][] points) {
        this.id = Objects.requireNonNull(id);
        this.points = points.clone();
        this.actualRanks = new int[this.points.length][];
        int maxPoints = 0, maxDimension = 0;
        for (int i = 0; i < this.points.length; ++i) {
            this.points[i] = this.points[i].clone();
            maxDimension = Math.max(maxDimension, this.points[i][0].length);
            maxPoints = Math.max(maxPoints, this.points[i].length);
            for (int j = 0; j < this.points[i].length; ++j) {
                this.points[i][j] = this.points[i][j].clone();
            }
            this.actualRanks[i] = new int[this.points[i].length];
        }
        this.maxDimension = maxDimension;
        this.maxPoints = maxPoints;
    }

    public String getId() {
        return id;
    }

    public int getNumberOfInstances() {
        return points.length;
    }

    public int getMaxNumberOfPoints() {
        return maxPoints;
    }

    public int getMaxDimension() {
        return maxDimension;
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
        for (int i = 0; i < this.points.length; ++i) {
            double[][] points = this.points[i];
            int[] ranks = actualRanks[i];
            Arrays.fill(ranks, 239);
            sorting.sort(points, ranks, maximalMeaningfulRank);
            int maximumRank = -1;
            for (int r : ranks) {
                maximumRank = Math.max(maximumRank, r);
            }
            sumMaximumRanks += maximumRank;
        }
        return sumMaximumRanks;
    }

    public static List<Dataset> concatenateAndSplitIntoWarmupAndControl(String newIdPrefix, List<Dataset> datasets) {
        List<double[][]> inputs = datasets.stream().flatMap(d -> Arrays.stream(d.points)).collect(Collectors.toList());
        Collections.shuffle(inputs);
        List<Dataset> rv = new ArrayList<>(2);
        int warmUpSize = (inputs.size() * 9) / 10;
        int controlSize = inputs.size() - warmUpSize;
        int start = 0;
        while (start < inputs.size()) {
            int localSize = start == 0 ? warmUpSize : controlSize;
            double[][][] allPoints = new double[localSize][][];
            for (int i = 0; i < allPoints.length; ++i) {
                allPoints[i] = inputs.get(start + i);
            }
            Arrays.sort(allPoints, Comparator.comparingInt(a -> a.length));
            rv.add(new Dataset(newIdPrefix + "." + rv.size(), allPoints));
            start += localSize;
        }
        return rv;
    }
}
