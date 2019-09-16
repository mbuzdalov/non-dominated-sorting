package ru.ifmo.nds.jfb.hybrid;

import java.util.Arrays;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;
import ru.ifmo.nds.jfb.JFBBase;
import ru.ifmo.nds.jfb.hybrid.tuning.Threshold;
import ru.ifmo.nds.jfb.hybrid.tuning.ThresholdFactory;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;
import ru.ifmo.nds.util.DominanceHelper;

public final class ENS extends HybridAlgorithmWrapper {
    private final ThresholdFactory threshold3D;
    private final ThresholdFactory thresholdAll;

    public ENS(ThresholdFactory threshold3D, ThresholdFactory thresholdAll) {
        this.threshold3D = threshold3D;
        this.thresholdAll = thresholdAll;
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public String getName() {
        return "ENS (threshold 3D = " + threshold3D.getDescription()
                + ", threshold all = " + thresholdAll.getDescription() + ")";
    }

    @Override
    public HybridAlgorithmWrapper.Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints) {
        return new Instance(ranks, indices, points, transposedPoints.length, threshold3D, thresholdAll);
    }

    private static final int MAX_THRESHOLD_INDEX = 7;

    private static final double[] A_IN_OPS_GEN = {
            1.8, // for d = 2.
            1.8,
            0.9,
            0.525,
            0.36,
            0.3,
            0.3,
            0.21,
    };

    private static final double[] A_IN_OPS_HA = {
            4.0, // for d = 2.
            2.45,
            1.7,
            1.2,
            0.95,
            0.88,
            0.88,
            0.87,
    };

    private static final double[] A_IN_OPS_FLAT = {
            3.0835217371949715, // for d = 2
            1.8511168247769056,
            1.110213301509251,
            0.6927596598471091,
            0.4372428030953217,
            0.38865134020812175,
            0.3758161777491492,
            0.40189790442448237, // WTF? but it works this way...
    };

    private static final double[] P_IN_OPS = {
            1.015332333045967268, // for d = 2
            1.14316676164960723,
            1.26411624362740815,
            1.3564856546604639,
            1.4162172410288698,
            1.4382815729645708,
            1.4428886704739746,
            1.44701314145948956,
    };

    private static int computeBudgetGen(int problemSize, int objective) {
        objective = Math.min(objective - 2, MAX_THRESHOLD_INDEX);
        return (int) (A_IN_OPS_GEN[objective] * Math.pow(problemSize, P_IN_OPS[objective]) * Math.log(1 + problemSize));
    }

    private static int computeBudgetHelperA(int problemSize, int objective) {
        objective = Math.min(objective - 2, MAX_THRESHOLD_INDEX);
        return (int) (A_IN_OPS_HA[objective] * Math.pow(problemSize, P_IN_OPS[objective]) * Math.log(1 + problemSize));
    }

    private static int computeBudgetFlat(int problemSize, int objective) {
        objective = Math.min(objective - 2, MAX_THRESHOLD_INDEX);
        return (int) (A_IN_OPS_FLAT[objective] * Math.pow(problemSize, P_IN_OPS[objective]) * Math.log(1 + problemSize));
    }

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private static final int STORAGE_MULTIPLE = 5;

        private final int[] space;
        private final int[] ranks;
        private final int[] indices;
        private final double[][] points;
        private final double[][] exPoints;

        private final Threshold[] thresholdsGen;
        private final Threshold[] thresholdsFlat;
        private final Threshold[] thresholdsHelperA;

        private Instance(int[] ranks, int[] indices, double[][] points, int dimension,
                         ThresholdFactory threshold3D, ThresholdFactory thresholdAll) {
            this.ranks = ranks;
            this.indices = indices;
            this.points = points;
            this.exPoints = new double[points.length][];
            this.space = new int[STORAGE_MULTIPLE * indices.length];
            thresholdsGen = new Threshold[dimension];
            thresholdsFlat = new Threshold[dimension];
            thresholdsHelperA = new Threshold[dimension];
            for (int i = 0; i < dimension; ++i) {
                ThresholdFactory f = i == 0 ? threshold3D : thresholdAll;
                thresholdsGen[i] = f.createThreshold();
                thresholdsFlat[i] = f.createThreshold();
                thresholdsHelperA[i] = f.createThreshold();
            }
        }

        private int checkIfDominatesA(int sliceIndex, int obj, int weakIndex) {
            int sliceRank = space[sliceIndex];
            if (ranks[weakIndex] > sliceRank) {
                return 1;
            }
            int virtualGoodIndex = space[sliceIndex + 2];
            double[] wp = points[weakIndex];
            int perfCount = 1;
            while (virtualGoodIndex != -1) {
                int realGoodIndex = space[virtualGoodIndex];
                ++perfCount;
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[realGoodIndex], wp, obj)) {
                    ranks[weakIndex] = 1 + sliceRank;
                    return perfCount;
                }
                virtualGoodIndex = space[virtualGoodIndex + 1];
            }
            return -perfCount;
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
            if (obj == 1) {
                return -from - 1;
            }
            Threshold threshold = thresholdsHelperA[obj - 2];
            int problemSize = until - from;
            if (problemSize >= threshold.getThreshold()) {
                return -from - 1;
            }

            int counter = 0;
            int budget = computeBudgetHelperA(problemSize, obj);

            int sliceOffset = from * STORAGE_MULTIPLE;
            int pointOffset = sliceOffset + 3 * problemSize;

            int sliceCurrent = sliceOffset - 3;
            int sliceFirst = -1;

            int minOverflow = until;
            for (int i = from, pointIndex = pointOffset; i < until; ++i) {
                int ii = indices[i];
                int domResult0 = 0;
                if (sliceFirst == -1 || (domResult0 = checkIfDominatesA(sliceFirst, obj, ii)) > 0) {
                    counter += domResult0;
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
                    counter -= domResult0;
                    int prevSlice = sliceFirst, nextSlice;
                    while ((nextSlice = space[prevSlice + 1]) != -1) {
                        int domResult1 = checkIfDominatesA(nextSlice, obj, ii);
                        counter += Math.abs(domResult1);
                        if (domResult1 > 0) {
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
                if (threshold.shallTerminate(budget, counter)) {
                    threshold.recordPerformance(problemSize, budget, counter, true);
                    return -i - 2;
                }
            }
            threshold.recordPerformance(problemSize, budget, counter, false);
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, until);
        }

        private int checkWhetherDominates(int goodFrom, int goodUntil, double[] wp, int obj) {
            int curr = goodUntil;
            while (curr > goodFrom) {
                --curr;
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(exPoints[curr], wp, obj)) {
                    return goodUntil - curr;
                }
            }
            return curr - goodUntil;
        }

        private int helperBSingleRank(int rank, int goodFrom, int goodUntil,
                                      int weakFrom, int weakUntil, int obj, int maximalMeaningfulRank, int tempFrom,
                                      Threshold threshold) {
            int minUpdated = weakUntil;
            int offset = tempFrom - goodFrom;
            int goodSize = goodUntil - goodFrom;
            int problemSize = goodSize + weakUntil - weakFrom;

            int counter = 0;
            int budget = computeBudgetFlat(problemSize, obj);

            for (int good = goodFrom; good < goodUntil; ++good) {
                exPoints[offset + good] = points[indices[good]];
            }

            for (int weak = weakFrom, good = goodFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                if (ranks[wi] > rank) {
                    continue;
                }
                good = ArrayHelper.findWhereNotSmaller(indices, good, goodUntil, wi);
                int domCheckResult = checkWhetherDominates(tempFrom, good + offset, points[wi], obj);
                counter += Math.abs(domCheckResult);
                if (domCheckResult > 0) {
                    ranks[wi] = rank + 1;
                    if (minUpdated > weak) {
                        minUpdated = weak;
                    }
                }
                if (threshold.shallTerminate(budget, counter)) {
                    threshold.recordPerformance(problemSize, budget, counter, true);
                    Arrays.fill(exPoints, tempFrom, tempFrom + goodSize, null);
                    return -weak - 2;
                }
            }
            threshold.recordPerformance(problemSize, budget, counter, false);
            Arrays.fill(exPoints, tempFrom, tempFrom + goodSize, null);
            return rank == maximalMeaningfulRank && minUpdated < weakUntil
                    ? JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minUpdated, weakUntil)
                    : weakUntil;
        }

        private int getRankIfAllSame(int goodFrom, int goodUntil) {
            int first = ranks[indices[goodFrom]];
            while (++goodFrom != goodUntil) {
                if (first != ranks[indices[goodFrom]]) {
                    return -1;
                }
            }
            return first;
        }

        private void transplantRanks(int goodFrom, int goodUntil, int ranksAndSlicesOffset, int sortedIndicesOffset) {
            for (int i = goodFrom, ri = ranksAndSlicesOffset, si = sortedIndicesOffset; i < goodUntil; ++i, ++ri, ++si) {
                space[ri] = -ranks[indices[i]];
                space[si] = ri;
            }
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

        private int helperBMultipleRanks(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj,
                                         int maximalMeaningfulRank, int tempFrom, Threshold threshold) {
            int goodSize = goodUntil - goodFrom;
            int problemSize = goodSize + weakUntil - weakFrom;
            int sortedIndicesOffset = tempFrom * STORAGE_MULTIPLE;
            int ranksAndSlicesOffset = sortedIndicesOffset + goodSize;
            int sliceOffset = ranksAndSlicesOffset + goodSize;
            transplantRanks(goodFrom, goodUntil, ranksAndSlicesOffset, sortedIndicesOffset);
            ArraySorter.sortIndicesByValues(space, space, sortedIndicesOffset, sortedIndicesOffset + goodSize);
            int sliceLast = distributePointsBetweenSlices(space, sortedIndicesOffset, sortedIndicesOffset + goodSize, sliceOffset, tempFrom);
            int minOverflowed = weakUntil;

            int counter = 0;
            int budget = computeBudgetGen(problemSize, obj);

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
                int currSlice = sliceLast;
                int sliceRankIndex = ((currSlice - sliceOffset) >>> 1) + sortedIndicesOffset;
                int weakRank = ranks[wi];
                double[] wp = points[wi];
                while (currSlice >= sliceOffset) {
                    int from = space[currSlice];
                    int until = space[currSlice + 1];
                    if (from < until) {
                        int currRank = -space[sliceRankIndex];
                        if (currRank >= weakRank) {
                            int domCheckResult = checkWhetherDominates(from, until, wp, obj);
                            counter += Math.abs(domCheckResult);
                            if (domCheckResult > 0) {
                                weakRank = currRank + 1;
                            } else {
                                break;
                            }
                        }
                    }
                    currSlice -= 2;
                    --sliceRankIndex;
                }
                ranks[wi] = weakRank;
                if (weakRank > maximalMeaningfulRank && minOverflowed > weak) {
                    minOverflowed = weak;
                }
                if (threshold.shallTerminate(budget, counter)) {
                    threshold.recordPerformance(problemSize, budget, counter, true);
                    Arrays.fill(exPoints, tempFrom, tempFrom + goodSize, null);
                    return -weak - 2;
                }
            }
            threshold.recordPerformance(problemSize, budget, counter, false);
            Arrays.fill(exPoints, tempFrom, tempFrom + goodSize, null);
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflowed, weakUntil);
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank) {
            if (obj == 1) return -weakFrom - 1;

            int problemSize = goodUntil - goodFrom + weakUntil - weakFrom;
            int objIndex = obj - 2;
            Threshold thresholdGen = thresholdsGen[objIndex];
            Threshold thresholdFlat = thresholdsFlat[objIndex];
            int genValue = thresholdGen.getThreshold();
            int flatValue = thresholdFlat.getThreshold();

            if (problemSize >= genValue && problemSize >= flatValue) {
                return -weakFrom - 1;
            }

            int theOnlyRank = getRankIfAllSame(goodFrom, goodUntil);
            if (theOnlyRank >= 0) {
                if (problemSize >= flatValue) {
                    return -weakFrom - 1;
                }
                // "good" has a single front, let's do the simple stuff
                return helperBSingleRank(theOnlyRank, goodFrom, goodUntil, weakFrom, weakUntil, obj, maximalMeaningfulRank, tempFrom, thresholdFlat);
            } else {
                if (problemSize >= genValue) {
                    return -weakFrom - 1;
                }
                // "good" has multiple fronts (called "slices" here), need to go a more complicated way.
                return helperBMultipleRanks(goodFrom, goodUntil, weakFrom, weakUntil, obj, maximalMeaningfulRank, tempFrom, thresholdGen);
            }
        }
    }
}
