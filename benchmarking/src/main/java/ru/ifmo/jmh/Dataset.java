package ru.ifmo.jmh;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ru.ifmo.NonDominatedSorting;

public class Dataset {
    private final double[][] points;
    private final int[] ranks;

    private Dataset(double[][] points, int[] ranks) {
        this.points = points;
        this.ranks = ranks;
    }

    public int runSortingOnMe(NonDominatedSorting sorting) {
        Arrays.fill(ranks, 0);
        sorting.sort(points, ranks);
        int sum = 0;
        for (int r : ranks) {
            sum += r;
        }
        return sum;
    }

    public static Dataset generateUniformHypercube(int numPoints, int dimension) {
        Random random = ThreadLocalRandom.current();
        double[][] points = new double[numPoints][dimension];
        for (double[] point : points) {
            for (int i = 0; i < dimension; ++i) {
                point[i] = random.nextDouble();
            }
        }
        return new Dataset(points, new int[numPoints]);
    }
}
