package ru.ifmo.fnds;

import ru.ifmo.NonDominatedSorting;

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

    @Override
    protected void sortChecked(double[][] points, int[] ranks) {
        int n = ranks.length;
        int dim = points[0].length;

        /*
         * Part 1: Counting who dominates who.
         */
        for (int i = 0; i < n; ++i) {
            double[] pi = points[i];
            for (int j = i + 1; j < n; ++j) {
                double[] pj = points[j];
                boolean iWeaklyDominatesJ = true;
                boolean jWeaklyDominatesI = true;
                for (int k = 0; k < dim; ++k) {
                    if (pi[k] < pj[k]) {
                        jWeaklyDominatesI = false;
                        if (!iWeaklyDominatesJ) {
                            break;
                        }
                    } else if (pi[k] > pj[k]) {
                        iWeaklyDominatesJ = false;
                        if (!jWeaklyDominatesI) {
                            break;
                        }
                    }
                }
                if (iWeaklyDominatesJ && !jWeaklyDominatesI) {
                    ++howManyDominateMe[j];
                } else if (jWeaklyDominatesI && !iWeaklyDominatesJ) {
                    ++howManyDominateMe[i];
                }
            }
        }

        /*
         * Part 2: Finding rank-0 points and moving them to the beginning.
         */
        for (int i = 0; i < n; ++i) {
            candidates[i] = i;
        }
        int currentRankLeft = 0, currentRankRight = 0;
        for (int i = 0; i < n; ++i) {
            if (howManyDominateMe[i] == 0) {
                if (currentRankRight != i) {
                    int tmp = candidates[i];
                    candidates[i] = candidates[currentRankRight];
                    candidates[currentRankRight] = tmp;
                }
                ++currentRankRight;
            }
        }

        /*
         * Part 3: Looping by current rank points, checking who they dominate,
         * and move points with no more dominating points to the beginning.
         */
        int currentRank = 0;
        while (currentRankLeft < n) {
            for (int i = currentRankLeft; i < currentRankRight; ++i) {
                ranks[candidates[i]] = currentRank;
            }

            int nextRankRight = currentRankRight;
            for (int currIndex = currentRankLeft; currIndex < currentRankRight && nextRankRight < n; ++currIndex) {
                double[] cp = points[candidates[currIndex]];
                for (int nextIndex = nextRankRight; nextIndex < n; ++nextIndex) {
                    int nextPoint = candidates[nextIndex];
                    double[] np = points[nextPoint];

                    boolean weakDomination = true;
                    boolean strictDomination = false;
                    for (int k = 0; k < dim; ++k) {
                        if (cp[k] > np[k]) {
                            weakDomination = false;
                            break;
                        }
                        strictDomination |= cp[k] < np[k];
                    }
                    if (weakDomination && strictDomination) {
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
            }

            currentRankLeft = currentRankRight;
            currentRankRight = nextRankRight;
            ++currentRank;
        }
    }
}
