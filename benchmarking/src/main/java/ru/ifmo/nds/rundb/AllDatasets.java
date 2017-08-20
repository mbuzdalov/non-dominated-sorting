package ru.ifmo.nds.rundb;

import ru.ifmo.nds.rundb.generators.DatasetGenerator;
import ru.ifmo.nds.rundb.generators.UniformHypercube;
import ru.ifmo.nds.rundb.generators.UniformHyperplanes;

import java.util.*;

public final class AllDatasets {
    private static final Map<String, DatasetGenerator> idToGenerator = new TreeMap<>();
    private static final List<String> allDatasets = new ArrayList<>();

    private static void addGenerator(DatasetGenerator generator) {
        List<String> ids = generator.getAllDatasetIds();
        for (String id : ids) {
            DatasetGenerator gen = idToGenerator.get(id);
            if (gen != null) {
                Dataset prev = gen.getDataset(id);
                Dataset curr = generator.getDataset(id);
                if (!prev.equals(curr)) {
                    throw new AssertionError("Dataset '" + id
                            + "' is generated in different ways by generators '" + gen.getName()
                            + "' and '" + generator.getName() + "'");
                }
            } else {
                allDatasets.add(id);
                idToGenerator.put(id, generator);
            }
        }
    }

    static {
        addGenerator(UniformHypercube.getInstance());
        addGenerator(UniformHyperplanes.getInstance());
    }

    public static List<String> getAllDatasetIds() {
        return Collections.unmodifiableList(allDatasets);
    }

    public static Dataset getDataset(String id) {
        DatasetGenerator generator = idToGenerator.get(id);
        if (generator == null) {
            throw new IllegalArgumentException("Dataset ID '" + id + "' is not known");
        } else {
            return generator.getDataset(id);
        }
    }
}
