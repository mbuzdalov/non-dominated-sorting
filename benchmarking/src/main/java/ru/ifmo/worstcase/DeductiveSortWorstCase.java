package ru.ifmo.worstcase;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import ru.ifmo.*;
import static ru.ifmo.worstcase.BasicWorstCase.Instance;

public class DeductiveSortWorstCase {
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

        int nameLength = sortings.stream().mapToInt(s -> s.getName().length()).max().orElse(-1);

        System.out.println("Warming up... ");
        for (NonDominatedSorting sorting : sortings) {
            System.out.println("    " + sorting.getName());
            Instance instance = BasicWorstCase.generateCloud(maxPoints / 5, maxObjectives);
            for (int i = 0; i < 100; ++i) {
                instance.sortMe(sorting);
            }
        }

        System.out.println("CLOUD: " + maxPoints + " points, " + maxObjectives + " objectives");
        Instance cloudInstance = BasicWorstCase.generateCloud(maxPoints, maxObjectives);
        for (NonDominatedSorting sorting : sortings) {
            cloudInstance.measure(sorting, nameLength);
        }

        for (int fronts = 1; fronts <= maxPoints; ++fronts) {
            if (maxPoints % fronts != 0) {
                continue;
            }
            System.out.println("NFRONTS: " + maxPoints + " points, " + maxObjectives + " objectives, " + fronts + " fronts");
            Instance instance = generateNFronts(maxPoints, maxObjectives, fronts);
            for (NonDominatedSorting sorting : sortings) {
                instance.measure(sorting, nameLength);
            }
        }
    }
}
