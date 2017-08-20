package ru.ifmo.nds.simple;

import ru.ifmo.nds.*;
import ru.ifmo.nds.rundb.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
public class SimpleBenchmarking {
    private final NonDominatedSorting instance;
    private final List<String> datasetIds;
    private final List<String> warmupIds;

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private long blackHole = 0;

    private int multiple;
    private Dataset lastDataset;

    public SimpleBenchmarking(NonDominatedSortingFactory algorithm, List<String> datasetIds) {
        int maxN = 0, maxD = 0;
        int minN = Integer.MAX_VALUE, minD = Integer.MAX_VALUE;

        String idMaxN = null, idMaxD = null, idMinD = null;
        Set<String> warmupSet = new HashSet<>();

        for (String id : datasetIds) {
            int n = (int) IdUtils.extract(id, "n");
            int d = (int) IdUtils.extract(id, "d");
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
        instance = algorithm.getInstance(maxN, maxD);
        this.datasetIds = new ArrayList<>(datasetIds);
        warmupSet.add(idMaxN);
        warmupSet.add(idMaxD);
        warmupSet.add(idMinD);
        this.warmupIds = new ArrayList<>(warmupSet);
    }

    private double measureImpl() {
        Dataset dataset = lastDataset;
        for (int attempt = 0; ; ++attempt) {
            long wallClock0 = System.nanoTime();
            long thread0 = threadMXBean.getCurrentThreadCpuTime();

            for (int i = 0; i < multiple; ++i) {
                blackHole += dataset.runAlgorithm(instance, dataset.getMaxNumberOfPoints());
            }

            long wallClockTime = System.nanoTime() - wallClock0;
            long threadTime = threadMXBean.getCurrentThreadCpuTime() - thread0;

            if (threadTime < 200_000_000) {
                return Double.NEGATIVE_INFINITY;
            }
            if (wallClockTime < threadTime * 1.02) {
                return (double) wallClockTime / multiple;
            }
            System.out.println("[warning] remeasuring as thread time (" + threadTime
                    + ") is much less than wall-clock time (" + wallClockTime
                    + "): Attempt " + attempt);
        }
    }

    private double measure(Dataset dataset) {
        if (dataset != lastDataset) {
            multiple = 0;
            lastDataset = dataset;
            while (true) {
                multiple = multiple == 0 ? 1 : multiple * 2;
                double result = measureImpl();
                if (!Double.isInfinite(result)) {
                    if (result < 3e8) {
                        multiple *= 2;
                    }
                    return result;
                }
            }
        } else {
            while (true) {
                double rv = measureImpl();
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
        return (max - min) < 0.02 * (max + min);
    }

    private void warmUp() {
        List<Dataset> warmUpInputs = warmupIds.stream().map(AllDatasets::getDataset).collect(Collectors.toList());
        List<Dataset> warmUpAndControl = Dataset.concatenateAndSplit("$.warm.up", warmUpInputs, 2);
        Dataset warmUp = warmUpAndControl.get(0);
        Dataset control = warmUpAndControl.get(1);

        boolean controlPassed;
        do {
            System.out.println("[info] warm-up dataset: " + warmUp.getNumberOfInstances() + " instances");
            System.out.println("[info] warm-up measurements:");
            List<Double> warmUpMeasurements = new ArrayList<>();
            while (warmUpMeasurements.size() < 10 ||
                    !practicallySame(warmUpMeasurements.subList(warmUpMeasurements.size() / 2, warmUpMeasurements.size()))) {
                double measurement = measure(warmUp);
                System.out.println("[info]     " + measurement);
                warmUpMeasurements.add(measurement);
            }

            System.out.println("[info] control dataset: " + control.getNumberOfInstances() + " instances");
            System.out.println("[info] warm-up measurements:");
            List<Double> controlMeasurements = new ArrayList<>();
            for (int i = 0; i < warmUpMeasurements.size(); ++i) {
                double measurement = measure(control);
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
            double cpuFrequency,
            String cpuModel,
            String comment,
            int repeats
    ) {
        List<Record> rv = new ArrayList<>(datasetIds.size());
        List<List<Double>> datasetIdTimes = new ArrayList<>();

        warmUp();

        for (String datasetId : datasetIds) {
            System.out.print("[info] " + datasetId + ":");
            Dataset dataset = AllDatasets.getDataset(datasetId);
            List<Double> results = new ArrayList<>();
            do {
                results.clear();
                for (int t = 0; t < repeats; ++t) {
                    double result = measure(dataset) / dataset.getNumberOfInstances() / 1e9;
                    System.out.printf(" %.3e", result);
                    results.add(result);
                }
            } while (!practicallySame(results));
            datasetIdTimes.add(results);
            System.out.println();
        }

        System.out.println("[info] black hole value: " + blackHole);
        LocalDateTime time = LocalDateTime.now();
        for (int i = 0; i < datasetIds.size(); ++i) {
            rv.add(new Record(
                    instance.getName(),
                    datasetIds.get(i),
                    "Simple",
                    benchmarkAuthor,
                    time,
                    cpuFrequency,
                    cpuModel,
                    Collections.emptyList(),
                    datasetIdTimes.get(i),
                    comment));
        }
        return rv;
    }

    private static final List<String> jmhIds = Arrays.asList(
            "uniform.hypercube.n10.d2", "uniform.hypercube.n10.d3", "uniform.hypercube.n10.d4",
            "uniform.hypercube.n10.d5", "uniform.hypercube.n10.d6", "uniform.hypercube.n10.d7",
            "uniform.hypercube.n10.d8", "uniform.hypercube.n10.d9", "uniform.hypercube.n10.d10",
            "uniform.hypercube.n100.d2", "uniform.hypercube.n100.d3", "uniform.hypercube.n100.d4",
            "uniform.hypercube.n100.d5", "uniform.hypercube.n100.d6", "uniform.hypercube.n100.d7",
            "uniform.hypercube.n100.d8", "uniform.hypercube.n100.d9", "uniform.hypercube.n100.d10",
            "uniform.hypercube.n1000.d2", "uniform.hypercube.n1000.d3", "uniform.hypercube.n1000.d4",
            "uniform.hypercube.n1000.d5", "uniform.hypercube.n1000.d6", "uniform.hypercube.n1000.d7",
            "uniform.hypercube.n1000.d8", "uniform.hypercube.n1000.d9", "uniform.hypercube.n1000.d10",
            "uniform.hypercube.n10000.d2", "uniform.hypercube.n10000.d3", "uniform.hypercube.n10000.d4",
            "uniform.hypercube.n10000.d5", "uniform.hypercube.n10000.d6", "uniform.hypercube.n10000.d7",
            "uniform.hypercube.n10000.d8", "uniform.hypercube.n10000.d9", "uniform.hypercube.n10000.d10",

            "uniform.hyperplanes.n10.d2.f1", "uniform.hyperplanes.n10.d3.f1", "uniform.hyperplanes.n10.d4.f1",
            "uniform.hyperplanes.n10.d5.f1", "uniform.hyperplanes.n10.d6.f1", "uniform.hyperplanes.n10.d7.f1",
            "uniform.hyperplanes.n10.d8.f1", "uniform.hyperplanes.n10.d9.f1", "uniform.hyperplanes.n10.d10.f1",
            "uniform.hyperplanes.n100.d2.f1", "uniform.hyperplanes.n100.d3.f1", "uniform.hyperplanes.n100.d4.f1",
            "uniform.hyperplanes.n100.d5.f1", "uniform.hyperplanes.n100.d6.f1", "uniform.hyperplanes.n100.d7.f1",
            "uniform.hyperplanes.n100.d8.f1", "uniform.hyperplanes.n100.d9.f1", "uniform.hyperplanes.n100.d10.f1",
            "uniform.hyperplanes.n1000.d2.f1", "uniform.hyperplanes.n1000.d3.f1", "uniform.hyperplanes.n1000.d4.f1",
            "uniform.hyperplanes.n1000.d5.f1", "uniform.hyperplanes.n1000.d6.f1", "uniform.hyperplanes.n1000.d7.f1",
            "uniform.hyperplanes.n1000.d8.f1", "uniform.hyperplanes.n1000.d9.f1", "uniform.hyperplanes.n1000.d10.f1",
            "uniform.hyperplanes.n10000.d2.f1", "uniform.hyperplanes.n10000.d3.f1", "uniform.hyperplanes.n10000.d4.f1",
            "uniform.hyperplanes.n10000.d5.f1", "uniform.hyperplanes.n10000.d6.f1", "uniform.hyperplanes.n10000.d7.f1",
            "uniform.hyperplanes.n10000.d8.f1", "uniform.hyperplanes.n10000.d9.f1", "uniform.hyperplanes.n10000.d10.f1"
    );

    private static final List<NonDominatedSortingFactory> factories = Arrays.asList(
            BestOrderSort.getImprovedImplementation(),
            BestOrderSort.getProteekImplementation(),
            CornerSort.getInstance(),
            DeductiveSort.getInstance(),
            DominanceTree.getNoPresortInsertion(true),
            DominanceTree.getNoPresortInsertion(false),
            DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.NO_DELAYED_INSERTION),
            DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION),
            DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.DELAYED_INSERTION_SEQUENTIAL_CONCATENATION),
            DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.NO_DELAYED_INSERTION),
            DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION),
            DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.DELAYED_INSERTION_SEQUENTIAL_CONCATENATION),
            ENS.getENS_BS(),
            ENS.getENS_SS(),
            FastNonDominatedSorting.getLinearMemoryImplementation(),
            FastNonDominatedSorting.getOriginalVersion(),
            JensenFortinBuzdalov.getFenwickSweepImplementation(),
            JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(),
            JensenFortinBuzdalov.getRedBlackTreeSweepHybridImplementation(),
            SumitMishraDivideConquer.getSumitImplementation2016(true, true),
            SumitMishraDivideConquer.getSumitImplementation2016(true, false),
            SumitMishraDivideConquer.getSumitImplementation2016(false, true),
            SumitMishraDivideConquer.getSumitImplementation2016(false, false)
    );

    public static void main(String[] args) throws IOException {
        int algoId = Integer.parseInt(args[0]);
        SimpleBenchmarking benchmarking = new SimpleBenchmarking(factories.get(algoId), jmhIds);
        LocalDateTime beginning = LocalDateTime.now();
        List<Record> records = benchmarking.evaluate(
                "Maxim Buzdalov",
                2.4e9,
                "Intel Core 2 Duo P8600",
                "First simple benchmark",
                3
        );
        System.out.println("[info] Total time to run the benchmark: " + Duration.between(beginning, LocalDateTime.now()));
        Records.saveToFile(records, Paths.get("/home/maxbuzz/owncloud/non-dominated-sorting/simple-" + algoId + ".json"));
    }
}
