package ru.ifmo.nds.jfb;

public abstract class HybridAlgorithmWrapper {
    public abstract boolean supportsMultipleThreads();
    public abstract String getName();
    public abstract Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints);

    public static abstract class Instance {
        /**
         * Tries to invoke the hook for the role of helperA on points at indices [{@code from}; {@code until})
         * with the given maximum objective and maximal meaningful rank.
         *
         * The method returns the new value for the largest index of the points to consider (exclusively).
         * Normally, it is the same as {@code until}.
         * However, some of the points may receive a rank that exceeds the maximal meaningful rank.
         * In this case, the method is expected to drop the corresponding point indices and return an updated value
         * that is to indicate the end of the updated range of point indices.
         *
         * If the hook did not completely solve the subproblem, it returns a negative number.
         * The value {@code -X - 1} means that {@code X} is the index of the first unprocessed point.
         * The inequality {@code from <= X <= until} should be fulfilled.
         * When such a value is returned, the ranks that exceeded {@code maximalMeaningfulRank} have not been filtered,
         * and doing so is the responsibility of the caller.
         *
         * @param from the smallest index of the points to consider (inclusively).
         * @param until the largest index of the points to consider (exclusively).
         * @param obj the maximum objective to consider.
         * @param maximalMeaningfulRank the maximal meaningful rank.
         * @return {@code -X - 1} if the job is not complete and the first unprocessed point index is {@code X},
         *         the new value for {@code until} otherwise.
         */
        public abstract int helperAHook(int from, int until, int obj, int maximalMeaningfulRank);

        /**
         * Tries to invoke the hook for the role of helperB
         * on good points at indices [{@code goodFrom}; {@code goodUntil})
         * and weak points at indices [{@code weakFrom}; {@code weakUntil})
         * with the given maximum objective and maximal meaningful rank,
         * and also assuming that the free-space indices start at {@code tempFrom}.
         *
         * The method returns the new value for the largest index of the weak points to consider (exclusively).
         * Normally, it is the same as {@code weakUntil}.
         * However, some of the points may receive a rank that exceeds the maximal meaningful rank.
         * In this case, the method is expected to drop the corresponding point indices and return an updated value
         * that is to indicate the end of the updated range of point indices.
         *
         * If the hook did not completely solve the subproblem, it returns a negative number.
         * The value {@code -X - 1} means that {@code X} is the index of the first unprocessed point.
         * The inequality {@code weakFrom <= X <= weakUntil} should be fulfilled.
         * When such a value is returned, the ranks that exceeded {@code maximalMeaningfulRank} have not been filtered,
         * and doing so is the responsibility of the caller.
         *
         * @param goodFrom the smallest index of the good points to consider (inclusively).
         * @param goodUntil the largest index of the good points to consider (exclusively).
         * @param weakFrom the smallest index of the weak points to consider (inclusively).
         * @param weakUntil the largest index of the weak points to consider (exclusively).
         * @param obj the maximum objective to consider.
         * @param tempFrom the smallest index of the free space.
         * @param maximalMeaningfulRank the maximal meaningful rank.
         * @return {@code -X - 1} if the job is not complete and the first unprocessed point index is {@code X},
         *         the new value for {@code weakUntil} otherwise.
         */
        public abstract int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank);
    }
}
