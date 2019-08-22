package ru.ifmo.nds.jfb.hybrid.tuning;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ThresholdTunerRunner {
    private static class Simple {
        private static final double BUDGET_CONST = 50;
        private static final double TAKEN_CONST = 2;

        private static int getTaken(int problemSize) {
            return (int) (TAKEN_CONST * problemSize * problemSize);
        }

        private static int getBudget(int problemSize) {
            return (int) (BUDGET_CONST * problemSize * Math.log(problemSize + 1));
        }

        static void run(String[] args) throws IOException {
            Random rand = new Random();
            int countRuns = Integer.parseInt(args[0]);
            ThresholdFactory thresholdFactory = new DynamicThresholdFactory(Integer.parseInt(args[1]));
            Threshold threshold = thresholdFactory.createThreshold();
            try (PrintWriter pw = new PrintWriter(thresholdFactory.getDescription() + ".csv")) {
                pw.println("i,n,budget,taken,thr");
                for (int i = 0; i < countRuns; i++) {
                    int problemSize = rand.nextInt(threshold.getThreshold());
                    int operationBudget = getBudget(problemSize);
                    int operationTaken = getTaken(problemSize);
                    boolean wasOK = !threshold.shallTerminate(operationBudget, operationTaken);
                    threshold.recordPerformance(problemSize, operationBudget, operationTaken, !wasOK);
                    pw.println((i + 1) + ", " + problemSize + ", " + operationBudget + ", " + operationTaken + ", " + threshold.getThreshold());
                }
            }
        }
    }

    private static class Hierarchic {
        private static double ADJUSTMENT_MULTIPLE = 1;
        private static double ITERATION_DEPENDENT_MULTIPLE = 1;

        private static long hybridOperations(int problemSize) {
            return (long) (ITERATION_DEPENDENT_MULTIPLE * problemSize * problemSize * ThreadLocalRandom.current().nextDouble());
        }

        private static int divideConquerOperationsForRound(int problemSize) {
            return 60 * problemSize;
        }

        private static int divideConquerTotalOperations(int problemSize) {
            int result = 0;
            int multiple = 1;
            while (problemSize > 1) {
                result += multiple * divideConquerOperationsForRound(problemSize);
                multiple *= 2;
                problemSize = (problemSize + 1) / 2;
            }
            return result + multiple * divideConquerOperationsForRound(problemSize);
        }

        static long go(int problemSize, Threshold threshold) {
            if (problemSize == 1) {
                return divideConquerOperationsForRound(problemSize);
            } else if (problemSize > threshold.getThreshold()) {
                int left = ThreadLocalRandom.current().nextInt(problemSize - 1) + 1;
                int right = problemSize - left;
                return divideConquerOperationsForRound(problemSize) + go(left, threshold) + go(right, threshold);
            } else {
                int plannedBudget = (int) (divideConquerTotalOperations(problemSize) * ADJUSTMENT_MULTIPLE);
                long plannedHybridTime = hybridOperations(problemSize);
                int actualHybridTime;
                boolean wasOk;
                if (plannedHybridTime > Integer.MAX_VALUE || threshold.shallTerminate(plannedBudget, (int) plannedHybridTime)) {
                    wasOk = false;
                    actualHybridTime = 2 * plannedBudget; // hardcoded for existing threshold implementations :/
                } else {
                    wasOk = true;
                    actualHybridTime = (int) plannedHybridTime;
                }
                threshold.recordPerformance(problemSize, plannedBudget, actualHybridTime, !wasOk);
                if (wasOk) {
                    return actualHybridTime;
                } else {
                    int left = ThreadLocalRandom.current().nextInt(problemSize - 1) + 1;
                    int right = problemSize - left;
                    return divideConquerOperationsForRound(problemSize) + go(left, threshold) + go(right, threshold) + actualHybridTime;
                }
            }
        }

        static void run(String[] args) throws IOException {
            int problemSize = Integer.parseInt(args[0]);
            int iterations = Integer.parseInt(args[1]);
            int averageRuns = Integer.parseInt(args[2]);

            ThresholdFactory[] factories = {
                    new ConstantThresholdFactory(0),
                    new ConstantThresholdFactory(problemSize + 1),
                    new ConstantThresholdFactory(10),
                    new ConstantThresholdFactory(20),
                    new ConstantThresholdFactory(50),
                    new ConstantThresholdFactory(100),
                    new ConstantThresholdFactory(250),
                    new DynamicThresholdFactory(10),
                    new DynamicThresholdFactory(1000),
                    new DynamicAdjustableIncreaseThresholdFactory(10, 1.000001),
                    new DynamicAdjustableIncreaseThresholdFactory(1000, 1.000001),
                    new DynamicAdjustableIncreaseThresholdFactory(10, 1.0001),
                    new DynamicAdjustableIncreaseThresholdFactory(1000, 1.0001),
            };

            double[] multiples = new double[] {0.1, 0.2, 0.3, 0.4, 0.5, 0.6};

            try (PrintWriter pw = new PrintWriter("output.json")) {
                pw.println("[");
                for (int i = 0; i < factories.length; i++) {
                    ThresholdFactory factory = factories[i];
                    for (double mul : (factory instanceof ConstantThresholdFactory ? new double[] { 1.0 } : multiples)) {
                        ADJUSTMENT_MULTIPLE = mul;
                        pw.println("    {\"factory\":\"" + factory.getDescription() + "@" + mul + "\",\"multiple\":" + mul + ",\"measurements\":[");
                        for (int run = 0; run < averageRuns; ++run) {
                            Threshold threshold = factory.createThreshold();
                            for (int iteration = 0; iteration < iterations; ++iteration) {
                                ITERATION_DEPENDENT_MULTIPLE = 1 - 0.8 * iteration / iterations;
                                pw.println("        {\"iteration\":" + iteration + ",\"values\":[");
                                long result = go(problemSize, threshold);
                                pw.println("            {\"type\":\"runtime\",\"value\":" + result + "},");
                                pw.println("            {\"type\":\"threshold\",\"value\":" + threshold.getThreshold() + "}");
                                pw.println("        ]}" + (iteration + 1 < iterations || run + 1 < averageRuns ? "," : ""));
                            }
                        }
                        pw.println("    ]}" + (i + 1 < factories.length || mul < multiples[multiples.length - 1] ? "," : ""));
                    }
                }
                pw.println("]");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
            case "simple":
                Simple.run(subArgs);
                break;
            case "hierarchic":
                Hierarchic.run(subArgs);
                break;
            default:
                System.out.println("Unknown runner type '" + args[0] + "'");
                System.exit(1);
        }
    }

}
