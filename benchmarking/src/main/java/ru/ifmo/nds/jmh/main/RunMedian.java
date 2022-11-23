package ru.ifmo.nds.jmh.main;

import java.util.Random;

import ru.ifmo.nds.util.median.DestructiveMedianAlgorithm;
import ru.ifmo.nds.util.median.DestructiveMedianFactory;

public class RunMedian {
    public static void main(String[] args) throws Exception {
        String algoName = args[0];
        int size = Integer.parseInt(args[1]);
        int count = Integer.parseInt(args[2]);
        int iterations = Integer.parseInt(args[3]);
        long seed = Long.parseLong(args[4]);

        DestructiveMedianAlgorithm algorithm = DestructiveMedianFactory.getByName(algoName).createInstance(size);

        //noinspection MismatchedReadAndWriteOfArray the inspection fails to detect the actual filling of the array
        double[][] instances = new double[count][size];
        double[] buffer = new double[size];

        Random random = new Random(seed);
        for (double[] instance : instances) {
            for (int i = 0; i < size; ++i) {
                instance[i] = random.nextDouble();
            }
        }

        double checksum = 0;
        for (int iteration = 0; iteration < iterations; ++iteration) {
            System.out.print("Iteration " + (iteration + 1) + ": ");
            for (double[] instance : instances) {
                System.arraycopy(instance, 0, buffer, 0, size);
                double median = algorithm.solve(buffer, 0, size);
                checksum += median;
            }
            System.out.println("done!");
        }
        System.out.println("Checksum: " + checksum);
    }
}
