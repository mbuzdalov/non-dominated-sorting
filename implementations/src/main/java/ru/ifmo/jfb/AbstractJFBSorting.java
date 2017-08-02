package ru.ifmo.jfb;

import ru.ifmo.NonDominatedSorting;
import ru.ifmo.util.DoubleArraySorter;
import ru.ifmo.util.MedianFinder;
import ru.ifmo.util.RankQueryStructure;

import java.util.Arrays;

public abstract class AbstractJFBSorting extends NonDominatedSorting {
    // Pre-allocated
    int[] indices;
    int[] ranks;
    int maximalMeaningfulRank;

    private DoubleArraySorter sorter;
    private MedianFinder medianFinder;
    private RankQueryStructure rankQuery;
    private int[] internalIndices;
    private double[] lastFrontOrdinates;
    private int[] splitScratchL, splitScratchM, splitScratchR;
    private double[][] transposedPoints;

    // Various answers
    private int splitL;
    private int splitM;

    // Partially carrying input data
    private double[][] points;

    AbstractJFBSorting(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);

        sorter = new DoubleArraySorter(maximumPoints);
        medianFinder = new MedianFinder(maximumPoints);
        indices = new int[maximumPoints];
        ranks = new int[maximumPoints];
        points = new double[maximumPoints][];
        transposedPoints = new double[maximumDimension][maximumPoints];
        rankQuery = createStructure(maximumPoints);

        internalIndices = new int[maximumPoints];
        lastFrontOrdinates = new double[maximumPoints];
        splitScratchL = new int[maximumPoints];
        splitScratchM = new int[maximumPoints];
        splitScratchR = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() throws Exception {
        sorter = null;
        medianFinder = null;
        indices = null;
        ranks = null;
        points = null;
        rankQuery = null;
        transposedPoints = null;

        internalIndices = null;
        lastFrontOrdinates = null;
        splitScratchL = null;
        splitScratchM = null;
        splitScratchR = null;
    }

    @Override
    protected final void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        int dim = points[0].length;
        Arrays.fill(ranks, 0);
        for (int i = 0; i < n; ++i) {
            internalIndices[i] = i;
        }
        sorter.lexicographicalSort(points, internalIndices, 0, n, dim);

        this.maximalMeaningfulRank = maximalMeaningfulRank;

        if (dim == 1) {
            // 1: This is equivalent to ordinary sorting.
            for (int i = 0, r = 0; i < n; ++i) {
                ranks[internalIndices[i]] = r;
                if (i + 1 < n && points[internalIndices[i]][0] != points[internalIndices[i + 1]][0]) {
                    ++r;
                }
            }
        } else if (dim == 2) {
            // 2: Special case: binary search.
            twoDimensionalCase(points, ranks);
        } else {
            // 3: General case.
            // 3.1: Moving points in a sorted order to internal structures
            int newN = DoubleArraySorter.retainUniquePoints(points, internalIndices, this.points, ranks);
            for (int i = 0; i < newN; ++i) {
                this.indices[i] = i;
                this.ranks[i] = 0;
            }

            // 3.2: Transposing points. This should fit in cache for reasonable dimensions.
            for (int i = 0; i < newN; ++i) {
                for (int j = 0; j < dim; ++j) {
                    transposedPoints[j][i] = this.points[i][j];
                }
            }

            // 3.3: Calling the actual sorting
            helperA(0, newN, dim - 1);

            // 3.4: Applying the results back. After that, the argument "ranks" array stops being abused.
            for (int i = 0; i < n; ++i) {
                ranks[i] = this.ranks[ranks[i]];
                this.points[i] = null;
            }
        }
    }

    boolean strictlyDominatesAssumingNotSame(int goodIndex, int weakIndex, int maxObj) {
        double[] goodPoint = points[goodIndex];
        double[] weakPoint = points[weakIndex];
        // Comparison in 0 makes no sense, as due to goodIndex < weakIndex the points are <= in this coordinate.
        for (int i = maxObj; i > 0; --i) {
            if (goodPoint[i] > weakPoint[i]) {
                return false;
            }
        }
        return true;
    }

    private void tryUpdateRank(int goodIndex, int weakIndex) {
        if (ranks[weakIndex] <= ranks[goodIndex]) {
            ranks[weakIndex] = 1 + ranks[goodIndex];
        }
    }

    private void splitInTwo(int from, int until, double median, int obj, boolean equalToLeft) {
        int left = 0, right = 0;
        double[] local = transposedPoints[obj];
        for (int i = from; i < until; ++i) {
            double v = local[indices[i]];
            if (v < median || (equalToLeft && v == median)) {
                splitScratchL[left++] = indices[i];
            } else {
                splitScratchR[right++] = indices[i];
            }
        }
        System.arraycopy(splitScratchL, 0, indices, from, left);
        System.arraycopy(splitScratchR, 0, indices, from + left, right);
        splitL = left;
    }

    private void splitInThree(int from, int until, double median, int obj) {
        int l = 0, m = 0, r = 0;
        double[] local = transposedPoints[obj];
        for (int i = from; i < until; ++i) {
            double v = local[indices[i]];
            if (v < median) {
                splitScratchL[l++] = indices[i];
            } else if (v == median) {
                splitScratchM[m++] = indices[i];
            } else {
                splitScratchR[r++] = indices[i];
            }
        }
        System.arraycopy(splitScratchL, 0, indices, from, l);
        System.arraycopy(splitScratchM, 0, indices, from + l, m);
        System.arraycopy(splitScratchR, 0, indices, from + l + m, r);
        splitL = l;
        splitM = m;
    }

    private void mergeTwo(int from, int mid, int until) {
        int target = 0;
        int l = from, r = mid;
        while (l < mid && r < until) {
            if (indices[l] <= indices[r]) {
                splitScratchM[target++] = indices[l++];
            } else {
                splitScratchM[target++] = indices[r++];
            }
        }
        while (l < mid) {
            splitScratchM[target++] = indices[l++];
        }
        System.arraycopy(splitScratchM, 0, indices, from, target);
    }

    protected abstract RankQueryStructure createStructure(int maximumPoints);

    private void sweepA(int from, int until) {
        double[] local = transposedPoints[1];
        for (int i = from; i < until; ++i) {
            rankQuery.addPossibleKey(local[indices[i]]);
        }
        rankQuery.init();
        for (int i = from; i < until; ++i) {
            int curr = indices[i];
            double currY = local[curr];
            int result = Math.max(ranks[curr], rankQuery.getMaximumWithKeyAtMost(currY) + 1);
            ranks[curr] = Math.min(result, 1 + maximalMeaningfulRank);
            rankQuery.put(currY, result);
        }
        rankQuery.clear();
    }

    private void sweepB(int goodFrom, int goodUntil, int weakFrom, int weakUntil) {
        double[] local = transposedPoints[1];
        for (int i = goodFrom; i < goodUntil; ++i) {
            rankQuery.addPossibleKey(local[indices[i]]);
        }
        rankQuery.init();
        int goodI = goodFrom;
        for (int weakI = weakFrom; weakI < weakUntil; ++weakI) {
            int weakCurr = indices[weakI];
            while (goodI < goodUntil && indices[goodI] < weakCurr) {
                int goodCurr = indices[goodI++];
                rankQuery.put(local[goodCurr], ranks[goodCurr]);
            }
            ranks[weakCurr] = Math.max(ranks[weakCurr], rankQuery.getMaximumWithKeyAtMost(local[weakCurr]) + 1);
            ranks[weakCurr] = Math.min(ranks[weakCurr], 1 + maximalMeaningfulRank);
        }
        rankQuery.clear();
    }

    protected boolean helperAHookCondition(int size, int obj) {
        return false;
    }
    protected void helperAHook(int from, int until, int obj) {
        throw new UnsupportedOperationException("helperAHook not yet implemented");
    }

    private void helperA(int from, int until, int obj) {
        int n = until - from;
        if (n <= 2) {
            if (n == 2) {
                int goodIndex = indices[from];
                int weakIndex = indices[from + 1];
                if (strictlyDominatesAssumingNotSame(goodIndex, weakIndex, obj)) {
                    tryUpdateRank(goodIndex, weakIndex);
                }
            }
        } else if (obj == 1) {
            sweepA(from, until);
        } else if (helperAHookCondition(until - from, obj)) {
            helperAHook(from, until, obj);
        } else {
            medianFinder.resetMedian();
            medianFinder.consumeDataForMedian(transposedPoints[obj], indices, from, until);
            if (medianFinder.getLastMedianConsumptionMin() == medianFinder.getLastMedianConsumptionMax()) {
                helperA(from, until, obj - 1);
            } else {
                double median = medianFinder.findMedian();
                int smallerThanMedian = medianFinder.howManySmallerThanMedian();
                int largerThanMedian = medianFinder.howManyLargerThanMedian();
                int equalToMedian = n - smallerThanMedian - largerThanMedian;
                if (equalToMedian < n / 2) {
                    // Few enough median-valued points, use two-way splitting.
                    splitInTwo(from, until, median, obj, smallerThanMedian < largerThanMedian);
                    int middle = from + splitL;
                    helperA(from, middle, obj);
                    helperB(from, middle, middle, until, obj - 1);
                    helperA(middle, until, obj);
                    mergeTwo(from, middle, until);
                } else {
                    // Too many median-valued points, use three-way splitting.
                    splitInThree(from, until, median, obj);
                    int startMid = from + splitL;
                    int startRight = startMid + splitM;
                    helperA(from, startMid, obj);
                    helperB(from, startMid, startMid, startRight, obj - 1);
                    helperA(startMid, startRight, obj - 1);
                    mergeTwo(from, startMid, startRight);
                    helperB(from, startRight, startRight, until, obj - 1);
                    helperA(startRight, until, obj);
                    mergeTwo(from, startRight, until);
                }
            }
        }
    }

    private void helperBGood1(int good, int weakFrom, int weakUntil, int obj) {
        int gi = indices[good];
        // Binary search to discard points which are definitely not dominated.
        int bs = Arrays.binarySearch(indices, weakFrom, weakUntil, gi);
        int weakStart = -bs - 1;
        for (int i = weakStart; i < weakUntil; ++i) {
            int wi = indices[i];
            if (strictlyDominatesAssumingNotSame(gi, wi, obj)) {
                tryUpdateRank(gi, wi);
            }
        }
    }

    private void helperBWeak1(int goodFrom, int goodUntil, int weak, int obj) {
        int wi = indices[weak];
        // Binary search to discard points which definitely do not dominate.
        int bs = Arrays.binarySearch(indices, goodFrom, goodUntil, wi);
        int goodFinish = -bs - 1;
        for (int i = goodFrom; i < goodFinish; ++i) {
            int gi = indices[i];
            if (strictlyDominatesAssumingNotSame(gi, wi, obj)) {
                tryUpdateRank(gi, wi);
            }
        }
    }

    protected boolean helperBHookCondition(int goodSize, int weakSize, int obj) {
        return false;
    }
    protected void helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        throw new UnsupportedOperationException("helperBHook not yet implemented");
    }

    private void helperB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        int goodN = goodUntil - goodFrom;
        int weakN = weakUntil - weakFrom;
        if (goodN > 0 && weakN > 0) {
            if (goodN == 1) {
                helperBGood1(goodFrom, weakFrom, weakUntil, obj);
            } else if (weakN == 1) {
                helperBWeak1(goodFrom, goodUntil, weakFrom, obj);
            } else if (obj == 1) {
                sweepB(goodFrom, goodUntil, weakFrom, weakUntil);
            } else if (helperBHookCondition(goodUntil - goodFrom, weakUntil - weakFrom, obj)) {
                helperBHook(goodFrom, goodUntil, weakFrom, weakUntil, obj);
            } else {
                medianFinder.resetMedian();
                medianFinder.consumeDataForMedian(transposedPoints[obj], indices, goodFrom, goodUntil);
                double goodMaxObj = medianFinder.getLastMedianConsumptionMax();
                medianFinder.consumeDataForMedian(transposedPoints[obj], indices, weakFrom, weakUntil);
                double weakMinObj = medianFinder.getLastMedianConsumptionMin();
                if (goodMaxObj <= weakMinObj) {
                    helperB(goodFrom, goodUntil, weakFrom, weakUntil, obj - 1);
                } else {
                    double median = medianFinder.findMedian();
                    int totalSmallerThanMedian = medianFinder.howManySmallerThanMedian();
                    int totalLargerThanMedian = medianFinder.howManyLargerThanMedian();
                    int totalEqualToMedian = (goodN + weakN) - totalLargerThanMedian - totalSmallerThanMedian;
                    if (totalEqualToMedian < (goodN + weakN) / 2) {
                        // Few enough median-valued points, use two-way splitting.
                        boolean leanLeft = totalSmallerThanMedian < totalLargerThanMedian;
                        splitInTwo(goodFrom, goodUntil, median, obj, leanLeft);
                        int goodMid = goodFrom + splitL;
                        splitInTwo(weakFrom, weakUntil, median, obj, leanLeft);
                        int weakMid = weakFrom + splitL;

                        helperB(goodFrom, goodMid, weakFrom, weakMid, obj);
                        helperB(goodFrom, goodMid, weakMid, weakUntil, obj - 1);
                        helperB(goodMid, goodUntil, weakMid, weakUntil, obj);
                        mergeTwo(goodFrom, goodMid, goodUntil);
                        mergeTwo(weakFrom, weakMid, weakUntil);
                    } else {
                        // Too many median-valued points, use three-way splitting.
                        splitInThree(goodFrom, goodUntil, median, obj);
                        int goodMidL = goodFrom + splitL;
                        int goodMidR = goodMidL + splitM;
                        splitInThree(weakFrom, weakUntil, median, obj);
                        int weakMidL = weakFrom + splitL;
                        int weakMidR = weakMidL + splitM;

                        helperB(goodFrom, goodMidL, weakFrom, weakMidL, obj);
                        helperB(goodMidR, goodUntil, weakMidR, weakUntil, obj);
                        mergeTwo(goodFrom, goodMidL, goodMidR);
                        mergeTwo(weakMidL, weakMidR, weakUntil);
                        helperB(goodFrom, goodMidR, weakMidL, weakUntil, obj - 1);
                        mergeTwo(goodFrom, goodMidR, goodUntil);
                        mergeTwo(weakFrom, weakMidL, weakUntil);
                    }
                }
            }
        }
    }

    private void twoDimensionalCase(double[][] points, int[] ranks) {
        // Also uses internalIndices and lastFrontOrdinates
        int maxRank = 1;
        int n = ranks.length;

        int lastII = internalIndices[0];
        double lastX = points[lastII][0];
        double lastY = points[lastII][1];
        double minY = lastY;

        // Point 0 always has rank 0.
        lastFrontOrdinates[0] = lastY;

        for (int i = 1; i < n; ++i) {
            int ii = internalIndices[i];
            double cx = points[ii][0];
            double cy = points[ii][1];

            if (cx == lastX && cy == lastY) {
                // Same point as the previous one.
                // The rank is the same as well.
                ranks[ii] = ranks[lastII];
            } else if (cy < minY) {
                // Y smaller than the smallest Y previously seen.
                // The rank is thus zero.
                minY = cy;
            } else {
                // At least the Y-smallest point dominates our point.
                int left, right;
                if (cy < lastY) {
                    // We are better than the previous point in Y.
                    // This means that we are at least that good.
                    left = 0;
                    right = ranks[lastII];
                } else {
                    // We are worse (or equal) than the previous point in Y.
                    // This means that we are worse than this point.
                    left = ranks[lastII];
                    right = maxRank;
                }
                // Running the binary search.
                while (right - left > 1) {
                    int mid = (left + right) >>> 1;
                    double midY = lastFrontOrdinates[mid];
                    if (cy < midY) {
                        right = mid;
                    } else {
                        left = mid;
                    }
                }
                // "right" is now our rank.
                ranks[ii] = right;
                lastFrontOrdinates[right] = cy;
                if (right == maxRank && maxRank <= maximalMeaningfulRank) {
                    ++maxRank;
                }
            }

            lastII = ii;
            lastX = cx;
            lastY = cy;
        }
    }
}
