package ru.ifmo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class TortureTesting {
    private static ThreadLocalRandom random = ThreadLocalRandom.current();
    private static double[][] generateCloud(int points, int dimension) {
        double[][] rv = new double[points][dimension];
        for (int i = 0; i < points; ++i) {
            for (int j = 0; j < dimension; ++j) {
                rv[i][j] = random.nextDouble();
            }
        }
        return rv;
    }

    public static void main(String[] args) throws IOException {
        int maxPoints = 10000;
        int maxDimension = 20;
        List<NonDominatedSortingFactory> sortingFactories = Arrays.asList(
                FastNonDominatedSorting.getOriginalVersion(),
                CornerSort.getInstance(),
                DeductiveSort.getInstance(),
                DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.DELAYED_INSERTION_SEQUENTIAL_CONCATENATION),
                ENS.getENS_BS(),
                ENS.getENS_SS(),
                FastNonDominatedSorting.getLinearMemoryImplementation(),
                JensenFortinBuzdalov.getFenwickSweepImplementation(),
                JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridImplementation(),
                BestOrderSort.getProteekImplementation()
        );
        List<NonDominatedSorting> sortings = sortingFactories
                .stream()
                .map(nonDominatedSortingFactory -> nonDominatedSortingFactory.getInstance(maxPoints, maxDimension))
                .collect(Collectors.toList());
        while (System.in.available() == 0) {
            int points = 1 + random.nextInt(maxPoints);
            int dimension = 2 + random.nextInt(maxDimension - 1);
            int maxRank = random.nextBoolean() ? random.nextInt(points + 1) : (int) (Math.sqrt(random.nextInt(points)));
            System.out.println("Uniform hypercube with " + points
                    + " points, dimension " + dimension
                    + ", max rank " + maxRank);
            System.out.println();
            double[][] instance = generateCloud(points, dimension);
            int[] reference = null;
            int[] ranks = new int[points];
            for (NonDominatedSorting sorting : sortings) {
                long t0 = System.currentTimeMillis();
                sorting.sort(instance, ranks, maxRank);
                long time = System.currentTimeMillis() - t0;
                System.out.printf("%95s: %d ms%n", sorting.getName(), time);
                if (reference == null) {
                    reference = ranks.clone();
                } else {
                    for (int i = 0; i < points; ++i) {
                        if (reference[i] != ranks[i]) {
                            throw new AssertionError("Ranks do not match: index " + i
                                    + " expected " + reference[i]
                                    + " found " + ranks[i]);
                        }
                    }
                }
            }
            System.out.println();
        }
    }
}
