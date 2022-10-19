package ru.ifmo.nds.util.median;

/**
 * <p>
 * This interface encapsulates an algorithm for finding a median.
 * Some algorithms may require additional memory to be allocated in advance,
 * so instances of this interface are typically created through a {@link DestructiveMedianFactory}.
 * </p>
 * <p>
 * The algorithm shall support calls from multiple threads on non-intersecting index sets.
 * More precisely, if <code>algo</code> is returned by a call to a {@link DestructiveMedianFactory}, then
 * <code>algo.solve(array, 0, 100)</code> and <code>algo.solve(array, 100, 200)</code> shall be able to run concurrently,
 * whereas <code>algo.solve(array, 0, 100)</code> and <code>algo.solve(array, 50, 150)</code> will never be run concurrently.
 * The latter does not hold for the results of two different calls to {@link DestructiveMedianFactory},
 * even if they returned the same object.
 * </p>
 *
 * @author Maxim Buzdalov
 */
public interface DestructiveMedianAlgorithm {
    /**
     * Returns the maximum size of the input to be processed,
     * that is, the maximum value of the <code>until</code> argument of the {@link #solve(double[], int, int)} method.
     * @return the maximum size of the input.
     */
    int maximumSize();

    /**
     * <p>
     * Returns the median of the sequence of numbers supplied in <code>array</code>
     * between indices <code>from</code>, inclusively, and <code>until</code>, exclusively.
     * </p>
     * <p>
     * The median is the element at index <code>(from + until) / 2</code>,
     * if doing integer division in the absence of overflow,
     * in the assumption that the elements get sorted in the non-decreasing order.
     * </p>
     * <p>
     * The contents of the array between these indices may be arbitrarily changed during the execution,
     * but not the other parts of the array.
     * </p>
     * <p>
     * The running time should be O(n) where the number of elements n is <code>until - from</code>.
     * </p>
     *
     * @param array the array containing the numbers.
     * @param from the smallest number index, inclusively.
     * @param until the largest number index, exclusively.
     * @return the median.
     */
    double solve(double[] array, int from, int until);
}
