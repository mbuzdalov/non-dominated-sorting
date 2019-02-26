package ru.ifmo.nds.util;

import java.util.Arrays;

public final class ArraySorter {
    private final double[] scratch;
    private double[][] points = null;
    private int[] indices = null;
    private int coordinate = -1;
    private int maxCoordinate = -1;

    private static final int INDICES_BY_VALUES_INSERTION_THRESHOLD = 15;
    private static final int INSERTION_SORT_THRESHOLD = 20;

    public ArraySorter(int maximumPoints) {
        this.scratch = new double[maximumPoints];
    }

    private static int split(double[] scratch, int[] indices, int from, int until) {
        double pivot = scratch[(from + until) >>> 1];
        int l = from, r = until - 1;
        while (l <= r) {
            double lv, rv;
            while ((lv = scratch[l]) < pivot) ++l;
            while ((rv = scratch[r]) > pivot) --r;
            if (l <= r) {
                ArrayHelper.swap(indices, l, r);
                scratch[l] = rv;
                scratch[r] = lv;
                ++l;
                --r;
            }
        }
        if (r + 1 == l) {
            return l;
        } else {
            return -l - 1;
        }
    }

    private static void insertionSort(double[] scratch, int[] indices, int from, int until) {
        int to = until - 1;
        for (int i = from, j = i; i < to; j = i) {
            double ai = scratch[++i], aj;
            int ii = indices[i];
            while (ai < (aj = scratch[j])) {
                scratch[j + 1] = aj;
                indices[j + 1] = indices[j];
                if (--j < from) {
                    break;
                }
            }
            scratch[++j] = ai;
            indices[j] = ii;
        }

    }

    private void sortImplInside(int from, int until) {
        if (from + INSERTION_SORT_THRESHOLD >= until) {
            insertionSort(scratch, indices, from, until);
        } else {
            int l = split(scratch, indices, from, until);
            int r = l;
            if (l < 0) {
                l = -l - 1;
                r = l - 1;
            }
            if (from + 1 < r) sortImplInside(from, r);
            if (l + 1 < until) sortImplInside(l, until);
        }
    }

    private void sortImpl(int from, int until) {
        for (int i = from; i < until; ++i) {
            scratch[i] = points[indices[i]][coordinate];
        }
        sortImplInside(from, until);
    }

    private void lexSortImpl(int from, int until, int coordinate) {
        this.coordinate = coordinate;
        sortImpl(from, until);

        if (coordinate + 1 < maxCoordinate) {
            int last = from;
            double lastX = scratch[from];
            for (int i = from + 1; i < until; ++i) {
                double currX = scratch[i];
                if (currX != lastX) {
                    if (last + 1 < i) {
                        lexSortImpl(last, i, coordinate + 1);
                    }
                    last = i;
                    lastX = currX;
                }
            }
            if (last + 1 < until) {
                lexSortImpl(last, until, coordinate + 1);
            }
        }
    }

    private void checkSize(int from, int until) {
        if (until > scratch.length) {
            throw new IllegalArgumentException("The internal scratch array length is " + scratch.length
                    + ", but you requested from = " + from + " until = " + until + " which is " + (until - from));
        }
    }

    public void compressCoordinates(double[] original, int[] indices, int[] target, int from, int until) {
        checkSize(from, until);

        System.arraycopy(original, from, scratch, from, until - from);
        for (int i = from; i < until; ++i) {
            indices[i] = i;
        }
        this.indices = indices;
        sortImplInside(from, until);
        this.indices = null;

        double prev = Double.NaN;
        for (int i = from, x = -1; i < until; ++i) {
            int ii = indices[i];
            double curr = original[ii];
            if (prev != curr) {
                prev = curr;
                ++x;
            }
            target[ii] = x;
        }
    }

    public void sort(double[][] points, int[] indices, int from, int until, int whichCoordinate) {
        checkSize(from, until);
        this.points = points;
        this.indices = indices;
        this.coordinate = whichCoordinate;

        sortImpl(from, until);

        this.points = null;
        this.indices = null;
        this.coordinate = -1;
    }

    public void lexicographicalSort(double[][] points, int[] indices, int from, int until, int maxCoordinate) {
        checkSize(from, until);
        this.points = points;
        this.indices = indices;
        this.maxCoordinate = maxCoordinate;

        lexSortImpl(from, until, 0);

        this.points = null;
        this.indices = null;
        this.maxCoordinate = -1;
    }

    private void sortComparingByIndicesIfEqualImpl(int from, int until) {
        sortImpl(from, until);

        // after sortImpl, scratch[i] == points[indices[i]][coordinate]

        int last = from;
        double lastX = scratch[from];
        for (int i = from + 1; i < until; ++i) {
            double currX = scratch[i];
            if (currX != lastX) {
                if (last + 1 < i) {
                    Arrays.sort(indices, last, i);
                }
                last = i;
                lastX = currX;
            }
        }
        if (last + 1 < until) {
            Arrays.sort(indices, last, until);
        }
    }

    public void sortComparingByIndicesIfEqual(double[][] points, int[] indices, int from, int until, int coordinate) {
        checkSize(from, until);

        this.points = points;
        this.indices = indices;
        this.coordinate = coordinate;

        sortComparingByIndicesIfEqualImpl(from, until);

        this.points = null;
        this.indices = null;
        this.coordinate = -1;
    }

    public static int retainUniquePoints(double[][] sourcePoints, int[] sortedIndices, double[][] targetPoints, int[] reindex) {
        int newN = 1, lastP = 0;
        int lastII = sortedIndices[0];
        double[] lastPoint = sourcePoints[lastII];
        targetPoints[0] = lastPoint;
        final int dim = lastPoint.length;
        reindex[lastII] = lastP;
        for (int i = 1, size = sourcePoints.length; i < size; ++i) {
            int currII = sortedIndices[i];
            double[] currPoint = sourcePoints[currII];
            if (!ArrayHelper.equal(lastPoint, currPoint, dim)) {
                targetPoints[newN] = currPoint;
                lastPoint = currPoint;
                lastP = newN;
                ++newN;
            }
            reindex[currII] = lastP;
        }
        return newN;
    }

    private static int splitIndicesByRanks(int[] indices, int[] values, int from, int until) {
        int left = from, right = until - 1;
        int pivot = values[indices[(from + until) >>> 1]];
        int sl, sr;
        while (left <= right) {
            while (values[sl = indices[left]] < pivot) ++left;
            while (values[sr = indices[right]] > pivot) --right;
            if (left <= right) {
                indices[left] = sr;
                indices[right] = sl;
                ++left;
                --right;
            }
        }
        return left - 1 == right ? left : -left - 1;
    }

    private static void insertionSortIndicesByValues(int[] indices, int[] values, int from, int to) {
        for (int i = from, j = i; i < to; j = i) {
            int ii = indices[++i], ij;
            int ai = values[ii];
            while (ai < values[ij = indices[j]]) {
                indices[j + 1] = ij;
                if (--j < from) {
                    break;
                }
            }
            indices[j + 1] = ii;
        }
    }

    public static void sortIndicesByValues(int[] indices, int[] values, int from, int until) {
        if (from + INDICES_BY_VALUES_INSERTION_THRESHOLD > until) {
            insertionSortIndicesByValues(indices, values, from, until - 1);
        } else {
            int left = splitIndicesByRanks(indices, values, from, until);
            int right = left;
            if (left < 0) {
                left = -left - 1;
                right = left - 1;
            }
            if (from + 1 < right) {
                sortIndicesByValues(indices, values, from, right);
            }
            if (left + 1 < until) {
                sortIndicesByValues(indices, values, left, until);
            }
        }
    }
}
