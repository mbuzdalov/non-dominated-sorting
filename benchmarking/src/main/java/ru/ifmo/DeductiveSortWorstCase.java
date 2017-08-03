package ru.ifmo;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class DeductiveSortWorstCase {
    static class Instance {
        final double[][] points;
        final int[] ranks;
        final int[] place;

        Instance(double[][] points, int[] ranks) {
            this.points = points;
            this.ranks = ranks;
            this.place = new int[points.length];
        }

        void sortMe(NonDominatedSorting sorting) {
            Arrays.fill(place, 82463);
            sorting.sort(points, place);
            if (ranks != null) {
                for (int i = 0; i < ranks.length; ++i) {
                    if (ranks[i] != place[i]) {
                        throw new AssertionError(sorting.getName() + " failed: at index "
                                + i + " expected " + ranks[i] + " found " + place[i]);
                    }
                }
            }
        }

        void measure(NonDominatedSorting sorting) {
            System.out.printf("    %50s", sorting.getName());
            for (int t = 0; t < 5; ++t) {
                long t0 = System.currentTimeMillis();
                sortMe(sorting);
                System.out.print(" " + (System.currentTimeMillis() - t0));
            }
            System.out.println();
        }
    }

    private static Instance generateCloud(int points, int objectives) {
        Random random = ThreadLocalRandom.current();
        double[][] rv = new double[points][objectives];
        for (int i = 0; i < points; ++i) {
            for (int j = 0; j < objectives; ++j) {
                rv[i][j] = random.nextDouble();
            }
        }
        return new Instance(rv, null);
    }

    private static Instance generateNFronts(int points, int objectives, int fronts) {
        Random random = ThreadLocalRandom.current();
        double[][] rv = new double[points][objectives];
        int pointsInLayer = (points + fronts - 1) / fronts;
        for (int i = 0; i < pointsInLayer; ++i) {
            for (int j = 1; j < objectives; ++j) {
                rv[i][j] = random.nextDouble();
                rv[i][0] -= rv[i][j];
            }
            rv[i][0] += 0.5 * objectives;
        }
        int[] ranks = new int[points];
        for (int i = pointsInLayer; i < points; ++i) {
            System.arraycopy(rv[i - pointsInLayer], 0, rv[i], 0, objectives);
            for (int j = 0; j < objectives; ++j) {
                rv[i][j] += 1e-8;
            }
            ranks[i] = ranks[i - pointsInLayer] + 1;
        }
        for (int i = 1; i < points; ++i) {
            int prev = random.nextInt(i + 1);
            if (prev != i) {
                double[] tmpD = rv[i];
                rv[i] = rv[prev];
                rv[prev] = tmpD;
                int tmpI = ranks[i];
                ranks[i] = ranks[prev];
                ranks[prev] = tmpI;
            }
        }
        return new Instance(rv, ranks);
    }

    private static final List<NonDominatedSortingFactory> factories = Arrays.asList(
            JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(),
            FastNonDominatedSorting.getOriginalVersion(),
            DeductiveSort.getInstance()
    );

    public static void main(String[] args) {
        final int maxPoints = 5000;
        final int maxObjectives = 3;

        List<NonDominatedSorting> sortings = factories
                .stream()
                .map(nonDominatedSortingFactory -> nonDominatedSortingFactory.getInstance(maxPoints, maxObjectives))
                .collect(Collectors.toList());

        System.out.println("Warming up... ");
        for (NonDominatedSorting sorting : sortings) {
            System.out.println("    " + sorting.getName());
            Instance instance = generateNFronts(1000, maxObjectives, 3);
            for (int i = 0; i < 100; ++i) {
                instance.sortMe(sorting);
            }
        }

        System.out.println("CLOUD: " + maxPoints + " points, " + maxObjectives + " objectives");
        Instance cloudInstance = generateCloud(maxPoints, maxObjectives);
        for (NonDominatedSorting sorting : sortings) {
            cloudInstance.measure(sorting);
        }

        for (int fronts = 1; fronts <= maxPoints; ++fronts) {
            if (maxPoints % fronts != 0) {
                continue;
            }
            System.out.println("NFRONTS: " + maxPoints + " points, " + maxObjectives + " objectives, " + fronts + " fronts");
            Instance instance = generateNFronts(maxPoints, maxObjectives, fronts);
            for (NonDominatedSorting sorting : sortings) {
                instance.measure(sorting);
            }
        }
    }
}
