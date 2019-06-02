package ru.ifmo.nds.jfb.hybrid.tuning;

public class DynamicThresholdFactory extends ThresholdFactory {
    private static final double TUNING_FAILURE_PROPORTION = 0.1;
    private static final double TUNING_SUCCESS_PROPORTION = 0.1;

    private final int initialValue;

    public DynamicThresholdFactory(int initialValue) {
        this.initialValue = initialValue;
    }

    @Override
    public String getDescription() {
        return "dynamic (initially " + initialValue + ")";
    }

    @Override
    public Threshold createThreshold() {
        return new Impl(initialValue);
    }

    private static final class Impl extends Threshold {
        private double threshold;

        Impl(int initialValue) {
            threshold = initialValue;
        }

        @Override
        public final int getThreshold() {
            return (int) threshold;
        }

        @Override
        public final boolean shallTerminate(int operationBudget, int operationsTaken) {
            return operationsTaken > operationBudget + operationBudget;
        }

//        @Override
//        public final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {
//            double thresholdEstimate = problemSize * Math.max(0.5, Math.min(2, (double) operationBudget / operationsTaken));
//            if (operationBudget >= operationsTaken) {
//                // OK, threshold can only grow
//                if (thresholdEstimate > threshold) {
//                    double ratio = thresholdEstimate / threshold;
//                    threshold *= (1 + (ratio - 1) * TUNING_SUCCESS_PROPORTION);
//                } else {
//                    threshold *= 1.0001;
//                }
//            } else {
//                // Not OK, threshold can only shrink
//                if (thresholdEstimate < threshold) {
//                    double ratio = threshold / thresholdEstimate;
//                    threshold /= (1 + (ratio - 1) * TUNING_FAILURE_PROPORTION);
//                } else {
//                    threshold /= 1.01;
//                }
//            }
//        }


//        private final Random rand = new Random();
//        private final double probability = 0.5;
//        @Override
//        public final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {
//            double thresholdEstimate = problemSize * Math.max(0.5, Math.min(2, (double) operationBudget / operationsTaken));
//            if (operationBudget >= operationsTaken) {
//                // OK, threshold can only grow
//                if (thresholdEstimate > threshold) {
//                    double ratio = thresholdEstimate / threshold;
//                    threshold *= (1 + (ratio - 1) * TUNING_SUCCESS_PROPORTION);
//                } else {
//                    if (rand.nextDouble() < probability) {
//                        threshold *= 1.01;
//                    }
//                }
//            } else {
//                // Not OK, threshold can only shrink
//                if (thresholdEstimate < threshold) {
//                    double ratio = threshold / thresholdEstimate;
//                    threshold /= (1 + (ratio - 1) * TUNING_FAILURE_PROPORTION);
//                } else {
//                    threshold /= 1.01;
//                }
//            }
//        }

//        private final int maxSequence = 5;
//        private int curSequence = 0;
//        @Override
//        public final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {
//            double thresholdEstimate = problemSize * Math.max(0.5, Math.min(2, (double) operationBudget / operationsTaken));
//            if (operationBudget >= operationsTaken) {
//                // OK, threshold can only grow
//                if (thresholdEstimate > threshold) {
//                    double ratio = thresholdEstimate / threshold;
//                    threshold *= (1 + (ratio - 1) * TUNING_SUCCESS_PROPORTION);
//                } else {
//                    curSequence++;
//                    if (curSequence == maxSequence) {
//                        threshold *= 1.01;
//                        curSequence = 0;
//                    }
//                }
//            } else {
//                curSequence = 0;
//                // Not OK, threshold can only shrink
//                if (thresholdEstimate < threshold) {
//                    double ratio = threshold / thresholdEstimate;
//                    threshold /= (1 + (ratio - 1) * TUNING_FAILURE_PROPORTION);
//                } else {
//                    threshold /= 1.01;
//                }
//            }
//        }

        private final double maxMult = 1.01;
        private final double minMult = 1.000001;
        private double mult = 1.01;
        private boolean isSequence = false;
        @Override
        public final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {
            double thresholdEstimate = problemSize * Math.max(0.5, Math.min(2, (double) operationBudget / operationsTaken));
            if (operationBudget >= operationsTaken) {
                // OK, threshold can only grow
                if (thresholdEstimate > threshold) {
                    double ratio = thresholdEstimate / threshold;
                    threshold *= (1 + (ratio - 1) * TUNING_SUCCESS_PROPORTION);
                } else {
                    threshold *= mult;
                    if (isSequence) {
                        mult = Math.max(1 + (mult - 1) / 2, minMult);
//                        System.out.println(mult);
                    }
                    isSequence = true;
                }
            } else {
                isSequence = false;
                mult = maxMult;
                // Not OK, threshold can only shrink
                if (thresholdEstimate < threshold) {
                    double ratio = threshold / thresholdEstimate;
                    threshold /= (1 + (ratio - 1) * TUNING_FAILURE_PROPORTION);
                } else {
                    threshold /= 1.01;
                }
            }
        }

    }
}
