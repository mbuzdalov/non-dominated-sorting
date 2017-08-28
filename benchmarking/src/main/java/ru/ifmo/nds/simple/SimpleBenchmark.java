package ru.ifmo.nds.simple;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import oshi.SystemInfo;

import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.rundb.Dataset;
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
    private final NonDominatedSortingFactory factory;
    private final List<String> datasetIds;
    private final double requiredPrecision;
    private final boolean keepSilent;

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private long blackHole = 0;

    private int multiple;
    private Dataset lastDataset;
    private NonDominatedSorting instance;

    public SimpleBenchmark(String algorithmId, List<String> datasetIds, double requiredPrecision, boolean keepSilent) {
        this.algorithmId = algorithmId;
        this.requiredPrecision = requiredPrecision;
        this.keepSilent = keepSilent;

        if (requiredPrecision <= 0) {
            throw new IllegalArgumentException("Parameter 'requiredPrecision' should be at least 1.0");
        }

        factory = IdCollection.getNonDominatedSortingFactory(algorithmId);
        this.datasetIds = new ArrayList<>(datasetIds);
    }

    private double measureImpl() {
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
            if (threadTime <= wallClockTime * (1 + requiredPrecision)
                    && wallClockTime <= threadTime * (1 + requiredPrecision)) {
                return (double) wallClockTime / multiple;
            }
            if (!keepSilent) {
                if (attempt < 10) {
                    System.out.print(threadTime > wallClockTime ? "[?]" : "[!]");
                } else {
                    System.out.print("[thread=" + threadTime + ",wc=" + wallClockTime + "]");
                }
            }
        }
    }

    private double measure(Dataset dataset) {
        if (dataset != lastDataset) {
            multiple = 0;
            lastDataset = dataset;
            if (instance != null) {
                instance.close();
            }
            instance = factory.getInstance(dataset.getMaxNumberOfPoints(), dataset.getMaxDimension());
            while (true) {
                multiple = multiple == 0 ? 1 : multiple * 2;
                double result = measureImpl();
                if (!Double.isInfinite(result)) {
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
        // average += (average * requiredPrecision)
        return (max - min) < requiredPrecision * (max + min);
    }

    private boolean evaluateOnDatasets(List<List<Double>> datasetIdTimes, int repeats) {
        for (String datasetId : datasetIds) {
            if (!keepSilent) {
                System.out.print("[info] " + datasetId + ":");
            }
            Dataset dataset = IdCollection.getDataset(datasetId);
            List<Double> results = new ArrayList<>(repeats);
            System.gc();
            System.gc();
            for (int t = 0; t < repeats; ++t) {
                double result = measure(dataset) / dataset.getNumberOfInstances() / 1e9;
                if (!keepSilent) {
                    System.out.printf(" %.3e", result);
                }
                results.add(result);
            }
            if (!practicallySame(results)) {
                System.out.println("\n[info] Instability found, restarting evaluations, datasets before restart: "
                        + datasetIdTimes.size());
                return false;
            }
            datasetIdTimes.add(results);
            if (!keepSilent) {
                System.out.println();
            }
        }
        return true;
    }

    public List<Record> evaluate(
            String benchmarkAuthor,
            String comment,
            int repeats
    ) {
        List<List<Double>> datasetIdTimes = new ArrayList<>(datasetIds.size());
        do {
            datasetIdTimes.clear();
        } while (!evaluateOnDatasets(datasetIdTimes, repeats));

        // From now on, nobody cares about benchmarking precision anymore...

        String javaRuntimeVersion = System.getProperty("java.runtime.version");
        String cpuModel = new SystemInfo().getHardware().getProcessor().getName();

        System.out.println("[info] Finished measuring: '" + algorithmId + "'");
        System.out.println("[info] Java runtime version: '" + javaRuntimeVersion + "'.");
        System.out.println("[info] CPU model: '" + cpuModel + "'.");
        System.out.println("[info] black hole value: " + blackHole);

        LocalDateTime time = LocalDateTime.now();
        List<Record> rv = new ArrayList<>(datasetIds.size());
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
