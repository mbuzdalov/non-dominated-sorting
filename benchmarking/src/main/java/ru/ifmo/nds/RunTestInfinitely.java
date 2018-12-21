package ru.ifmo.nds;

import java.io.IOException;
import java.util.Random;

public class RunTestInfinitely {
    private static void run(NonDominatedSorting sorting, double[][][] tests, int[] ranks) throws IOException {
        while (System.in.available() == 0) {
            long t0 = System.nanoTime();
            int sum = 0;
            for (double[][] test : tests) {
                sorting.sort(test, ranks);
                for (int i : ranks) sum += i;
            }
            long time = System.nanoTime() - t0;
            System.out.println("Time " + (time / 1e9) + " s, checksum " + sum);
        }
    }

    public static void main(String[] args) throws IOException {
        NonDominatedSortingFactory factory = IdCollection.getNonDominatedSortingFactory(args[0]);
        int n = Integer.parseInt(args[1]);
        int d = Integer.parseInt(args[2]);
        int count = Integer.parseInt(args[3]);

        double[][][] tests = new double[count][n][d];
        int[] ranks = new int[n];
        Random random = new Random(8234925);

        for (int i = 0; i < count; ++i) {
            for (int j = 0; j < n; ++j) {
                for (int k = 0; k < d; ++k) {
                    tests[i][j][k] = random.nextDouble();
                }
            }
        }

        NonDominatedSorting sorting = factory.getInstance(n, d);
        run(sorting, tests, ranks);
    }
}
