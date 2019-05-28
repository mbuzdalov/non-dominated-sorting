package ru.ifmo.nds.jfb.hybrid.tuning;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class ThresholdTunerRunner {
    private static final double BUDGET_CONST = 50;
    private static final double TAKEN_CONST = 2;

    public static void main(String[] args) throws FileNotFoundException {
        Random rand = new Random();
        int countRuns = Integer.valueOf(args[0]);
        ThresholdFactory thresholdFactory = new DynamicThresholdFactory(Integer.valueOf(args[1]));
        Threshold threshold = thresholdFactory.createThreshold();
        try (PrintWriter pw = new PrintWriter(thresholdFactory.getDescription() + ".csv")) {
            pw.println("n,d,iter,thr");
            for (int i = 0; i < countRuns; i++) {
                int problemSize = rand.nextInt(threshold.getThreshold());
                int operationBudget = getBudget(problemSize);
                int operationTaken = getTaken(problemSize);
                threshold.recordPerformance(problemSize, operationBudget, operationTaken, true);
                pw.println(problemSize + ", " + operationBudget + ", " + operationTaken + ", " + threshold.getThreshold());
            }
        }
    }

    private static int getTaken(int problemSize) {
        return (int) (TAKEN_CONST * problemSize * problemSize);
    }

    private static int getBudget(int problemSize) {
        return (int) (BUDGET_CONST * problemSize * Math.log(problemSize + 1));
    }
}
