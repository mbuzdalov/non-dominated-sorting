package ru.ifmo.jfb;

import ru.ifmo.NonDominatedSorting;
import ru.ifmo.util.DoubleArraySorter;
import ru.ifmo.util.RankQueryStructure;

import java.util.Arrays;

public abstract class AbstractJFBSorting extends NonDominatedSorting {
    // Pre-allocated
    private int[] indices;
    private int[] ranks;
    private DoubleArraySorter sorter;
    private RankQueryStructure rankQuery;
    private int[] internalIndices;
    private int[] lastFrontIndices;
    private int[] splitScratchL, splitScratchM, splitScratchR;

    // Various answers
    private int splitL;
    private int splitM;

    // Partially carrying input data
    private double[][] points;

    AbstractJFBSorting(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);

        sorter = new DoubleArraySorter(maximumPoints);
        indices = new int[maximumPoints];
        ranks = new int[maximumPoints];
        points = new double[maximumPoints][];
        rankQuery = createStructure(maximumPoints);

        internalIndices = new int[maximumPoints];
        lastFrontIndices = new int[maximumPoints];
        splitScratchL = new int[maximumPoints];
        splitScratchM = new int[maximumPoints];
        splitScratchR = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() throws Exception {
        sorter = null;
        indices = null;
        ranks = null;
        points = null;
        rankQuery = null;

        internalIndices = null;
        lastFrontIndices = null;
        splitScratchL = null;
        splitScratchM = null;
        splitScratchR = null;
    }

    @Override
    protected final void sortChecked(double[][] points, int[] ranks) {
        int n = points.length;
        int dim = points[0].length;
        Arrays.fill(ranks, 0);
        if (dim > 0) {
            for (int i = 0; i < n; ++i) {
                internalIndices[i] = i;
            }
            sorter.lexicographicalSort(points, internalIndices, 0, n, dim);

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
                int newN = 1;
                int lastII = internalIndices[0];
                this.points[0] = points[lastII];
                this.indices[0] = 0;
                this.ranks[lastII] = 0;
                for (int i = 1; i < n; ++i) {
                    int currII = internalIndices[i];
                    double[] curr = points[lastII];
                    double[] next = points[currII];
                    boolean same = true;
                    for (int t = dim - 1; t >= 0; --t) {
                        if (curr[t] != next[t]) {
                            same = false;
                            break;
                        }
                    }
                    if (!same) {
                        // Copying the point to the internal array.
                        this.points[newN] = points[currII];
                        this.indices[newN] = newN;
                        this.ranks[newN] = 0;
                        lastII = currII;
                        ++newN;
                    }

                    // Abusing the argument "ranks" array to keep where the reference point lies.
                    ranks[currII] = newN - 1;
                }

                // 3.2: Calling the actual sorting
                helperA(0, newN, dim - 1);

                // 3.3: Applying the results back. After that, the argument "ranks" array stops being abused.
                for (int i = 0; i < n; ++i) {
                    ranks[i] = this.ranks[ranks[i]];
                }
            }
        }
    }

    private boolean strictlyDominatesAssumingNotSame(int goodIndex, int weakIndex, int maxObj) {
        double[] goodPoint = points[goodIndex];
        double[] weakPoint = points[weakIndex];
        for (int i = maxObj; i >= 0; --i) {
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
        for (int i = from; i < until; ++i) {
            double v = points[indices[i]][obj];
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
        for (int i = from; i < until; ++i) {
            double v = points[indices[i]][obj];
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
        int target = from;
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
        System.arraycopy(splitScratchM, from, indices, from, target - from);
    }

    protected abstract RankQueryStructure createStructure(int maximumPoints);

    private void sweepA(int from, int until) {
        for (int i = from; i < until; ++i) {
            rankQuery.addPossibleKey(points[indices[i]][1]);
        }
        rankQuery.init();
        for (int i = from; i < until; ++i) {
            int curr = indices[i];
            double currY = points[curr][1];
            int result = Math.max(ranks[curr], rankQuery.getMaximumWithKeyAtMost(currY) + 1);
            ranks[curr] = result;
            rankQuery.put(currY, result);
        }
        rankQuery.clear();
    }

    private void sweepB(int goodFrom, int goodUntil, int weakFrom, int weakUntil) {
        for (int i = goodFrom; i < goodUntil; ++i) {
            rankQuery.addPossibleKey(points[indices[i]][1]);
        }
        rankQuery.init();
        int goodI = goodFrom;
        for (int weakI = weakFrom; weakI < weakUntil; ++weakI) {
            int weakCurr = indices[weakI];
            while (goodI < goodUntil && indices[goodI] < weakCurr) {
                int goodCurr = indices[goodI++];
                rankQuery.put(points[goodCurr][1], ranks[goodCurr]);
            }
            ranks[weakCurr] = Math.max(ranks[weakCurr], rankQuery.getMaximumWithKeyAtMost(points[weakCurr][1]) + 1);
        }
        rankQuery.clear();
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
        } else {
            sorter.resetMedian();
            sorter.consumeDataForMedian(points, indices, from, until, obj);
            if (sorter.getLastMedianConsumptionMin() == sorter.getLastMedianConsumptionMax()) {
                helperA(from, until, obj - 1);
            } else {
                double median = sorter.findMedian();
                int smallerThanMedian = sorter.howManySmallerThanMedian();
                int largerThanMedian = sorter.howManyLargerThanMedian();
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

    private void helperB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        int goodN = goodUntil - goodFrom;
        int weakN = weakUntil - weakFrom;
        if (goodN == 1) {
            int gi = indices[goodFrom];
            for (int i = weakFrom; i < weakUntil; ++i) {
                int wi = indices[i];
                if (strictlyDominatesAssumingNotSame(gi, wi, obj)) {
                    tryUpdateRank(gi, wi);
                }
            }
        } else if (weakN == 1) {
            int wi = indices[weakFrom];
            for (int i = goodFrom; i < goodUntil; ++i) {
                int gi = indices[i];
                if (strictlyDominatesAssumingNotSame(gi, wi, obj)) {
                    tryUpdateRank(gi, wi);
                }
            }
        } else if (obj == 1) {
            sweepB(goodFrom, goodUntil, weakFrom, weakUntil);
        } else {
            sorter.resetMedian();
            sorter.consumeDataForMedian(points, indices, goodFrom, goodUntil, obj);
            double goodMaxObj = sorter.getLastMedianConsumptionMax();
            sorter.consumeDataForMedian(points, indices, weakFrom, weakUntil, obj);
            double weakMinObj = sorter.getLastMedianConsumptionMin();
            if (goodMaxObj <= weakMinObj) {
                helperB(goodFrom, goodUntil, weakFrom, weakUntil, obj - 1);
            } else {
                double median = sorter.findMedian();
                int totalSmallerThanMedian = sorter.howManySmallerThanMedian();
                int totalLargerThanMedian = sorter.howManyLargerThanMedian();
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

    private void twoDimensionalCase(double[][] points, int[] ranks) {
        // Also uses internalIndices and lastFrontIndices
        int maxRank = 1;
        int n = ranks.length;

        int lastII = internalIndices[0];
        double lastX = points[lastII][0];
        double lastY = points[lastII][1];
        double minY = lastY;

        // Point 0 always has rank 0.
        lastFrontIndices[0] = lastII;

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
                    double midY = points[lastFrontIndices[mid]][1];
                    if (cy < midY) {
                        right = mid;
                    } else {
                        left = mid;
                    }
                }
                // "right" is now our rank.
                ranks[ii] = right;
                lastFrontIndices[right] = ii;
                if (right == maxRank) {
                    ++maxRank;
                }
            }

            lastII = ii;
            lastX = cx;
            lastY = cy;
        }
    }
}
