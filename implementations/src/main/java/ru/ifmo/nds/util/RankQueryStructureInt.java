package ru.ifmo.nds.util;

/**
 * A rank query structure to be used in 2D algorithms for non-dominated sorting (with lower bounds).
 *
 * @author Maxim Buzdalov
 */
public abstract class RankQueryStructureInt {
    /**
     * Returns the name of this data structure.
     * @return the name of this data structure.
     */
    public abstract String getName();

    /**
     * Returns the maximum number of points which is supported by this data structure.
     * @return the maximum number of points which is supported by this data structure.
     */
    public abstract int maximumPoints();

    /**
     * Returns whether this data structure supports using multiple threads.
     * @return whether this data structure supports using multiple threads.
     */
    public abstract boolean supportsMultipleThreads();

    /**
     * Creates a {@link RangeHandle} that uses the storage of the data structure and performs the actual operations.
     * @param from the minimum inclusive index of the storage the handle is allowed to use
     * @param until the maximum exclusive index of the storage the handle is allowed to use
     * @return the range handle.
     */
    public abstract RangeHandle createHandle(int storageStart, int from, int until, int[] indices, int[] keys);

    public abstract static class RangeHandle {
        /**
         * Puts a value for the given key.
         *
         * This operation should be performed in the initialized mode,
         * and may throw exceptions in the preparation mode.
         *
         * The value must be non-negative.
         *
         * @param key the key.
         * @param value the value.
         * @return the same handle or a new improved handle on the same range.
         */
        public abstract RangeHandle put(int key, int value);

        /**
         * Returns a maximum value, among those which were added to the structure previously
         * and which were associated with the key smaller than, or equal to, the given key.
         *
         * This function supports returning imprecise answers with greater speed.
         * If one specifies the parameter {@code minimumMeaningfulAnswer} greater than {@code -1},
         * the search may return faster when it encounters that the answer will be smaller than the given parameter.
         * In this case, it will return {@code minimumMeaningfulAnswer - 1} or a smaller value.
         *
         * A safe value for {@code minimumMeaningfulAnswer} is -1; in this case, no information is lost.
         *
         * This operation should be performed in the initialized mode,
         * and may throw exceptions in the preparation mode.
         *
         * @param key the key.
         * @param minimumMeaningfulAnswer the minimum meaningful answer to return.
         * @return the maximum found value for another key above this key.
         */
        public abstract int getMaximumWithKeyAtMost(int key, int minimumMeaningfulAnswer);
    }
}
