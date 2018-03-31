package ru.ifmo.nds.cli;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import ru.ifmo.nds.rundb.IdUtils;
import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

public class Compare extends JCommanderRunnable {
    private Compare() {}

    @Parameter(names = "--left",
            variableArity = true,
            required = true,
            description = "Specify the file name for the left side.")
    private List<String> leftFiles;

    @Parameter(names = "--right",
            variableArity = true,
            required = true,
            description = "Specify the file name for the right side.")
    private List<String> rightFiles;

    private List<Record> readRecords(String file) throws CLIWrapperException {
        try {
            return Records.loadFromFile(Paths.get(file));
        } catch (IOException ex) {
            throw new CLIWrapperException("Error reading from file '" + file + "'.", ex);
        }
    }

    private static double[] fromList(List<Double> list) {
        double[] rv = new double[list.size()];
        for (int i = 0; i < rv.length; ++i) {
            rv[i] = list.get(i);
        }
        return rv;
    }

    private static double median(double[] a) {
        Arrays.sort(a);
        if (a.length % 2 == 1) {
            return a[a.length / 2];
        } else {
            return (a[a.length / 2] + a[a.length / 2 - 1]) / 2;
        }
    }

    // taken from http://math.usask.ca/~laverty/S245/Tables/wmw.pdf
    // and fixed to be symmetric
    private static final int[][] mannWhitneyU = new int[][] {
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1,  0,  0,  0,  0,  1,  1,  1,  1,  1,  2,  2,  2,  2},
            {-1, -1, -1, -1,  0,  1,  1,  2,  2,  3,  3,  4,  4,  5,  5,  6,  6,  7,  7,  8},
            {-1, -1, -1,  0,  1,  2,  3,  4,  4,  5,  6,  7,  8,  9, 10, 11, 11, 12, 13, 14},
            {-1, -1,  0,  1,  2,  3,  5,  6,  7,  8,  9, 11, 12, 13, 14, 15, 17, 18, 19, 20},

            {-1, -1,  1,  2,  3,  5,  6,  8, 10, 11, 13, 14, 16, 17, 19, 21, 22, 24, 25, 27},
            {-1, -1,  1,  3,  5,  6,  8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34},
            {-1,  0,  2,  4,  6,  8, 10, 13, 15, 17, 19, 22, 24, 26, 29, 31, 34, 36, 38, 41},
            {-1,  0,  2,  4,  7, 10, 12, 15, 17, 21, 23, 26, 28, 31, 34, 37, 39, 42, 45, 48},
            {-1,  0,  3,  5,  8, 11, 14, 17, 21, 23, 26, 29, 33, 36, 39, 42, 45, 48, 52, 55},

            {-1,  0,  3,  6,  9, 13, 16, 19, 23, 26, 30, 33, 37, 40, 44, 47, 51, 55, 58, 62},
            {-1,  1,  4,  7, 11, 14, 18, 22, 26, 29, 33, 37, 41, 45, 49, 53, 57, 61, 65, 69},
            {-1,  1,  4,  8, 12, 16, 20, 24, 28, 33, 37, 41, 45, 50, 54, 59, 63, 67, 72, 76},
            {-1,  1,  5,  9, 13, 17, 22, 26, 31, 36, 40, 45, 50, 55, 59, 64, 67, 74, 78, 83},
            {-1,  1,  5, 10, 14, 19, 24, 29, 34, 39, 44, 49, 54, 59, 64, 70, 75, 80, 85, 90},

            {-1,  1,  6, 11, 15, 21, 26, 31, 37, 42, 47, 53, 59, 64, 70, 75, 81, 86, 92, 98},
            {-1,  2,  6, 11, 17, 22, 28, 34, 39, 45, 51, 57, 63, 67, 75, 81, 87, 93, 99, 105},
            {-1,  2,  7, 12, 18, 24, 30, 36, 42, 48, 55, 61, 67, 74, 80, 86, 93, 99, 106, 112},
            {-1,  2,  7, 13, 19, 25, 32, 38, 45, 52, 58, 65, 72, 78, 85, 92, 99, 106, 113, 119},
            {-1,  2,  8, 14, 20, 27, 34, 41, 48, 55, 62, 69, 76, 83, 90, 98, 105, 112, 119, 127},
    };

    private static class Result {
        final int comparisonResult;
        final double relativeMedianDifference;

        private Result(int comparisonResult, double relativeMedianDifference) {
            this.comparisonResult = comparisonResult;
            this.relativeMedianDifference = relativeMedianDifference;
        }
    }

    private static class ComparisonHolder {
        final List<Record> left = new ArrayList<>();
        final List<Record> right = new ArrayList<>();
        final MannWhitneyUTest uTest = new MannWhitneyUTest();

        Result conductAndPrintUTest() {
            double[] lefts = fromList(left.stream()
                    .flatMap(r -> r.getMeasurements().stream())
                    .collect(Collectors.toList()));
            double[] rights = fromList(right.stream()
                    .flatMap(r -> r.getMeasurements().stream())
                    .collect(Collectors.toList()));

            boolean significant;
            if (lefts.length <= 20 && rights.length <= 20) {
                double uMax = uTest.mannWhitneyU(lefts, rights);
                double uMin = (double) (lefts.length) * rights.length - uMax;
                significant = uMin <= mannWhitneyU[lefts.length - 1][rights.length - 1];
            } else {
                significant = uTest.mannWhitneyUTest(lefts, rights) <= 0.05;
            }

            Result rv;
            if (significant) {
                double ml = median(lefts);
                double mr = median(rights);
                double diff = 2 * Math.abs(ml - mr) / (ml + mr);
                if (ml < mr) {
                    System.out.printf("L < R (relative diff = %.2f)", diff);
                    rv = new Result(-1, diff);
                } else {
                    System.out.printf("L > R (relative diff = %.2f)", diff);
                    rv = new Result(+1, diff);
                }
                System.out.println();
                System.out.printf("%35s: %s%n", "=> left",
                        Arrays.stream(lefts).mapToObj(v -> String.format("%.2e", v)).collect(Collectors.toList()));
                System.out.printf("%35s: %s", "=> right",
                        Arrays.stream(rights).mapToObj(v -> String.format("%.2e", v)).collect(Collectors.toList()));
            } else {
                System.out.print("L ~ R");
                rv = new Result(0, 0);
            }
            System.out.println();
            return rv;
        }
    }

    @Override
    protected void run() throws CLIWrapperException {
        Map<String, Map<String, ComparisonHolder>> map = new TreeMap<>();
        for (String leftFile : leftFiles) {
            for (Record record : readRecords(leftFile)) {
                map.computeIfAbsent(record.getAlgorithmId(), a -> new TreeMap<>(IdUtils.getLexicographicalIdComparator()))
                        .computeIfAbsent(record.getDatasetId(), d -> new ComparisonHolder())
                        .left.add(record);
            }
        }
        for (String rightFile : rightFiles) {
            for (Record record : readRecords(rightFile)) {
                map.computeIfAbsent(record.getAlgorithmId(), a -> new TreeMap<>(IdUtils.getLexicographicalIdComparator()))
                        .computeIfAbsent(record.getDatasetId(), d -> new ComparisonHolder())
                        .right.add(record);
            }
        }
        List<String> grandSummary = new ArrayList<>();

        for (Map.Entry<String, Map<String, ComparisonHolder>> byAlgorithm : map.entrySet()) {
            int countStatisticallyLess = 0;
            int countStatisticallyGreater = 0;
            int countStatisticallySame = 0;

            double sumDiffStatisticallyLess = 0;
            double sumDiffStatisticallyGreater = 0;
            double maxDiffStatisticallyLess = 0;
            double maxDiffStatisticallyGreater = 0;

            int bothDatasets = 0;
            int leftOnlyDatasets = 0;
            int rightOnlyDatasets = 0;

            System.out.println(byAlgorithm.getKey());

            for (Map.Entry<String, ComparisonHolder> e : byAlgorithm.getValue().entrySet()) {
                ComparisonHolder h = e.getValue();
                if (!h.left.isEmpty() && !h.right.isEmpty()) {
                    ++bothDatasets;
                    System.out.printf("%35s: ", e.getKey());
                    Result comp = h.conductAndPrintUTest();
                    if (comp.comparisonResult < 0) {
                        ++countStatisticallyLess;
                        sumDiffStatisticallyLess += comp.relativeMedianDifference;
                        maxDiffStatisticallyLess = Math.max(maxDiffStatisticallyLess, comp.relativeMedianDifference);
                    } else if (comp.comparisonResult > 0) {
                        ++countStatisticallyGreater;
                        sumDiffStatisticallyGreater += comp.relativeMedianDifference;
                        maxDiffStatisticallyGreater = Math.max(maxDiffStatisticallyGreater, comp.relativeMedianDifference);
                    } else {
                        ++countStatisticallySame;
                    }
                } else if (!h.left.isEmpty()) {
                    ++leftOnlyDatasets;
                } else {
                    ++rightOnlyDatasets;
                }
            }
            String averageDiffLess = countStatisticallyLess == 0
                    ? ""
                    : String.format(" (average difference %.2f, max difference %.2f)",
                        sumDiffStatisticallyLess / countStatisticallyLess, maxDiffStatisticallyLess);
            String averageDiffGreater = countStatisticallyGreater == 0
                    ? ""
                    : String.format(" (average difference %.2f, max difference %.2f)",
                        sumDiffStatisticallyGreater / countStatisticallyGreater, maxDiffStatisticallyGreater);

            System.out.println("    Summary for " + byAlgorithm.getKey() + ":");
            System.out.println("        Configurations on both sides: " + bothDatasets + ", of them:");
            System.out.println("            statistically L < R: " + countStatisticallyLess + averageDiffLess);
            System.out.println("            statistically L > R: " + countStatisticallyGreater + averageDiffGreater);
            System.out.println("            statistically L ~ R: " + countStatisticallySame);
            System.out.println("        Configurations on left side only: " + leftOnlyDatasets);
            System.out.println("        Configurations on right side only: " + rightOnlyDatasets);
            System.out.println();

            String symbol;
            if (countStatisticallyLess + countStatisticallySame < countStatisticallyGreater) {
                symbol = "[>>]";
            } else if (countStatisticallyGreater + countStatisticallySame < countStatisticallyLess) {
                symbol = "[<<]";
            } else if (countStatisticallyLess + countStatisticallySame < countStatisticallyGreater * 1.1) {
                symbol = "[>?]";
            } else if (countStatisticallyGreater + countStatisticallySame < countStatisticallyLess * 1.1) {
                symbol = "[<?]";
            } else {
                symbol = "[==]";
            }
            grandSummary.add(symbol + " " + byAlgorithm.getKey() + ": [L < R] => "
                    + countStatisticallyLess + averageDiffLess + ", [L ~ R] => "
                    + countStatisticallySame + ", [L > R] => "
                    + countStatisticallyGreater + averageDiffGreater + ".");
        }
        System.out.println("Grand summary:");
        for (String s : grandSummary) {
            System.out.println("    " + s);
        }
    }

    public static void main(String[] args) {
        JCommanderRunnable.run(new Compare(), args);
    }
}
