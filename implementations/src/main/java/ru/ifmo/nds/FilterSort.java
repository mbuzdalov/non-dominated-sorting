package ru.ifmo.nds;

import java.util.Arrays;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.ArraySorter;

public final class FilterSort {
    private FilterSort() {}

    public static NonDominatedSortingFactory getInstance() {
        return INSTANCE;
    }

    private static final NonDominatedSortingFactory INSTANCE = (maximumPoints, maximumDimension) ->
            new NonDominatedSorting(maximumPoints, maximumDimension) {
        private double[][] points = new double[maximumPoints][];
        private int[][] orderByObjective = new int[maximumDimension][maximumPoints];
        private int[][] indexInObjective = new int[maximumDimension][];
        private int[] bestObjective = new int[maximumPoints];
        private int[] worstObjective = new int[maximumPoints];
        private int[] ranks = new int[maximumPoints];
        private int[] sumOfIndices = new int[maximumPoints];
        private int[] orderBySumOfIndices = new int[maximumPoints];
        private int[] startOrderByObjective = new int[maximumDimension];
        private boolean[] alive = new boolean[maximumPoints];
        private boolean[] isCandidate = new boolean[maximumPoints];
        private int[] candidates = new int[maximumPoints];

        {
            if (maximumDimension > 0) {
                indexInObjective[0] = orderByObjective[0];
                for (int i = 1; i < maximumDimension; ++i) {
                    indexInObjective[i] = new int[maximumPoints];
                }
            }
        }

        @Override
        public String getName() {
            return "Filter Sort";
        }

        @Override
        protected void closeImpl() {
            points = null;
            orderByObjective = null;
            indexInObjective = null;
            bestObjective = null;
            worstObjective = null;
            ranks = null;
            sumOfIndices = null;
            orderBySumOfIndices = null;
            alive = null;
            isCandidate = null;
            candidates = null;
        }

        private void initializeObjectiveIndices(int size, int dim) {
            Arrays.fill(bestObjective, 0, size, 0);
            Arrays.fill(worstObjective, 0, size, 0);
            ArrayHelper.fillIdentity(sumOfIndices, size);
            ArrayHelper.fillIdentity(orderByObjective[0], size);
            for (int d = 1; d < dim; ++d) {
                int[] currentObjectiveOrder = orderByObjective[d];
                ArrayHelper.fillIdentity(currentObjectiveOrder, size);
                sorter.sortComparingByIndicesIfEqual(points, currentObjectiveOrder, 0, size, d);
                int[] currentObjectiveIndex = indexInObjective[d];
                for (int i = 0; i < size; ++i) {
                    currentObjectiveIndex[currentObjectiveOrder[i]] = i;
                }
                for (int i = 0; i < size; ++i) {
                    int coi = currentObjectiveIndex[i];
                    if (coi < indexInObjective[bestObjective[i]][i]) {
                        bestObjective[i] = d;
                    }
                    if (coi > indexInObjective[worstObjective[i]][i]) {
                        worstObjective[i] = d;
                    }
                    sumOfIndices[i] += coi;
                }
            }
            ArrayHelper.fillIdentity(orderBySumOfIndices, size);
            ArraySorter.sortIndicesByValues(orderBySumOfIndices, sumOfIndices, 0, size);
        }

        private int populateCandidates(int filterIndex, int obj, int nCandidates) {
            int start = startOrderByObjective[obj];
            int[] order = orderByObjective[obj];
            // Find where the filter is.
            int last = start;
            while (order[last] != filterIndex) {
                ++last;
            }
            // The filter is at `last`.
            // Scan backwards, move the alive ones to the end of the region, mark them as candidates
            int newStart = last;
            while (--last >= start) {
                int current = order[last];
                if (alive[current]) {
                    if (!isCandidate[current]) {
                        isCandidate[current] = true;
                        candidates[nCandidates] = current;
                        ++nCandidates;
                    }
                    order[--newStart] = current;
                }
            }
            startOrderByObjective[obj] = newStart;
            return nCandidates;
        }

        private boolean isCandidateGood(int curr, int maxObj) {
            double[] currPoint = points[curr];
            int worstObj = worstObjective[curr];
            double currWorst = currPoint[worstObj];
            int bestObj = bestObjective[curr];
            int[] bestOrder = orderByObjective[bestObj];

            for (int j = startOrderByObjective[bestObj], test; (test = bestOrder[j]) != curr; ++j) {
                if (alive[test]) {
                    double[] testPoint = points[test];
                    if (testPoint[worstObj] <= currWorst &&
                            DominanceHelper.strictlyDominatesAssumingNotEqual(testPoint, currPoint, maxObj)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private void runSorting(int n, int maxObj, int maximalMeaningfulRank) {
            Arrays.fill(alive, 0, n, true);
            Arrays.fill(ranks, maximalMeaningfulRank + 1);

            int aliveCount = n;
            int filterIndex, filterIndexToStart = 0;

            Arrays.fill(startOrderByObjective, 0, maxObj + 1, 0);

            for (int currentRank = 0; currentRank <= maximalMeaningfulRank && aliveCount > 0; ++currentRank) {
                // Choose the filter
                do {
                    filterIndex = orderBySumOfIndices[filterIndexToStart];
                    ++filterIndexToStart;
                } while (!alive[filterIndex]);

                // The filter is non-dominated, assign the rank to it
                ranks[filterIndex] = currentRank;
                alive[filterIndex] = false;
                if (--aliveCount == 0) {
                    break;
                }

                // Collect the candidate points (those which precede the filter in at least one objective
                int nCandidates = 0;
                for (int obj = 0; obj <= maxObj; ++obj) {
                    nCandidates = populateCandidates(filterIndex, obj, nCandidates);
                }

                // For each candidate, determine whether it is good or bad
                for (int i = 0; i < nCandidates; ++i) {
                    int curr = candidates[i];
                    isCandidate[curr] = false;
                    if (isCandidateGood(curr, maxObj)) {
                        ranks[curr] = currentRank;
                    }
                }
                for (int i = 0; i < nCandidates; ++i) {
                    int curr = candidates[i];
                    if (ranks[curr] == currentRank) {
                        alive[curr] = false;
                        --aliveCount;
                    }
                }
            }
        }

        @Override
        protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            int oldN = points.length;
            int dim = points[0].length;
            ArrayHelper.fillIdentity(indices, oldN);
            sorter.lexicographicalSort(points, indices, 0, oldN, dim);
            int n = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);
            initializeObjectiveIndices(n, dim);
            runSorting(n, dim - 1, maximalMeaningfulRank);
            for (int i = 0; i < oldN; ++i) {
                ranks[i] = this.ranks[ranks[i]];
                this.points[i] = null;
            }
        }
    };
}
