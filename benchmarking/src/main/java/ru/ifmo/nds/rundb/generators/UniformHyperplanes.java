package ru.ifmo.nds.rundb.generators;

import ru.ifmo.nds.rundb.Dataset;

import java.util.*;

public class UniformHyperplanes {
    private UniformHyperplanes() {}

    private static final int[] supportedSizes = {
            10, 20, 50,
            100, 200, 500,
            1000, 2000, 5000,
            10000, 20000, 50000,
            100000, 200000, 500000,
            1000000
    };
    private static final int[] supportedDimensions = {
            2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30
    };
    private static final int[] supportedFronts = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
    };
    private static final int instanceCount = 10;
    private static final List<String> ids = new ArrayList<>(
            supportedDimensions.length * supportedSizes.length * supportedFronts.length * instanceCount
    );
    static {
        for (int size : supportedSizes) {
            Set<Integer> allFronts = new TreeSet<>();
            for (int f : supportedFronts) {
                allFronts.add(f);
                allFronts.add(size / f);
            }
            for (int dim : supportedDimensions) {
                for (int fronts : allFronts) {
                    ids.add("uniform.hyperplanes.n" + size + ".d" + dim + ".f" + fronts);
                }
            }
        }
    }

    private static final String ERROR_PREFIX = "UniformHyperplanes cannot contain dataset";

    public static DatasetGenerator getInstance() {
        return instance;
    }

    private static final DatasetGenerator instance = new DatasetGenerator() {
        @Override
        public List<String> getAllDatasetIds() {
            return ids;
        }

        @Override
        public String getName() {
            return "UniformHyperplanes";
        }

        @Override
        public Dataset getDataset(String id) {
            Tokenizer tok = new Tokenizer(id);
            tok.expectNext("uniform", ERROR_PREFIX);
            tok.expectNext("hyperplanes", ERROR_PREFIX);
            int n = tok.parseIntWithPrefix("n", ERROR_PREFIX);
            int d = tok.parseIntWithPrefix("d", ERROR_PREFIX);
            int f = tok.parseIntWithPrefix("f", ERROR_PREFIX);
            tok.expectFinish(ERROR_PREFIX);

            Random random = new Random(88325 + 77723692566L * id.hashCode());
            int frontSize = n / f;
            int firstFrontSize = n - (f - 1) * frontSize;

            double[][][] rv = new double[instanceCount][n][d];
            for (int x = 0; x < instanceCount; ++x) {
                for (int i = 0; i < firstFrontSize; ++i) {
                    double sum = 1.0;
                    for (int j = d - 1; j > 0; --j) {
                        rv[x][i][j] = sum * (1 - Math.pow(1 - random.nextDouble(), 1.0 / j));
                        sum -= rv[x][i][j];
                    }
                    rv[x][i][0] = sum;
                }
                for (int i = firstFrontSize; i < n; ++i) {
                    rv[x][i] = rv[x][i - frontSize].clone();
                    for (int j = 0; j < d; ++j) {
                        rv[x][i][j] += 1e-9;
                    }
                }
                Collections.shuffle(Arrays.asList(rv[x]), random);
            }
            return new Dataset(id, rv);
        }
    };
}
