package ru.ifmo.nds.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import ru.ifmo.nds.util.DominanceHelper;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 10)
@Warmup(iterations = 4)
@Measurement(iterations = 3)
@Fork(value = 3)
public class DominanceHelperBenchmark {
    @Param(value = {"2", "5", "10", "20"})
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
                        if (random.nextBoolean() || j == diffIndex) {
                            instances[i][1][j] = instances[i][0][j];
                        } else {
                            instances[i][1][j] = instances[i][0][j] + random.nextDouble();
                        }
                    }
                }
                break;
            case "rightDominates":
                for (int i = 0; i < nInstances; ++i) {
                    int diffIndex = random.nextInt(size);
                    for (int j = 0; j < size; ++j) {
                        instances[i][1][j] = random.nextDouble();
                        if (random.nextBoolean() || j == diffIndex) {
                            instances[i][0][j] = instances[i][1][j];
                        } else {
                            instances[i][0][j] = instances[i][1][j] + random.nextDouble();
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
