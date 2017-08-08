package ru.ifmo.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import ru.ifmo.NonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public abstract class AbstractBenchmark {
    @Param({"2", "3", "4", "5", "6", "7", "8", "9", "10"})
    private int dimension;

    @Param("10")
    private int nInstances;

    private NonDominatedSorting sorting;
    private Dataset[] datasets;

    @Setup
    public void initializeSorterAndData(BenchmarkParams params) {
        String benchmarkName = params.getBenchmark();
        String benchmarkMethodName = benchmarkName.substring(benchmarkName.lastIndexOf('.') + 1);
        StringTokenizer tokenizer = new StringTokenizer(benchmarkMethodName, "_");
        String benchmarkType = tokenizer.nextToken();
        Map<String, Integer> benchmarkParams = new HashMap<>();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            int firstNumber = 0;
            while (!Character.isDigit(token.charAt(firstNumber))) {
                ++firstNumber;
            }
            String paramName = token.substring(0, firstNumber);
            int paramValue = Integer.parseInt(token.substring(firstNumber));
            benchmarkParams.put(paramName, paramValue);
        }

        benchmarkParams.put("dimension", dimension);

        datasets = new Dataset[nInstances];
        for (int i = 0; i < nInstances; ++i) {
            int realSeed = (benchmarkMethodName + "%" + dimension + "%" + i).hashCode();
            benchmarkParams.put("seed", realSeed);
            datasets[i] = Dataset.generate(benchmarkType, benchmarkParams);
        }
        sorting = getFactory().getInstance(benchmarkParams.get("N"), dimension);
    }

    private int runBenchmark() {
        int sum = 0;
        for (Dataset dataset : datasets) {
            sum += dataset.runSortingOnMe(sorting);
        }
        return sum;
    }

    @TearDown
    public void destroySorter() {
        sorting.close();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHypercube_N10() {
        return runBenchmark();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHypercube_N100() {
        return runBenchmark();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHypercube_N1000() {
        return runBenchmark();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 20)
    @Measurement(iterations = 5, time = 20)
    public int uniformHypercube_N10000() {
        return runBenchmark();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHyperplane_N10() {
        return runBenchmark();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHyperplane_N100() {
        return runBenchmark();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHyperplane_N1000() {
        return runBenchmark();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 20)
    @Measurement(iterations = 5, time = 20)
    public int uniformHyperplane_N10000() {
        return runBenchmark();
    }

    protected abstract NonDominatedSortingFactory getFactory();
}
