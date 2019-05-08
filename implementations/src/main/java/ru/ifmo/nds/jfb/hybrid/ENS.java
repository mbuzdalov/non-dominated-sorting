package ru.ifmo.nds.jfb.hybrid;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;
import ru.ifmo.nds.jfb.JFBBase;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class ENS extends HybridAlgorithmWrapper {
    private final int threshold3D;
    private final int thresholdAll;

    private static final double TUNING_MULTIPLE = 1.5;
    private static final double TUNING_MULTIPLE_FAIL = 1.0 / TUNING_MULTIPLE;
    private static final double TUNING_MULTIPLE_SUCCESS = Math.pow(TUNING_MULTIPLE, 0.01);

    private final boolean useTuning;

    public ENS(int threshold3D, int thresholdAll, boolean useTuning) {
        this.threshold3D = threshold3D;
        this.thresholdAll = thresholdAll;
        this.useTuning = useTuning;
    }

    public ENS(int threshold3D, int thresholdAll) {
        this(threshold3D, thresholdAll, false);
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public String getName() {
        if (useTuning) {
            return "ENS (dynamic thresholds, initial 3D = " + threshold3D + ", initial all = " + thresholdAll + ")";
        } else {
            return "ENS (threshold 3D = " + threshold3D + ", threshold all = " + thresholdAll + ")";
        }
    }

    @Override
    public HybridAlgorithmWrapper.Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints) {
        return new Instance(ranks, indices, points, threshold3D, thresholdAll, useTuning);
    }

    private static final ThreadLocal<OperationCounter> counters = ThreadLocal.withInitial(OperationCounter::new);

    private static class OperationCounter {
        private int remains = 0;

        // TODO: Get rid of this parameter and implement something which actually works.
        // For d = 4 the good value is 8.5.
        // For d = 5 the good value is 6.875.
        // For d = 10 the good value is 2.75.
        // Having it this way is not a good idea.
        private static final double CORRECTOR = 8.5;

        private void initialize(int problemSize) {
            // Notes on performance counting on Irene's laptop.
            // For helperB in ENS hybrid:
            //     for x operations, the time is roughly 7.95 x + 392 nanoseconds.
            // For helperB in divide-and-conquer:
            //     for n points, the time is estimated as exp(3.5 + 1.5 * log(n)) = 33.11 * n * sqrt(n) nanoseconds.
            // For n points, the number of operations that makes up the limit is:
            //     (33.11 * n * sqrt(n) - 392) / 7.95 = 4.165 * n * sqrt(n) - 49.3
            remains = (int) ((4.165 * problemSize * Math.sqrt(problemSize) - 49.3) / CORRECTOR);
        }

        private void consume(int nOperations) {
            remains -= nOperations;
        }

        private boolean shallTerminate() {
            return remains <= 0;
        }
    }

    private static class ThresholdAdaptor {
        private double threshold;
        private final double multipleFail;
        private final double multipleSuccess;

        ThresholdAdaptor(int initialThresholdValue, double multipleFail, double multipleSuccess) {
            this.threshold = initialThresholdValue;
            this.multipleFail = multipleFail;
            this.multipleSuccess = multipleSuccess;
        }

        private synchronized void algorithmFailed() {
            threshold *= multipleFail;
        }

        private synchronized void algorithmSucceeded() {
            threshold *= multipleSuccess;
        }
    }

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private static final int STORAGE_MULTIPLE = 5;

        private final int[] space;
        private final int[] ranks;
        private final int[] indices;
        private final double[][] points;
        private final double[][] exPoints;

        private final boolean useTuning;
        private final ThresholdAdaptor threshold3D;
        private final ThresholdAdaptor thresholdAll;

        private Instance(int[] ranks, int[] indices, double[][] points, int threshold3D, int thresholdAll,
                         boolean useTuning) {
            this.ranks = ranks;
            this.indices = indices;
            this.points = points;
            this.exPoints = new double[points.length][];
            this.space = new int[STORAGE_MULTIPLE * indices.length];
            this.useTuning = useTuning;
            this.threshold3D = new ThresholdAdaptor(threshold3D, useTuning ? TUNING_MULTIPLE_FAIL : 1, useTuning ? TUNING_MULTIPLE_SUCCESS : 1);
            this.thresholdAll = new ThresholdAdaptor(thresholdAll, useTuning ? TUNING_MULTIPLE_FAIL : 1, useTuning ? TUNING_MULTIPLE_SUCCESS : 1);
        }

        private boolean notHookCondition(int size, int obj) {
            switch (obj) {
                case 1:
                    return true;
                case 2:
                    return size >= threshold3D.threshold;
                default:
                    return size >= thresholdAll.threshold;
            }
        }

        private boolean checkIfDominatesA(int sliceIndex, int obj, int weakIndex) {
            int sliceRank = space[sliceIndex];
            if (ranks[weakIndex] > sliceRank) {
                return true;
            }
            int virtualGoodIndex = space[sliceIndex + 2];
            double[] wp = points[weakIndex];
            while (virtualGoodIndex != -1) {
                int realGoodIndex = space[virtualGoodIndex];
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[realGoodIndex], wp, obj)) {
                    ranks[weakIndex] = 1 + sliceRank;
                    return true;
                }
                virtualGoodIndex = space[virtualGoodIndex + 1];
            }
            return false;
        }

        private void initNewSliceA(int prevSlice, int currSlice, int nextSlice, int rank, int firstPointIndex) {
            space[currSlice] = rank;
            space[currSlice + 1] = nextSlice;
            space[currSlice + 2] = firstPointIndex;
            if (prevSlice != -1) {
                space[prevSlice + 1] = currSlice;
            }
        }

        @Override
        public int helperAHook(int from, int until, int obj, int maximalMeaningfulRank) {
            if (notHookCondition(until - from, obj)) {
                return -1;
            }

            int sliceOffset = from * STORAGE_MULTIPLE;
            int pointOffset = sliceOffset + 3 * (until - from);

            int sliceCurrent = sliceOffset - 3;
            int sliceFirst = -1;

            int minOverflow = until;
            for (int i = from, pointIndex = pointOffset; i < until; ++i) {
                int ii = indices[i];
                if (sliceFirst == -1 || checkIfDominatesA(sliceFirst, obj, ii)) {
                    if (ranks[ii] <= maximalMeaningfulRank) {
                        sliceCurrent += 3;
                        initNewSliceA(-1, sliceCurrent, sliceFirst, ranks[ii], pointIndex);
                        space[pointIndex] = ii;
                        space[pointIndex + 1] = -1;
                        sliceFirst = sliceCurrent;
                        pointIndex += 2;
                    } else if (minOverflow > i) {
                        minOverflow = i;
                    }
                } else {
                    int prevSlice = sliceFirst, nextSlice;
                    while ((nextSlice = space[prevSlice + 1]) != -1) {
                        if (checkIfDominatesA(nextSlice, obj, ii)) {
                            break;
                        }
                        prevSlice = nextSlice;
                    }
                    // prevSlice does not dominate, nextSlice already dominates
                    space[pointIndex] = ii;
                    int currRank = ranks[ii];
                    if (currRank == space[prevSlice]) {
                        // insert the current point into prevSlice
                        space[pointIndex + 1] = space[prevSlice + 2];
                        space[prevSlice + 2] = pointIndex;
                    } else {
                        sliceCurrent += 3;
                        // create a new slice and insert it between prevSlice and nextSlice
                        initNewSliceA(prevSlice, sliceCurrent, nextSlice, currRank, pointIndex);
                        space[pointIndex + 1] = -1;
                    }
                    pointIndex += 2;
                }
            }
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, until);
        }

        private boolean checkWhetherDominates(int goodFrom, int goodUntil, double[] wp, int obj,
                                              OperationCounter counter) {
            int curr = goodUntil;
            while (curr > goodFrom) {
                --curr;
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(exPoints[curr], wp, obj)) {
                    counter.consume(goodUntil - curr);
                    return true;
                }
            }
            counter.consume(goodUntil - curr);
            return false;
        }

        private int helperBSingleRank(int rank, int goodFrom, int goodUntil,
                                      int weakFrom, int weakUntil, int obj, int maximalMeaningfulRank, int tempFrom,
                                      ThresholdAdaptor adaptor, OperationCounter counter) {
            int minUpdated = weakUntil;
            int offset = tempFrom - goodFrom;
            for (int good = goodFrom; good < goodUntil; ++good) {
                exPoints[offset + good] = points[indices[good]];
            }
            for (int weak = weakFrom, good = goodFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                if (ranks[wi] > rank) {
                    continue;
                }
                good = ArrayHelper.findWhereNotSmaller(indices, good, goodUntil, wi);
                if (checkWhetherDominates(tempFrom, good + offset, points[wi], obj, counter)) {
                    ranks[wi] = rank + 1;
                    if (minUpdated > weak) {
                        minUpdated = weak;
                    }
                }
                if (useTuning && counter.shallTerminate()) {
                    adaptor.algorithmFailed();
                    Arrays.fill(exPoints, tempFrom, tempFrom + goodUntil - goodFrom, null);
                    return -1;
                }
            }
            adaptor.algorithmSucceeded();
            Arrays.fill(exPoints, tempFrom, tempFrom + goodUntil - goodFrom, null);
            return rank == maximalMeaningfulRank && minUpdated < weakUntil
                    ? JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minUpdated, weakUntil)
                    : weakUntil;
        }

        private int transplantRanksAndCheckWhetherAllAreSame(int goodFrom, int goodUntil, int ranksAndSlicesOffset, int sortedIndicesOffset) {
            int firstRank = -ranks[indices[goodFrom]];
            boolean allSame = true;
            space[ranksAndSlicesOffset] = firstRank;
            space[sortedIndicesOffset] = ranksAndSlicesOffset;
            for (int i = goodFrom + 1, ri = ranksAndSlicesOffset, si = sortedIndicesOffset; i < goodUntil; ++i) {
                ++ri;
                ++si;
                int rank = -ranks[indices[i]];
                allSame &= firstRank == rank;
                space[ri] = rank;
                space[si] = ri;
            }
            return allSame ? firstRank : 1;
        }

        private static int distributePointsBetweenSlices(int[] space, int from, int until, int sliceOffset, int pointsBySlicesOffset) {
            int sliceLast = sliceOffset - 2;
            int atSliceLast = 0;
            int prevRank = 1;
            int sliceRankIndex = from - 1;
            for (int i = from; i < until; ++i) {
                int currIndex = space[i];
                int currRank = space[currIndex];
                if (prevRank != currRank) {
                    prevRank = currRank;
                    if (sliceLast >= sliceOffset) {
                        space[sliceLast] = atSliceLast;
                        atSliceLast = 0;
                    }
                    space[++sliceRankIndex] = currRank;
                    sliceLast += 2;
                }
                ++atSliceLast;
                space[currIndex] = sliceLast;
            }
            space[sliceLast] = atSliceLast;
            for (int i = sliceOffset, collected = pointsBySlicesOffset; i <= sliceLast; i += 2) {
                int current = space[i];
                space[i] = collected;
                space[i + 1] = collected;
                collected += current;
            }
            return sliceLast;
        }

        private int findRankInSlices(int sliceOffset, int sliceLast, int wi, int obj, int sliceRankOffset,
                                     OperationCounter counter) {
            int currSlice = sliceLast;
            int sliceRankIndex = ((currSlice - sliceOffset) >>> 1) + sliceRankOffset;
            int weakRank = ranks[wi];
            double[] wp = points[wi];
            while (currSlice >= sliceOffset) {
                int from = space[currSlice];
                int until = space[currSlice + 1];
                if (from < until) {
                    int currRank = -space[sliceRankIndex];
                    if (currRank >= weakRank) {
                        if (checkWhetherDominates(from, until, wp, obj, counter)) {
                            weakRank = currRank + 1;
                        } else {
                            break;
                        }
                    }
                }
                currSlice -= 2;
                --sliceRankIndex;
            }
            return ranks[wi] = weakRank;
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank) {
            int goodSize = goodUntil - goodFrom;
            if (notHookCondition(goodSize + weakUntil - weakFrom, obj)) {
                return -1;
            }

            ThresholdAdaptor adaptor = obj == 2 ? threshold3D : thresholdAll;
            OperationCounter counter = counters.get();
            counter.initialize(goodSize + weakUntil - weakFrom);

            if (useTuning && counter.shallTerminate()) { // it can be like that
                adaptor.algorithmFailed();
                return -1;
            }

            int sortedIndicesOffset = tempFrom * STORAGE_MULTIPLE;
            int ranksAndSlicesOffset = sortedIndicesOffset + goodSize;
            int sliceOffset = ranksAndSlicesOffset + goodSize;

            int minRank = transplantRanksAndCheckWhetherAllAreSame(goodFrom, goodUntil, ranksAndSlicesOffset, sortedIndicesOffset);
            if (minRank != 1) {
                // "good" has a single front, let's do the simple stuff
                return helperBSingleRank(-minRank, goodFrom, goodUntil, weakFrom, weakUntil, obj, maximalMeaningfulRank,
                        tempFrom, adaptor, counter);
            } else {
                // "good" has multiple fronts (called "slices" here), need to go a more complicated way.
                ArraySorter.sortIndicesByValues(space, space, sortedIndicesOffset, sortedIndicesOffset + goodSize);
                int sliceLast = distributePointsBetweenSlices(space, sortedIndicesOffset, sortedIndicesOffset + goodSize, sliceOffset, tempFrom);
                int minOverflowed = weakUntil;
                for (int weak = weakFrom, good = goodFrom, sliceOfGood = ranksAndSlicesOffset; weak < weakUntil; ++weak) {
                    int wi = indices[weak];
                    int gi;
                    while (good < goodUntil && (gi = indices[good]) < wi) {
                        int sliceTailIndex = space[sliceOfGood] + 1;
                        int spaceAtTail = space[sliceTailIndex];
                        exPoints[spaceAtTail] = points[gi];
                        space[sliceTailIndex] = spaceAtTail + 1;
                        ++good;
                        ++sliceOfGood;
                    }
                    int weakRank = findRankInSlices(sliceOffset, sliceLast, wi, obj, sortedIndicesOffset, counter);
                    if (weakRank > maximalMeaningfulRank && minOverflowed > weak) {
                        minOverflowed = weak;
                    }
                    if (useTuning && counter.shallTerminate()) {
                        adaptor.algorithmFailed();
                        Arrays.fill(exPoints, tempFrom, tempFrom + goodUntil - goodFrom, null);
                        return -1;
                    }
                }
                adaptor.algorithmSucceeded();
                Arrays.fill(exPoints, tempFrom, tempFrom + goodUntil - goodFrom, null);
                return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflowed, weakUntil);
            }
        }
    }
}
