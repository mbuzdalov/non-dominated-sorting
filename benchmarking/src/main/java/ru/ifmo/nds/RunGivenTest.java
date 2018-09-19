package ru.ifmo.nds;

import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class RunGivenTest {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        try (Scanner in = new Scanner(System.in)) {
            int n = in.nextInt();
            int d = in.nextInt();
            double[][] data = new double[n][d];
            for (int i = 0; i < n; ++i) {
                for (int j = 0; j < d; ++j) {
                    data[i][j] = in.nextDouble();
                }
            }
            int[] ranks = new int[n];
            int[] referenceRanks = new int[n];
            boolean set = false;
            String referenceAlgo = null;
            for (String s : IdCollection.getAllNonDominatedSortingIDs()) {
                Arrays.fill(ranks, -1);
                NonDominatedSorting sorting = IdCollection.getNonDominatedSortingFactory(s).getInstance(n, d);
                sorting.sort(data, ranks);
                if (set) {
                    if (!Arrays.equals(referenceRanks, ranks)) {
                        System.out.println("Ranks different for " + s + " and " + referenceAlgo);
                    }
                } else {
                    set = true;
                    referenceAlgo = s;
                    System.arraycopy(ranks, 0, referenceRanks, 0, n);
                }
                System.out.println(s + " seems to be OK, rank[0] = " + ranks[0]);
            }
        }
    }
}
