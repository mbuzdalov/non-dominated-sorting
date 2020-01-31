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
        addNonDominatedSortingFactory("deductive.original", DeductiveSort.getOriginalImplementation());
        addNonDominatedSortingFactory("deductive.library.v1", DeductiveSort.getLibraryImplementationV1());
        addNonDominatedSortingFactory("deductive.library.v2", DeductiveSort.getLibraryImplementationV2());

        for (boolean isMergeRecursive : new boolean[] { false, true }) {
            String mergeString = isMergeRecursive ? "recmerge" : "seqmerge";
            addNonDominatedSortingFactory(
                    "dominance.tree.nopresort." + mergeString,
                    DominanceTree.getNoPresortInsertion(isMergeRecursive)
            );
            for (boolean useDelayedInsertion: new boolean[] { false, true }) {
                String delayedString = useDelayedInsertion ? ".nodelayed" : ".delayed";
                addNonDominatedSortingFactory(
                        "dominance.tree.presort." + mergeString + delayedString,
                        DominanceTree.getPresortInsertion(isMergeRecursive, useDelayedInsertion)
                );
            }
        }

        addNonDominatedSortingFactory("ens.bs", ENS.getENS_BS());
        addNonDominatedSortingFactory("ens.ss", ENS.getENS_SS());

        StringTokenizer ndtThresholdsTok = new StringTokenizer(ndtThresholds, ",");
        int[] ndtThresholds = new int[ndtThresholdsTok.countTokens()];
        for (int i = 0; ndtThresholdsTok.hasMoreTokens(); ++i) {
            ndtThresholds[i] = Integer.parseInt(ndtThresholdsTok.nextToken());
        }
        for (int threshold : ndtThresholds) {
            addNonDominatedSortingFactory("ens.ndt." + threshold, ENS.getENS_NDT(threshold));
            addNonDominatedSortingFactory("ens.ndt.one.tree." + threshold, ENS.getENS_NDT_OneTree(threshold));
            addNonDominatedSortingFactory("jfb.rbtree.hybrid.ndt." + threshold, JensenFortinBuzdalov.getRedBlackTreeSweepHybridNDTImplementation(threshold, 1));
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
            for (int threshold : ndtThresholds) {
                addNonDominatedSortingFactory("jfb.rbtree.hybrid.ndt." + threshold + ".th" + threads, JensenFortinBuzdalov.getRedBlackTreeSweepHybridNDTImplementation(threshold, threads));
            }
        }
        addNonDominatedSortingFactory("jfb.veb", JensenFortinBuzdalov.getVanEmdeBoasImplementation());
        addNonDominatedSortingFactory("jfb.veb.hybrid.ens", JensenFortinBuzdalov.getVanEmdeBoasHybridENSImplementation());

        addNonDominatedSortingFactory("dcns.bs", SumitMishraDivideConquer.getDCNS_BS());
        addNonDominatedSortingFactory("dcns.ss", SumitMishraDivideConquer.getDCNS_SS());

        addNonDominatedSortingFactory("filter", FilterSort.getInstance());
        addNonDominatedSortingFactory("mnds.bitsets", SetIntersectionSort.getBitSetInstance());
    }
}
