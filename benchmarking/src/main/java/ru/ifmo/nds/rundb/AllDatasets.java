package ru.ifmo.nds.rundb;

import ru.ifmo.nds.JensenFortinBuzdalov;
import ru.ifmo.nds.NonDominatedSorting;
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

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        System.out.println("[info] " + allDatasets.size() + " datasets created");
        long tStart = System.nanoTime();
        try (NonDominatedSorting sorting = JensenFortinBuzdalov.getRedBlackTreeSweepHybridImplementation().getInstance(100000, 30)) {
            for (String id : getAllDatasetIds()) {
                Dataset ds = getDataset(id);
                long t0 = System.nanoTime();
                ds.runAlgorithm(sorting, ds.getNumberOfPoints());
                System.out.printf("%s => %.2g seconds%n", ds.getId(), (System.nanoTime() - t0) / 1e9);
            }
        }
        System.out.printf("[info] Total time: %.2g seconds%n", (System.nanoTime() - tStart) / 1e9);
    }
}
