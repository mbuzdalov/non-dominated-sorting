package ru.ifmo.nds.simple;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import oshi.SystemInfo;

import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.rundb.Dataset;
import ru.ifmo.nds.rundb.IdUtils;
import ru.ifmo.nds.rundb.Record;

/**
 * This is a "simple" benchmarking tool for non-dominated sorting algorithms.
 *
 * "Simple" serves as a contrast to the JMH utility. We aim to measure more inputs within same time limits.
 * To achieve this, we try to relax the following conditions:
 * <ul>
 *     <li>
 *         Benchmarks for non-dominated sorting algorithms are done with a single algorithm and multiple tests,
 *         so most often the same code is used for all tests. We warm up the code on the representative subset
 *         of the required datasets, then run the algorithm on all datasets to measure the running time.
 *     </li>
 *     <li>
 *         We are measuring operations which are more costly than, say, a single <code>Math.log</code>,
 *         so we can use trivial deoptimization (black-hole) techniques.
 *     </li>
 * </ul>
 */
public class SimpleBenchmark {
    private final String algorithmId;
    private final NonDominatedSorting instance;
    private final List<String> datasetIds;
    private final List<String> warmupIds;

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private long blackHole = 0;

    private int multiple;
    private Dataset lastDataset;

    public SimpleBenchmark(String algorithmId, List<String> datasetIds) {
        this.algorithmId = algorithmId;
        int maxN = 0, maxD = 0;
        int minN = Integer.MAX_VALUE, minD = Integer.MAX_VALUE;

        String idMaxN = null, idMaxD = null, idMinD = null;
        Set<String> warmupSet = new HashSet<>();

        for (String id : datasetIds) {
            int n = IdUtils.extract(id, "n");
            int d = IdUtils.extract(id, "d");
            if (maxN < n) {
                maxN = n;
                idMinD = null;
                minD = Integer.MAX_VALUE;
                idMaxN = id;
            }
            if (maxN == n && minD > d) {
                minD = d;
                idMinD = id;
            }
            if (maxD < d) {
                maxD = d;
                idMaxD = id;
            }
            if (minN >= n) {
                if (minN > n) {
                    warmupSet.clear();
                    minN = n;
                }
                warmupSet.add(id);
            }
        }
        instance = IdCollection.getNonDominatedSortingFactory(algorithmId).getInstance(maxN, maxD);
        this.datasetIds = new ArrayList<>(datasetIds);
        warmupSet.add(idMaxN);
        warmupSet.add(idMaxD);
        warmupSet.add(idMinD);
        this.warmupIds = new ArrayList<>(warmupSet);
    }

    private double measureImpl(boolean usePrintln) {
        Dataset dataset = lastDataset;
        for (int attempt = 0; ; ++attempt) {
            long wallClock0 = System.nanoTime();
            long thread0 = threadMXBean.getCurrentThreadCpuTime();

            for (int i = 0, limit = multiple; i < limit; ++i) {
                blackHole += dataset.runAlgorithm(instance, dataset.getMaxNumberOfPoints());
            }

            long wallClockTime = System.nanoTime() - wallClock0;
            long threadTime = threadMXBean.getCurrentThreadCpuTime() - thread0;

            if (threadTime < 200_000_000) {
                return Double.NEGATIVE_INFINITY;
            }
            if (threadTime <= wallClockTime * 1.01 && wallClockTime <= threadTime * 1.01) {
                return (double) wallClockTime / multiple;
            }
            if (usePrintln) {
                if (threadTime > wallClockTime) {
                    System.out.println("[warning] remeasuring as thread time (" + threadTime
                            + ") is GREATER than wall-clock time (" + wallClockTime
                            + "): Attempt " + attempt);
                } else {
                    System.out.println("[warning] remeasuring as thread time (" + threadTime
                            + ") is much less than wall-clock time (" + wallClockTime
                            + "): Attempt " + attempt);
                }
            } else {
                System.out.print(threadTime > wallClockTime ? "[?]" : "[!]");
            }
        }
    }

    private double measure(Dataset dataset, boolean usePrintln) {
        if (dataset != lastDataset) {
            multiple = 0;
            lastDataset = dataset;
            while (true) {
                multiple = multiple == 0 ? 1 : multiple * 2;
                double result = measureImpl(usePrintln);
                if (!Double.isInfinite(result)) {
                    return result;
                }
            }
        } else {
            while (true) {
                double rv = measureImpl(usePrintln);
                if (!Double.isInfinite(rv)) {
                    return rv;
                } else {
                    multiple *= 2;
                }
            }
        }
    }

    private boolean practicallySame(List<Double> measurements) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double v : measurements) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return (max - min) < 0.01 * (max + min);
    }

    private void warmUp() {
        List<Dataset> warmUpInputs = warmupIds.stream().map(IdCollection::getDataset).collect(Collectors.toList());
        List<Dataset> warmUpAndControl = Dataset.concatenateAndSplitIntoWarmupAndControl("$", warmUpInputs);
        Dataset warmUp = warmUpAndControl.get(0);
        Dataset control = warmUpAndControl.get(1);

        boolean controlPassed;
        do {
            System.out.println("[info] warm-up dataset: " + warmUp.getNumberOfInstances() + " instances");
            System.out.println("[info] warm-up measurements:");
            List<Double> warmUpMeasurements = new ArrayList<>();
            while (warmUpMeasurements.size() < 10 ||
                    !practicallySame(warmUpMeasurements.subList(warmUpMeasurements.size() / 2, warmUpMeasurements.size()))) {
                double measurement = measure(warmUp, true);
                System.out.println("[info]     " + measurement);
                warmUpMeasurements.add(measurement);
            }

            System.out.println("[info] control dataset: " + control.getNumberOfInstances() + " instances");
            System.out.println("[info] warm-up measurements:");
            List<Double> controlMeasurements = new ArrayList<>();
            for (int i = 0; i < warmUpMeasurements.size(); ++i) {
                double measurement = measure(control, true);
                System.out.println("[info]     " + measurement);
                controlMeasurements.add(measurement);
            }
            controlPassed = practicallySame(controlMeasurements);
            if (controlPassed) {
                System.out.println("[info] control passed. proceed with actual measurements!");
            } else {
                System.out.println("[warning] control not passed, repeating warm-up!");
            }
        } while (!controlPassed);
    }

    public List<Record> evaluate(
            String benchmarkAuthor,
            String comment,
            int repeats
    ) {
        List<Record> rv = new ArrayList<>(datasetIds.size());
        List<List<Double>> datasetIdTimes = new ArrayList<>();

        warmUp();

        for (String datasetId : datasetIds) {
            System.out.print("[info] " + datasetId + ":");
            Dataset dataset = IdCollection.getDataset(datasetId);
            List<Double> results = new ArrayList<>(repeats);
            System.gc();
            System.gc();
            do {
                results.clear();
                for (int t = 0; t < repeats; ++t) {
                    double result = measure(dataset, false) / dataset.getNumberOfInstances() / 1e9;
                    System.out.printf(" %.3e", result);
                    results.add(result);
                }
            } while (!practicallySame(results));
            datasetIdTimes.add(results);
            System.out.println();
        }

        String javaRuntimeVersion = System.getProperty("java.runtime.version");
        String cpuModel = new SystemInfo().getHardware().getProcessor().getName();

        System.out.println("[info] Java runtime version: '" + javaRuntimeVersion + "'.");
        System.out.println("[info] CPU model: '" + cpuModel + "'.");
        System.out.println("[info] black hole value: " + blackHole);

        LocalDateTime time = LocalDateTime.now();
        for (int i = 0; i < datasetIds.size(); ++i) {
            rv.add(new Record(
                    algorithmId,
                    datasetIds.get(i),
                    "Simple",
                    benchmarkAuthor,
                    time,
                    cpuModel,
                    javaRuntimeVersion,
                    datasetIdTimes.get(i),
                    comment));
        }
        return rv;
    }
}
