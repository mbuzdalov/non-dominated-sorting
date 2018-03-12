package ru.ifmo.nds.rundb.generators;

import java.util.*;

import ru.ifmo.nds.rundb.Dataset;

public class UniformCorrelated {
    private UniformCorrelated() {}

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
    private static final int instanceCount = 10;
    private static final List<String> ids = new ArrayList<>(
            supportedDimensions.length * supportedSizes.length * instanceCount
    );
    static {
        for (int size : supportedSizes) {
            for (int dim : supportedDimensions) {
                for (int x = 0; x < dim; ++x) {
                    ids.add("uniform.correlated.n" + size + ".d" + dim + ".diff" + x);
                }
            }
        }
    }

    private static final String ERROR_PREFIX = "UniformCorrelated cannot contain dataset";

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
            return "UniformCorrelated";
        }

        @Override
        public Dataset getDataset(String id) {
            Tokenizer tok = new Tokenizer(id);
            tok.expectNext("uniform", ERROR_PREFIX);
            tok.expectNext("correlated", ERROR_PREFIX);
            int n = tok.parseIntWithPrefix("n", ERROR_PREFIX);
            int d = tok.parseIntWithPrefix("d", ERROR_PREFIX);
            int x = tok.parseIntWithPrefix("diff", ERROR_PREFIX);
            tok.expectFinish(ERROR_PREFIX);

            Random random = new Random(88325 + 77723692566L * id.hashCode());

            double[][][] rv = new double[instanceCount][n][d];
            for (int j = 0; j < instanceCount; ++j) {
                for (int i = 0; i < n; ++i) {
                    double first = random.nextDouble();
                    for (int k = 0; k < d; ++k) {
                        rv[j][i][k] = k == x ? -first : first;
                    }
                }
                Collections.shuffle(Arrays.asList(rv[j]), random);
            }
            return new Dataset(id, rv);
        }
    };
}
