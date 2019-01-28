package ru.ifmo.nds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MeasureGivenTest {
    private static String decimateTime(long time) {
        String raw = String.valueOf(time);
        StringBuilder sb = new StringBuilder();
        for (int i = 12; i >= 0; --i) {
            int index = raw.length() - 1 - i;
            if (index < 0) {
                sb.append(' ');
                if (i > 0 && i % 3 == 0) {
                    sb.append('_');
                }
            } else {
                sb.append(raw.charAt(index));
                if (i > 0 && i % 3 == 0) {
                    sb.append('_');
                }
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        try (BufferedReader in = new BufferedReader(new FileReader(args[0]))) {
            StringTokenizer header = new StringTokenizer(in.readLine());
            int n = Integer.parseInt(header.nextToken());
            int d = Integer.parseInt(header.nextToken());
            double[][] data = new double[n][d];
            for (int i = 0; i < n; ++i) {
                StringTokenizer st = new StringTokenizer(in.readLine());
                for (int j = 0; j < d; ++j) {
                    data[i][j] = Double.parseDouble(st.nextToken());
                }
            }
            int[] ranks = new int[n];
            if (args.length == 1) {
                Set<String> factories = IdCollection.getAllNonDominatedSortingIDs();
                List<NonDominatedSorting> sortings = new ArrayList<>();
                for (String f : factories) {
                    try {
                        sortings.add(IdCollection.getNonDominatedSortingFactory(f).getInstance(n, d));
                    } catch (Throwable th) {
                        System.out.println("[error] Could not create '" + f + "': " + th.getMessage());
                    }
                }
                while (System.in.available() == 0) {
                    for (NonDominatedSorting sorting : sortings) {
                        long t0 = System.nanoTime();
                        sorting.sort(data, ranks);
                        long time = System.nanoTime() - t0;
                        System.out.println(decimateTime(time) + " ms <- " + sorting.getName());
                    }
                    System.out.println();
                }
            } else {
                NonDominatedSorting sorting = IdCollection.getNonDominatedSortingFactory(args[1]).getInstance(n, d);
                while (System.in.available() == 0) {
                    long t0 = System.nanoTime();
                    sorting.sort(data, ranks);
                    long time = System.nanoTime() - t0;
                    System.out.println(time + " ms");
                }
            }
        }
    }
}
