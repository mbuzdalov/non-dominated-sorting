package ru.ifmo.nds.rundb.generators;

import ru.ifmo.nds.rundb.Dataset;

import java.util.*;

public class UniformHypercube {
    private UniformHypercube() {}

    private static final int[] supportedSizes = {
            10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000
    };
    private static final int[] supportedDimensions = {
            2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30
    };
    private static final int instanceCount = 10;
    private static final List<String> ids = new ArrayList<>(
            supportedDimensions.length * supportedSizes.length
    );
    static {
        for (int size : supportedSizes) {
            for (int dim : supportedDimensions) {
                ids.add("uniform.hypercube.n" + size + ".d" + dim);
            }
        }
    }

    private static final String ERROR_PREFIX = "UniformHypercube cannot contain dataset";

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
            return "UniformHypercube";
        }

        @Override
        public Dataset getDataset(String id) {
            Tokenizer tok = new Tokenizer(id);
            tok.expectNext("uniform", ERROR_PREFIX);
            tok.expectNext("hypercube", ERROR_PREFIX);
            int n = tok.parseIntWithPrefix("n", ERROR_PREFIX);
            int d = tok.parseIntWithPrefix("d", ERROR_PREFIX);
            tok.expectFinish(ERROR_PREFIX);

            Random random = new Random(7243876 + 777236925662244L * id.hashCode());
            double[][][] rv = new double[instanceCount][n][d];
            for (int x = 0; x < instanceCount; ++x) {
                for (int i = 0; i < n; ++i) {
                    for (int j = 0; j < d; ++j) {
                        rv[x][i][j] = random.nextDouble();
                    }
                }
            }
            return new Dataset(id, rv);
        }
    };
}
