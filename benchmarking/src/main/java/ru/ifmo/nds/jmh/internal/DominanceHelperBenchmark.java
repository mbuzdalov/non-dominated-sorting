package ru.ifmo.nds.jmh.internal;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import ru.ifmo.nds.util.DominanceHelper;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 10)
@Warmup(time = 1, iterations = 10)
@Measurement(time = 1, iterations = 5)
@Fork(value = 10)
public class DominanceHelperBenchmark {
    @Param(value = {"2", "3", "4", "5", "6", "8", "10", "12", "14", "17", "20"})
    private int size;

    @Param(value = {"random", "leftDominates", "rightDominates", "equal"})
    private String type;

    private double[][][] instances;

    @Setup
    public void initialize() {
        int nInstances = 1000;
        instances = new double[nInstances][2][size];

        Random random = new Random(size * 8235435177845322L + type.hashCode());
        switch (type) {
            case "random":
                for (int i = 0; i < nInstances; ++i) {
                    for (int j = 0; j < size; ++j) {
                        instances[i][0][j] = random.nextDouble();
                        instances[i][1][j] = random.nextDouble();
                    }
                }
                break;
            case "leftDominates":
                for (int i = 0; i < nInstances; ++i) {
                    int diffIndex = random.nextInt(size);
                    for (int j = 0; j < size; ++j) {
                        instances[i][0][j] = random.nextDouble();
                        if (j == diffIndex || random.nextBoolean()) {
                            instances[i][1][j] = instances[i][0][j] + random.nextDouble();
                        } else {
                            instances[i][1][j] = instances[i][0][j];
                        }
                    }
                }
                break;
            case "rightDominates":
                for (int i = 0; i < nInstances; ++i) {
                    int diffIndex = random.nextInt(size);
                    for (int j = 0; j < size; ++j) {
                        instances[i][1][j] = random.nextDouble();
                        if (j == diffIndex || random.nextBoolean()) {
                            instances[i][0][j] = instances[i][1][j] + random.nextDouble();
                        } else {
                            instances[i][0][j] = instances[i][1][j];
                        }
                    }
                }
                break;
            case "equal":
                for (int i = 0; i < nInstances; ++i) {
                    for (int j = 0; j < size; ++j) {
                        instances[i][0][j] = instances[i][1][j] = random.nextDouble();
                    }
                }
                break;
        }
    }

    @Benchmark
    public void dominanceComparison(Blackhole bh) {
        for (double[][] instance : instances) {
            bh.consume(DominanceHelper.dominanceComparison(instance[0], instance[1], size));
        }
    }

    @Benchmark
    public void strictlyDominates(Blackhole bh) {
        for (double[][] instance : instances) {
            bh.consume(DominanceHelper.strictlyDominates(instance[0], instance[1], size));
        }
    }
}
