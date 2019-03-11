package ru.ifmo.nds.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.Test;

import ru.ifmo.nds.ndt.SplitBuilder;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;

public class SplitBuilderTest {
    @SuppressWarnings("SameParameterValue")
    private static double[][] readDoublesFromResource(String resource) throws IOException {
        try (InputStream stream = CorrectnessTestsBase.class.getResourceAsStream(resource);
             InputStreamReader reader = new InputStreamReader(stream);
             BufferedReader lines = new BufferedReader(reader)) {
            List<double[]> rv = new ArrayList<>();
            String line;
            int dimension = -1;
            while ((line = lines.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                int ct = st.countTokens();
                if (ct == 0) {
                    continue;
                }
                if (dimension == -1) {
                    dimension = ct;
                } else if (dimension != ct) {
                    throw new IOException("Dimension inconsistency: first dimension " + dimension + ", one more " + ct);
                }
                double[] point = new double[dimension];
                for (int i = 0; i < dimension; ++i) {
                    point[i] = Double.parseDouble(st.nextToken());
                }
                rv.add(point);
            }
            return rv.toArray(new double[rv.size()][]);
        }
    }

    private void validateOnPoints(double[][] points) {
        int n = points.length;
        int dim = points[0].length;
        int[] indices = new int[n];

        double[][] newPoints = new double[n][];
        double[][] transposedPoints = new double[dim][n];

        ArraySorter sorter = new ArraySorter(n);

        int[] ranks = new int[n];
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);
        int newN = ArraySorter.retainUniquePoints(points, indices, newPoints, ranks);

        for (int i = 0; i < newN; ++i) {
            for (int j = 0; j < dim; ++j) {
                transposedPoints[j][i] = newPoints[i][j];
            }
        }

        new SplitBuilder(transposedPoints, n, 50).result(newN, dim);
    }

    @Test
    public void checkEqualPointHandling2D() {
        double[][] pts = new double[1000][2];
        for (int i = 0; i < pts.length; ++i) {
            pts[i][0] = i;
        }
        validateOnPoints(pts);
    }

    @Test
    public void checkEqualPointHandling10D() {
        double[][] pts = new double[1000][10];
        for (int i = 0; i < pts.length; ++i) {
            pts[i][0] = i;
        }
        validateOnPoints(pts);
    }

    @Test
    public void checkStackOverflowProblem() throws IOException {
        double[][] points = readDoublesFromResource("test6.in");
        validateOnPoints(points);
    }
}
