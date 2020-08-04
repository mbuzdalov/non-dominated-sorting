package ru.ifmo.nds;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class TortureTesting {
    private static final ThreadLocalRandom random = ThreadLocalRandom.current();
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

    private static void printPoint(StringBuilder sb, double[] point, int dimension) {
        sb.append(" {");
        for (int j = 0; j < dimension; ++j) {
            double v = point[j];
            int vi = (int) v;
            if (v == vi) {
                sb.append(vi);
            } else {
                sb.append(v);
            }
            if (j + 1 != dimension) {
                sb.append(", ");
            } else {
                sb.append("},");
            }
        }
    }

    private static void printInt(StringBuilder sb, int value, boolean notLast) {
        sb.append(" ").append(value);
        if (notLast) {
            sb.append(",");
        } else {
            sb.append("\n");
        }
    }

    private static final int WIDTH_IN_COLUMNS = 120;
    private static final String ARRAY_PREFIX = "               ";

    private static void printTest(int points, int dimension, double[][] instance, int[] reference) {
        System.out.println("        groupCheck(new double[][] {");
        StringBuilder sb = new StringBuilder();
        sb.append(ARRAY_PREFIX);
        for (int i = 0; i < points; ++i) {
            int length = sb.length();
            printPoint(sb, instance[i], dimension);
            if (sb.length() > WIDTH_IN_COLUMNS) {
                sb.setLength(length);
                System.out.println(sb);
                sb.setLength(ARRAY_PREFIX.length());
                printPoint(sb, instance[i], dimension);
            }
        }
        System.out.println(sb);
        System.out.println("        }, new int[] {");

        sb.setLength(ARRAY_PREFIX.length());
        for (int i = 0; i < points; ++i) {
            int length = sb.length();
            printInt(sb, reference[i], i + 1 != points);
            if (sb.length() > WIDTH_IN_COLUMNS) {
                sb.setLength(length);
                System.out.println(sb);
                sb.setLength(ARRAY_PREFIX.length());
                printInt(sb, reference[i], i + 1 != points);
            }
        }
        System.out.println(sb);
        System.out.println("        });");
    }

    public static void main(String[] args) throws IOException {
        int maxPoints = 10000;
        int maxDimension = 20;
        List<NonDominatedSortingFactory> sortingFactories = Arrays.asList(
                FastNonDominatedSorting.getOriginalVersion(),
                CornerSort.getInstance(),
                DeductiveSort.getOriginalImplementation(),
                DeductiveSort.getReorderingImplementationV1(),
                DeductiveSort.getReorderingImplementationV2(),
                DeductiveSort.getRandomizedQuadraticImplementationV1(),
                DeductiveSort.getRandomizedQuadraticImplementationV2(),
                DominanceTree.getPresortInsertion(false, false),
                DominanceTree.getPresortInsertion(true, true),
                DominanceTree.getNoPresortInsertion(false),
                DominanceTree.getNoPresortInsertion(true),
                ENS.getENS_BS(),
                ENS.getENS_SS(),
                FastNonDominatedSorting.getLinearMemoryImplementation(),
                JensenFortinBuzdalov.getFenwickSweepImplementation(1),
                JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(1),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridFNDSImplementation(1),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridENSImplementation(1),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridNDTImplementation(8, 1),
                JensenFortinBuzdalov.getRedBlackTreeSweepHybridNDTImplementation(1, 1),
                BestOrderSort.getProteekImplementation(),
                BestOrderSort.getImprovedImplementation(),
                ENS.getENS_NDT(8),
                ENS.getENS_NDT_Arrays(),
                ENS.getENS_NDT_OneTree(8),
                ENS.getENS_NDT_OneTree(1),
                SumitMishraDivideConquer.getDCNS_BS(),
                SumitMishraDivideConquer.getDCNS_SS(),
                FilterSort.getInstance(),
                SetIntersectionSort.getBitSetInstance()
        );
        Map<String, Integer> winnerCounts = new HashMap<>();
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
            double[][] instanceCopy = instance.clone();
            for (int i = 0; i < points; ++i) {
                instanceCopy[i] = instanceCopy[i].clone();
            }
            int[] reference = null;
            int[] ranks = new int[points];

            NonDominatedSorting winner = null;
            double winnerTime = Double.POSITIVE_INFINITY;

            for (NonDominatedSorting sorting : sortings) {
                System.gc();
                System.gc();
                long t0 = System.nanoTime();
                try {
                    sorting.sort(instance, ranks, maxRank);
                    if (!Arrays.deepEquals(instanceCopy, instance)) {
                        throw new AssertionError("Test has been altered!");
                    }
                    double time = (System.nanoTime() - t0) * 1e-9;
                    System.out.printf(Locale.US, "%102s: %f s%n", sorting.getName(), time);
                    if (time < winnerTime) {
                        winner = sorting;
                        winnerTime = time;
                    }
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
                } catch (Throwable th) {
                    th.printStackTrace();
                    printTest(points, dimension, instance, reference);
                    System.exit(1);
                }
            }
            System.out.println();
            if (winner != null) {
                System.out.println("Winner: " + winner.getName());
                winnerCounts.compute(winner.getName(), (n, old) -> old == null ? 1 : old + 1);
            }
        }
        dumpWinners(winnerCounts);
    }

    private static void dumpWinners(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getValue));
        System.out.println("Winners:");
        for (int i = entries.size() - 1; i >= 0; --i) {
            System.out.println("    " + entries.get(i).getValue() + " times: " + entries.get(i).getKey());
        }
    }
}
