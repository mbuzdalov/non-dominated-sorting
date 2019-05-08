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
        private int baseline = 0;

        private static final double[] A_IN_OPS = {
                3.806816959499965, // for d = 2
                4.113592943948679,
                2.8467007731006437,
                1.8473590929256243,
                1.3249781911979446,
                1.1777313339640052,
                1.1653214813927109,
                1.1401358990765458
        };

        private static final double[] B_IN_OPS = {
                -186.48883310027958, // for d = 2
                -484.71257133393243,
                -584.9885133808805,
                -423.9142423658353,
                -178.74936469020457,
                -46.92822531334395,
                -38.59458963435686,
                -20.611906174873603
        };

        private static final double[] P_IN_OPS = {
                0.015332333045967268, // for d = 2
                0.14316676164960723,
                0.26411624362740815,
                0.3564856546604639,
                0.4162172410288698,
                0.4382815729645708,
                0.4428886704739746,
                0.44701314145948956
        };

        private void initialize(int problemSize, int objective) {
            // Notes on performance counting on some fixed laptop.
            // For helperB in ENS hybrid:
            //     for x operations, the time is roughly 13 x + 2000 nanoseconds.
            // For helperB in divide-and-conquer:
            //     for n points and objective d, the time is estimated, in nanoseconds, as
            //        b_d + a_d * n * pow(n, p_d)
            //     where:
            //        d = 2:  a_2 = 49.488620473499545, b_2 =  -424.3548303036347, p_2 = 0.015332333045967268
            //        d = 3:  a_3 = 53.476708271332825, b_3 = -4301.263427341121,  p_3 = 0.14316676164960723
            //        d = 4:  a_4 = 37.00711005030837,  b_4 = -5604.850673951447,  p_4 = 0.26411624362740815
            //        d = 5:  a_5 = 24.015668208033116, b_5 = -3510.8851507558597, p_5 = 0.3564856546604639
            //        d = 6:  a_6 = 17.22471648557328,  b_6 =  -323.7417409726593, p_6 = 0.4162172410288698
            //        d = 7:  a_7 = 15.310507341532068, b_7 =  1389.9330709265287, p_7 = 0.4382815729645708
            //        d = 8:  a_8 = 15.14917925810524,  b_8 =  1498.2703347533609, p_8 = 0.4428886704739746
            //        d = 9+: a_9 = 14.821766687995096, b_9 =  1732.0452197266432, p_9 = 0.44701314145948956
            // Hence the arrays above have been computed.

            objective = Math.min(objective - 2, 7);
            remains = (int) ((B_IN_OPS[objective] + A_IN_OPS[objective] * problemSize * Math.pow(problemSize, P_IN_OPS[objective]) * 2));
            baseline = -remains;
        }

        private void consume(int nOperations) {
            remains -= nOperations;
        }

        private boolean considerFailed() { return remains <= 0; }

        private boolean shallTerminate() {
            return remains <= baseline;
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
            if (useTuning && counter.considerFailed()) {
                adaptor.algorithmFailed();
            } else {
                adaptor.algorithmSucceeded();
            }
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
            counter.initialize(goodSize + weakUntil - weakFrom, obj);

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
                if (useTuning && counter.considerFailed()) {
                    adaptor.algorithmFailed();
                } else {
                    adaptor.algorithmSucceeded();
                }
                Arrays.fill(exPoints, tempFrom, tempFrom + goodUntil - goodFrom, null);
                return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflowed, weakUntil);
            }
        }
    }
}
