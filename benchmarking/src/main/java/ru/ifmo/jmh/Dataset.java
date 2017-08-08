package ru.ifmo.jmh;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import ru.ifmo.NonDominatedSorting;

class Dataset {
    private final double[][] points;
    private final int[] ranks;

    private Dataset(double[][] points, int[] ranks) {
        this.points = points;
        this.ranks = ranks;
    }

    int runSortingOnMe(NonDominatedSorting sorting) {
        Arrays.fill(ranks, 0);
        sorting.sort(points, ranks);
        int sum = 0;
        for (int r : ranks) {
            sum += r;
        }
        return sum;
    }

    static Dataset generate(String benchmarkName, Map<String, Integer> params) {
        switch (benchmarkName) {
            case "uniformHypercube":
                return generateUniformHypercube(params.get("N"), params.get("dimension"), params.get("seed"));
            case "uniformHyperplane":
                return generateUniformHyperplane(params.get("N"), params.get("dimension"), params.get("seed"));
            default: throw new AssertionError("Unknown generator name '" + benchmarkName + "'");
        }
    }

    private static Dataset generateUniformHyperplane(int numPoints, int dimension, int seed) {
        Random random = new Random(seed);
        double[][] points = new double[numPoints][dimension];
        for (double[] point : points) {
            for (int i = 1; i < dimension; ++i) {
                point[i] = random.nextDouble();
                point[0] -= point[i];
            }
            point[0] += dimension * 0.5;
        }
        return new Dataset(points, new int[numPoints]);
    }

    private static Dataset generateUniformHypercube(int numPoints, int dimension, int seed) {
        Random random = new Random(seed);
        double[][] points = new double[numPoints][dimension];
        for (double[] point : points) {
            for (int i = 0; i < dimension; ++i) {
                point[i] = random.nextDouble();
            }
        }
        return new Dataset(points, new int[numPoints]);
    }
}
