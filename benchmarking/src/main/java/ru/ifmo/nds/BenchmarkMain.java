package ru.ifmo.nds;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import ru.ifmo.nds.rundb.IdUtils;
import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class BenchmarkMain {
    private BenchmarkMain() {}

    private static final List<Module> modules = Arrays.asList(
            new Compare(), new Merge()
    );

    private static void printErrorMessageAndExit(String errorMessage, Throwable cause) {
        if (errorMessage != null) {
            System.err.println("Error: " + errorMessage);
        }
        if (cause != null) {
            cause.printStackTrace();
        }
        System.err.println("Usage: ru.ifmo.nds.BenchmarkMain <command> [parameters], where <command> is one of:");
        for (Module module : modules) {
            for (String explanation : module.getExplanation()) {
                System.err.print("   ");
                System.err.println(explanation);
            }
        }
        System.exit(1);
        throw new UnsupportedOperationException("System.exit(1) did not exit");
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printErrorMessageAndExit(null, null);
        }
        for (Module m : modules) {
            if (m.getName().equals(args[0])) {
                m.run(Arrays.copyOfRange(args, 1, args.length));
                return;
            }
        }
        printErrorMessageAndExit("Unknown command: " + args[0], null);
    }

    private interface Module {
        String getName();
        String[] getExplanation();
        void run(String[] args);
    }

    private static class Compare implements Module {
        private static MannWhitneyUTest uTest = new MannWhitneyUTest();

        @Override
        public String getName() {
            return "compare";
        }

        @Override
        public String[] getExplanation() {
            return new String[] {
                    "compare <file1> <file2>",
                    "  Compare matching records in data files <file1> and <file2>."
            };
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
        private static int[][] mannWhitneyU = new int[][] {
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

        static class ComparisonHolder {
            final List<Record> left = new ArrayList<>();
            final List<Record> right = new ArrayList<>();

            int conductAndPrintUTest() {
                double[] lefts = fromList(left.stream()
                        .flatMap(r -> r.getReleaseMeasurements().stream())
                        .collect(Collectors.toList()));
                double[] rights = fromList(right.stream()
                        .flatMap(r -> r.getReleaseMeasurements().stream())
                        .collect(Collectors.toList()));

                boolean significant;
                if (lefts.length <= 20 && rights.length <= 20) {
                    double uMax = uTest.mannWhitneyU(lefts, rights);
                    double uMin = (double) (lefts.length) * rights.length - uMax;
                    significant = uMin <= mannWhitneyU[lefts.length - 1][rights.length - 1];
                } else {
                    significant = uTest.mannWhitneyUTest(lefts, rights) <= 0.05;
                }

                int rv;
                if (significant) {
                    if (median(lefts) < median(rights)) {
                        System.out.print("L < R");
                        rv = -1;
                    } else {
                        System.out.print("L > R");
                        rv = 1;
                    }
                } else {
                    System.out.print("L ~ R");
                    rv = 0;
                }
                System.out.println();
                return rv;
            }
        }

        @Override
        public void run(String[] args) {
            if (args.length != 2) {
                printErrorMessageAndExit("Too few arguments.", null);
            }
            Map<String, Map<String, ComparisonHolder>> map = new TreeMap<>();
            try {
                for (Record record : Records.loadFromFile(Paths.get(args[0]))) {
                    map.computeIfAbsent(record.getAlgorithmId(), a -> new TreeMap<>(IdUtils.getLexicographicalIdComparator()))
                            .computeIfAbsent(record.getDatasetId(), d -> new ComparisonHolder())
                            .left.add(record);
                }
            } catch (IOException ex) {
                printErrorMessageAndExit("Error reading from file '" + args[0] + "'", ex);
            }
            try {
                for (Record record : Records.loadFromFile(Paths.get(args[1]))) {
                    map.computeIfAbsent(record.getAlgorithmId(), a -> new TreeMap<>(IdUtils.getLexicographicalIdComparator()))
                            .computeIfAbsent(record.getDatasetId(), d -> new ComparisonHolder())
                            .right.add(record);
                }
            } catch (IOException ex) {
                printErrorMessageAndExit("Error reading from file '" + args[1] + "'", ex);
            }

            List<String> grandSummary = new ArrayList<>();

            for (Map.Entry<String, Map<String, ComparisonHolder>> byAlgorithm : map.entrySet()) {
                int countStatisticallyLess = 0;
                int countStatisticallyGreater = 0;
                int countStatisticallySame = 0;

                int bothDatasets = 0;
                int leftOnlyDatasets = 0;
                int rightOnlyDatasets = 0;

                System.out.println(byAlgorithm.getKey());

                for (Map.Entry<String, ComparisonHolder> e : byAlgorithm.getValue().entrySet()) {
                    ComparisonHolder h = e.getValue();
                    if (!h.left.isEmpty() && !h.right.isEmpty()) {
                        ++bothDatasets;
                        System.out.printf("%35s: ", e.getKey());
                        int comp = h.conductAndPrintUTest();
                        if (comp < 0) {
                            ++countStatisticallyLess;
                        } else if (comp > 0) {
                            ++countStatisticallyGreater;
                        } else {
                            ++countStatisticallySame;
                        }
                    } else if (!h.left.isEmpty()) {
                        ++leftOnlyDatasets;
                    } else {
                        ++rightOnlyDatasets;
                    }
                }
                System.out.println("    Summary for " + byAlgorithm.getKey() + ":");
                System.out.println("        Configurations on both sides: " + bothDatasets + ", of them:");
                System.out.println("            statistically less:     " + countStatisticallyLess);
                System.out.println("            statistically greater:  " + countStatisticallyGreater);
                System.out.println("            statistically unsure:   " + countStatisticallySame);
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
                grandSummary.add(symbol + " " + byAlgorithm.getKey() + ": "
                        + countStatisticallyLess + " less, "
                        + countStatisticallySame + " same, "
                        + countStatisticallyGreater + " greater.");
            }
            System.out.println("Grand summary:");
            for (String s : grandSummary) {
                System.out.println("    " + s);
            }
        }
    }

    private static class Merge implements Module {
        @Override
        public String getName() {
            return "merge";
        }

        @Override
        public String[] getExplanation() {
            return new String[] {
                    "merge <file1> <file2> ... <fileN> --output <output-file>",
                    "  Merge all data from files <file1> to <fileN>",
                    "  and place the results in <output-file>.",
                    "  All input files are assumed to be JSON files,",
                    "  the output file will be a JSON file."
            };
        }

        @Override
        public void run(String[] args) {
            int outputIndex = Arrays.asList(args).indexOf("--output");
            if (outputIndex != args.length - 2) {
                printErrorMessageAndExit("--output is not the next-to-last argument.", null);
            }
            List<Record> allRecords = new ArrayList<>();
            for (int i = 0; i < outputIndex; ++i) {
                try {
                    allRecords.addAll(Records.loadFromFile(Paths.get(args[i])));
                } catch (IOException ex) {
                    printErrorMessageAndExit("Error reading data from file '" + args[i] + ".'", ex);
                }
            }
            String outputFileName = args[outputIndex + 1];
            try {
                Records.saveToFile(allRecords, Paths.get(outputFileName));
            } catch (IOException ex) {
                printErrorMessageAndExit("Error writing results to file '" + outputFileName + "'.", ex);
            }
        }
    }
}
