package ru.ifmo.nds.fnds;

import ru.ifmo.nds.NonDominatedSorting;

public class LinearMemory extends NonDominatedSorting {
    private int[] howManyDominateMe;
    private int[] candidates;

    public LinearMemory(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);

        howManyDominateMe = new int[maximumPoints];
        candidates = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "Fast Non-Dominated Sorting (with linear memory)";
    }

    @Override
    protected void closeImpl() throws Exception {
        howManyDominateMe = null;
        candidates = null;
    }

    private static final int HAS_LESS_MASK = 1;
    private static final int HAS_GREATER_MASK = 2;

    private int dominanceComparison(double[] a, double[] b, int breakMask) {
        int dim = a.length;
        int result = 0;
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai < bi) {
                result |= HAS_LESS_MASK;
            } else if (ai > bi) {
                result |= HAS_GREATER_MASK;
            }
            if ((result & breakMask) == breakMask) {
                break;
            }
        }
        return result;
    }

    private void comparePointWithOthers(int index, double[][] points, int from, int until) {
        double[] pi = points[index];
        for (int j = from; j < until; ++j) {
            int comp = dominanceComparison(pi, points[j], HAS_LESS_MASK | HAS_GREATER_MASK);
            if (comp == HAS_LESS_MASK) {
                ++howManyDominateMe[j];
            } else if (comp == HAS_GREATER_MASK) {
                ++howManyDominateMe[index];
            }
        }
    }

    private void compareAllPoints(double[][] points, int count) {
        for (int i = 0; i < count; ++i) {
            comparePointWithOthers(i, points, i + 1, count);
        }
    }

    private void fillIdentity(int[] array, int count) {
        for (int i = 0; i < count; ++i) {
            array[i] = i;
        }
    }

    private int moveNonDominatedForward(int count) {
        int current = 0;
        for (int i = 0; i < count; ++i) {
            if (howManyDominateMe[i] == 0) {
                if (current != i) {
                    int tmp = candidates[i];
                    candidates[i] = candidates[current];
                    candidates[current] = tmp;
                }
                ++current;
            }
        }
        return current;
    }

    private void assignRankToRange(int[] ranks, int rank, int from, int until) {
        for (int i = from; i < until; ++i) {
            ranks[candidates[i]] = rank;
        }
    }

    private int punchNextPointsBySinglePoint(int index, double[][] points, int nextRankRight, int n) {
        double[] cp = points[candidates[index]];
        for (int nextIndex = nextRankRight; nextIndex < n; ++nextIndex) {
            int nextPoint = candidates[nextIndex];
            double[] np = points[nextPoint];

            int comp = dominanceComparison(cp, np, HAS_GREATER_MASK);
            if (comp == HAS_LESS_MASK) {
                if (--howManyDominateMe[nextPoint] == 0) {
                    if (nextIndex != nextRankRight) {
                        int tmp = candidates[nextIndex];
                        candidates[nextIndex] = candidates[nextRankRight];
                        candidates[nextRankRight] = tmp;
                    }
                    ++nextRankRight;
                }
            }
        }
        return nextRankRight;
    }

    private int punchNextPoints(double[][] points, int currentRankLeft, int currentRankRight, int n) {
        int nextRankRight = currentRankRight;
        for (int currIndex = currentRankLeft; currIndex < currentRankRight && nextRankRight < n; ++currIndex) {
            nextRankRight = punchNextPointsBySinglePoint(currIndex, points, nextRankRight, n);
        }
        return nextRankRight;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        compareAllPoints(points, n);
        fillIdentity(candidates, n);
        int currentRankLeft = 0;
        int currentRankRight = moveNonDominatedForward(n);

        int currentRank = 0;
        while (currentRankLeft < n) {
            assignRankToRange(ranks, currentRank, currentRankLeft, currentRankRight);
            int nextRankRight = punchNextPoints(points, currentRankLeft, currentRankRight, n);
            currentRankLeft = currentRankRight;
            currentRankRight = nextRankRight;
            ++currentRank;
        }
    }
}
