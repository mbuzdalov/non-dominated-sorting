package ru.ifmo.nds;

import java.util.*;

public final class IdCollection {
    private static final String maxThreads = System.getProperty("nds.max.threads", "1");
    private static final String ndtThresholds = System.getProperty("nds.ndt.thresholds", "1,4");

    private static final Map<String, NonDominatedSortingFactory> allNDSFactories = new TreeMap<>();
    private static final Map<String, String> factoryNameToId = new HashMap<>();

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

    public static Set<String> getAllNonDominatedSortingIDs() {
        return allNDSFactories.keySet();
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

        StringTokenizer ndtThresholdsTok = new StringTokenizer(ndtThresholds, ",");
        while (ndtThresholdsTok.hasMoreTokens()) {
            int threshold = Integer.parseInt(ndtThresholdsTok.nextToken());
            addNonDominatedSortingFactory("ens.ndt." + threshold, ENS.getENS_NDT(threshold));
            addNonDominatedSortingFactory("ens.ndt.one.tree." + threshold, ENS.getENS_NDT_OneTree(threshold));
            addNonDominatedSortingFactory("jfb.rbtree.hybrid.ndt." + threshold, JensenFortinBuzdalov.getRedBlackTreeSweepHybridNDTImplementation(threshold));
            addNonDominatedSortingFactory("jfb.veb.hybrid.ndt." + threshold, JensenFortinBuzdalov.getVanEmdeBoasHybridNDTImplementation(threshold));
        }

        addNonDominatedSortingFactory("ens.ndt.arrays", ENS.getENS_NDT_Arrays());
        addNonDominatedSortingFactory("fnds.original", FastNonDominatedSorting.getOriginalVersion());
        addNonDominatedSortingFactory("fnds.linear", FastNonDominatedSorting.getLinearMemoryImplementation());
        addNonDominatedSortingFactory("jfb.fenwick", JensenFortinBuzdalov.getFenwickSweepImplementation(1));
        addNonDominatedSortingFactory("jfb.rbtree", JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(1));
        addNonDominatedSortingFactory("jfb.rbtree.hybrid.fnds", JensenFortinBuzdalov.getRedBlackTreeSweepHybridFNDSImplementation(1));
        addNonDominatedSortingFactory("jfb.rbtree.hybrid.ens", JensenFortinBuzdalov.getRedBlackTreeSweepHybridENSImplementation(1));

        int maxThreadsValue = Integer.parseInt(maxThreads);
        for (int threads = 2; threads <= maxThreadsValue; ++threads) {
            addNonDominatedSortingFactory("jfb.rbtree.th" + threads, JensenFortinBuzdalov.getRedBlackTreeSweepImplementation(threads));
            addNonDominatedSortingFactory("jfb.rbtree.hybrid.fnds.th" + threads, JensenFortinBuzdalov.getRedBlackTreeSweepHybridFNDSImplementation(threads));
            addNonDominatedSortingFactory("jfb.rbtree.hybrid.ens.th" + threads, JensenFortinBuzdalov.getRedBlackTreeSweepHybridENSImplementation(threads));
        }
        addNonDominatedSortingFactory("jfb.veb", JensenFortinBuzdalov.getVanEmdeBoasImplementation());
        addNonDominatedSortingFactory("jfb.veb.hybrid.ens", JensenFortinBuzdalov.getVanEmdeBoasHybridENSImplementation());

        addNonDominatedSortingFactory("dcns.bs", SumitMishraDivideConquer.getSumitImplementation(true, false));
        addNonDominatedSortingFactory("dcns.bss", SumitMishraDivideConquer.getSumitImplementation(true, true));
        addNonDominatedSortingFactory("dcns.ss", SumitMishraDivideConquer.getSumitImplementation(false, false));
        addNonDominatedSortingFactory("dcns.sss", SumitMishraDivideConquer.getSumitImplementation(false, true));
    }
}
