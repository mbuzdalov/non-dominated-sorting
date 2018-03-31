package ru.ifmo.nds;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class TortureTesting {
    private static ThreadLocalRandom random = ThreadLocalRandom.current();
    private static double[][] generateCloud(int points, int dimension) {
        double[][] rv = new double[points][dimension];
        if (random.nextBoolean()) {
            for (int i = 0; i < points; ++i) {
                for (int j = 0; j < dimension; ++j) {
                    rv[i][j] = random.nextDouble();
                }
            }
        } else {
            for (int i = 0; i < points; ++i) {
                for (int j = 0; j < dimension; ++j) {
                    rv[i][j] = random.nextInt(20);
                }
            }
        }
        return rv;
    }

    private static void printTest(int points, int dimension, double[][] instance, int[] reference) {
        System.out.println("        groupCheck(new double[][] {");
        for (int i = 0; i < points; ++i) {
            System.out.print("                {");
            for (int j = 0; j < dimension; ++j) {
                System.out.print(instance[i][j]);
                if (j + 1 != dimension) {
                    System.out.print(", ");
                } else {
                    System.out.println("},");
                }
            }
        }
        System.out.println("        }, new int[] {");
        System.out.print("                ");
        for (int i = 0; i < points; ++i) {
            System.out.print(reference[i]);
            if (i + 1 != points) {
                System.out.print(",");
            } else {
                System.out.println();
            }
        }
        System.out.println("        });");

    }

    public static void main(String[] args) throws IOException {
        int maxPoints = 10000;
        int maxDimension = 20;
        List<NonDominatedSortingFactory> sortingFactories = Arrays.asList(
                FastNonDominatedSorting.getOriginalVersion(),
                CornerSort.getInstance(),
                DeductiveSort.getInstance(),
                DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.DELAYED_INSERTION_SEQUENTIAL_CONCATENATION),
                DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION),
                DominanceTree.getNoPresortInsertion(false),
                DominanceTree.getNoPresortInsertion(true),
                ENS.getENS_BS(),
                ENS.getENS_SS(),
                FastNonDominatedSorting.getLinearMemoryImplementation(),
                JensenFortinBuzdalov.getFenwickSweepImplementation(1),
                JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(1),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridFNDSImplementation(1),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridENSImplementation(1),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridNDTImplementation(8),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridNDTImplementation(1),
                BestOrderSort.getProteekImplementation(),
                BestOrderSort.getImprovedImplementation(),
                ENS.getENS_NDT(8),
                ENS.getENS_NDT_Arrays(),
                ENS.getENS_NDT_OneTree(8),
                ENS.getENS_NDT_OneTree(1)
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
                System.gc();
                System.gc();
                long t0 = System.currentTimeMillis();
                try {
                    sorting.sort(instance, ranks, maxRank);
                    long time = System.currentTimeMillis() - t0;
                    System.out.printf("%95s: %d ms%n", sorting.getName(), time);
                    if (reference == null) {
                        reference = ranks.clone();
                    } else {
                        for (int i = 0; i < points; ++i) {
                            if (reference[i] != ranks[i]) {
                                printTest(points, dimension, instance, reference);
                                throw new AssertionError("Ranks do not match: index " + i
                                        + " expected " + reference[i]
                                        + " found " + ranks[i]);
                            }
                        }
                    }
                } catch (Throwable th) {
                    printTest(points, dimension, instance, reference);
                    System.exit(1);
                }
            }
            System.out.println();
        }
    }
}
