package ru.ifmo.nds;

import java.util.*;

import ru.ifmo.nds.rundb.Dataset;
import ru.ifmo.nds.rundb.generators.DatasetGenerator;
import ru.ifmo.nds.rundb.generators.UniformCorrelated;
import ru.ifmo.nds.rundb.generators.UniformHypercube;
import ru.ifmo.nds.rundb.generators.UniformHyperplanes;

public final class IdCollection {
    private static final Map<String, NonDominatedSortingFactory> allNDSFactories = new TreeMap<>();
    private static final Map<String, String> factoryNameToId = new HashMap<>();
    private static final Map<String, DatasetGenerator> idToDatasetGenerator = new TreeMap<>();
    private static final List<String> allDatasets = new ArrayList<>();

    private static void addGenerator(DatasetGenerator generator) {
        List<String> ids = generator.getAllDatasetIds();
        for (String id : ids) {
            DatasetGenerator gen = idToDatasetGenerator.get(id);
            if (gen != null) {
                Dataset prev = gen.getDataset(id);
                Dataset curr = generator.getDataset(id);
                if (!prev.equals(curr)) {
                    throw new IllegalArgumentException("Dataset '" + id
                            + "' is generated in different ways by generators '" + gen.getName()
                            + "' and '" + generator.getName() + "'");
                }
            } else {
                allDatasets.add(id);
                idToDatasetGenerator.put(id, generator);
            }
        }
    }

    public static List<String> getAllDatasetIds() {
        return Collections.unmodifiableList(allDatasets);
    }

    public static Dataset getDataset(String id) {
        DatasetGenerator generator = idToDatasetGenerator.get(id);
        if (generator == null) {
            throw new IllegalArgumentException("Dataset ID '" + id + "' is not known");
        } else {
            return generator.getDataset(id);
        }
    }

    static {
        addGenerator(UniformHypercube.getInstance());
        addGenerator(UniformHyperplanes.getInstance());
        addGenerator(UniformCorrelated.getInstance());
    }

    private static void addNonDominatedSortingFactory(String id, NonDominatedSortingFactory factory) {
        String currName = factory.getName();
        NonDominatedSortingFactory prev = allNDSFactories.get(id);
        if (prev != null) {
            String prevName = prev.getName();
            if (!prevName.equals(currName)) {
                throw new IllegalArgumentException("The factory '" + currName + "' is added under ID '" + id
                        + "', under which the factory '" + prevName + "' has been added");
            }
        }
        String currID = factoryNameToId.get(currName);
        if (currID != null) {
            if (!currID.equals(id)) {
                throw new IllegalArgumentException("The factory '" + currName + "' is added under ID '" + id
                        + "', but it was previously added under different ID '" + currID + "'");
            }
        }
        factoryNameToId.put(currName, id);
        allNDSFactories.put(id, factory);
    }

    public static List<String> getAllNonDominatedSortingFactoryIds() {
        return new ArrayList<>(allNDSFactories.keySet());
    }

    public static List<NonDominatedSortingFactory> getAllNonDominatedSortingFactories() {
        return new ArrayList<>(allNDSFactories.values());
    }

    public static NonDominatedSortingFactory getNonDominatedSortingFactory(String id) {
        NonDominatedSortingFactory factory = allNDSFactories.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Non-dominated sorting factory ID '" + id + "' is not known");
        }
        return factory;
    }

    static {
        addNonDominatedSortingFactory("bos.proteek", BestOrderSort.getProteekImplementation());
        addNonDominatedSortingFactory("bos.improved", BestOrderSort.getImprovedImplementation());
        addNonDominatedSortingFactory("corner", CornerSort.getInstance());
        addNonDominatedSortingFactory("deductive", DeductiveSort.getInstance());

        for (boolean isMergeRecursive : new boolean[] { false, true }) {
            String mergeString = isMergeRecursive ? "recmerge" : "seqmerge";
            addNonDominatedSortingFactory(
                    "dominance.tree.nopresort." + mergeString,
                    DominanceTree.getNoPresortInsertion(isMergeRecursive)
            );
            addNonDominatedSortingFactory(
                    "dominance.tree.presort." + mergeString + ".nodelayed",
                    DominanceTree.getPresortInsertion(isMergeRecursive, DominanceTree.InsertionOption.NO_DELAYED_INSERTION)
            );
            addNonDominatedSortingFactory(
                    "dominance.tree.presort." + mergeString + ".delayed.recconcat",
                    DominanceTree.getPresortInsertion(isMergeRecursive, DominanceTree.InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION)
            );
            addNonDominatedSortingFactory(
                    "dominance.tree.presort." + mergeString + ".delayed.seqconcat",
                    DominanceTree.getPresortInsertion(isMergeRecursive, DominanceTree.InsertionOption.DELAYED_INSERTION_SEQUENTIAL_CONCATENATION)
            );
        }

        addNonDominatedSortingFactory("ens.bs", ENS.getENS_BS());
        addNonDominatedSortingFactory("ens.hs", ENS.getENS_HS());
        addNonDominatedSortingFactory("ens.ss", ENS.getENS_SS());
        for (int threshold : new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50, 100, 200}) {
            addNonDominatedSortingFactory("ens.ndt." + threshold, ENS.getENS_NDT(threshold));
        }
        addNonDominatedSortingFactory("ens.ndt.one.tree.1", ENS.getENS_NDT_OneTree(1));
        addNonDominatedSortingFactory("ens.ndt.arrays", ENS.getENS_NDT_Arrays());
        addNonDominatedSortingFactory("fnds.original", FastNonDominatedSorting.getOriginalVersion());
        addNonDominatedSortingFactory("fnds.linear", FastNonDominatedSorting.getLinearMemoryImplementation());
        addNonDominatedSortingFactory("jfb.fenwick", JensenFortinBuzdalov.getFenwickSweepImplementation(1));
        addNonDominatedSortingFactory("jfb.rbtree", JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(1));
        addNonDominatedSortingFactory("jfb.rbtree.hybrid.fnds", JensenFortinBuzdalov.getRedBlackTreeSweepHybridFNDSImplementation(1));
        addNonDominatedSortingFactory("jfb.rbtree.hybrid.ens", JensenFortinBuzdalov.getRedBlackTreeSweepHybridENSImplementation(1));
        for (int threads = 2; threads <= 8; ++threads) {
            addNonDominatedSortingFactory("jfb.rbtree.th" + threads, JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(threads));
            addNonDominatedSortingFactory("jfb.rbtree.hybrid.fnds.th" + threads, JensenFortinBuzdalov.getRedBlackTreeSweepHybridFNDSImplementation(threads));
            addNonDominatedSortingFactory("jfb.rbtree.hybrid.ens.th" + threads, JensenFortinBuzdalov.getRedBlackTreeSweepHybridENSImplementation(threads));
        }
        addNonDominatedSortingFactory("jfb.rbtree.thmax", JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(-1));
        addNonDominatedSortingFactory("jfb.rbtree.hybrid.fnds.thmax", JensenFortinBuzdalov.getRedBlackTreeSweepHybridFNDSImplementation(-1));
        addNonDominatedSortingFactory("jfb.rbtree.hybrid.ens.thmax", JensenFortinBuzdalov.getRedBlackTreeSweepHybridENSImplementation(-1));
        addNonDominatedSortingFactory("dcns.bs", SumitMishraDivideConquer.getSumitImplementation2016(true, false));
        addNonDominatedSortingFactory("dcns.bss", SumitMishraDivideConquer.getSumitImplementation2016(true, true));
        addNonDominatedSortingFactory("dcns.ss", SumitMishraDivideConquer.getSumitImplementation2016(false, false));
        addNonDominatedSortingFactory("dcns.sss", SumitMishraDivideConquer.getSumitImplementation2016(false, true));
    }
}
