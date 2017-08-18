package ru.ifmo.nds.rundb.generators;

import ru.ifmo.nds.rundb.Dataset;

import java.util.List;

public interface DatasetGenerator {
    String getName();
    List<String> getAllDatasetIds();
    Dataset getDataset(String id);
}
