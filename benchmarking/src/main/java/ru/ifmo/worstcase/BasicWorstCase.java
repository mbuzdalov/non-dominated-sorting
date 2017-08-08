package ru.ifmo.worstcase;

import ru.ifmo.NonDominatedSorting;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class BasicWorstCase {
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

        void measure(NonDominatedSorting sorting, int nameLength) {
            String fmt = "    %" + nameLength + "s";
            System.out.printf(fmt, sorting.getName());
            for (int t = 0; t < 5; ++t) {
                long t0 = System.currentTimeMillis();
                sortMe(sorting);
                System.out.print(" " + (System.currentTimeMillis() - t0));
            }
            System.out.println();
        }
    }

    static Instance generateCloud(int points, int objectives) {
        Random random = ThreadLocalRandom.current();
        double[][] rv = new double[points][objectives];
        for (int i = 0; i < points; ++i) {
            for (int j = 0; j < objectives; ++j) {
                rv[i][j] = random.nextDouble();
            }
        }
        return new Instance(rv, null);
    }
}
