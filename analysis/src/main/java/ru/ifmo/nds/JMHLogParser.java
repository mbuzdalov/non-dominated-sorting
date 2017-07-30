package ru.ifmo.nds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class JMHLogParser {
    private static final String BENCHMARK_NAME_START = "# Benchmark: ";
    private static final String PARAMETERS_START = "# Parameters: ";
    private static final String FORK_START = "# Fork: ";
    private static final String RUN_COMPLETE_START = "# Run complete.";
    private static final String ITERATION = "Iteration";
    private static final String WARMUP_ITERATION = "# Warmup Iteration";
    private static final String RESULT_START = "Result";

    private static final List<String> UNWANTED_PREFIXES = Arrays.asList("[info]", "[success]");

    private static String cleanStringFromColors(String s) {
        StringBuilder rv = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (ch == '\u001b') {
                i += 3;
            } else {
                rv.append(ch);
            }
        }
        return rv.toString();
    }

    private List<JMHBenchmarkResult> consumedBenchmarks = new ArrayList<>();

    private int state = 0;
    private String lastBenchmarkName = null;
    private Map<String, Integer> lastBenchmarkParams = new HashMap<>();
    private List<Double> lastBenchmarkData = new ArrayList<>();

    private void consumeBenchmarkName(String line) {
        lastBenchmarkData.clear();
        lastBenchmarkParams.clear();
        lastBenchmarkName = line.substring(BENCHMARK_NAME_START.length()).trim();
        int lastDot = lastBenchmarkName.lastIndexOf('.');
        lastBenchmarkName = lastBenchmarkName.substring(lastDot + 1);
        while (true) {
            int lastUnderscore = lastBenchmarkName.lastIndexOf('_');
            if (lastUnderscore < 0) {
                break;
            }
            int lastNonDigit = lastBenchmarkName.length() - 1;
            while (lastNonDigit > lastUnderscore && Character.isDigit(lastBenchmarkName.charAt(lastNonDigit))) {
                --lastNonDigit;
            }
            if (lastNonDigit < lastBenchmarkName.length() - 1) {
                String paramName = lastBenchmarkName.substring(lastUnderscore + 1, lastNonDigit + 1);
                int paramValue = Integer.parseInt(lastBenchmarkName.substring(lastNonDigit + 1));
                lastBenchmarkParams.put(paramName, paramValue);
                lastBenchmarkName = lastBenchmarkName.substring(0, lastUnderscore);
            }
        }
    }

    private void consumeParameters(String line) {
        StringTokenizer st = new StringTokenizer(line.substring(PARAMETERS_START.length()).trim(), "()");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int equalSign = token.indexOf('=');
            if (equalSign >= 0) {
                try {
                    String key = token.substring(0, equalSign).trim();
                    int value = Integer.parseInt(token.substring(equalSign + 1).trim());
                    lastBenchmarkParams.put(key, value);
                } catch (Exception ex) {
                    System.out.println("Parameter parse failed on token '" + token + "'");
                }
            }
        }
    }

    private double parseDoubleIgnoringLocale(String s) {
        s = s.replace(',', '.');
        return Double.parseDouble(s);
    }

    private void consumeLine(String line) {
        line = cleanStringFromColors(line);
        boolean lineChanged;
        do {
            lineChanged = false;
            for (String prefix : UNWANTED_PREFIXES) {
                if (line.startsWith(prefix)) {
                    line = line.substring(prefix.length()).trim();
                    lineChanged = true;
                }
            }
        } while (lineChanged);

        switch (state) {
            case 0: {
                if (line.startsWith(BENCHMARK_NAME_START)) {
                    consumeBenchmarkName(line);
                    state = 1;
                }
                break;
            }
            case 1: {
                if (line.startsWith(PARAMETERS_START)) {
                    consumeParameters(line);
                    state = 2;
                } else if (line.startsWith(FORK_START)) {
                    state = 3;
                }
                break;
            }
            case 2: {
                if (line.startsWith(FORK_START)) {
                    state = 3;
                }
                break;
            }
            case 3: {
                if (line.startsWith(ITERATION)) {
                    StringTokenizer st = new StringTokenizer(line.substring(ITERATION.length()));
                    st.nextToken(); // iteration number
                    String value = st.nextToken();
                    String unit = st.nextToken();
                    if (!unit.equals("us/op")) {
                        throw new UnsupportedOperationException("Cannot work with units other than us/op: '" + unit);
                    }
                    lastBenchmarkData.add(parseDoubleIgnoringLocale(value) * 1e-6);
                } else if (line.startsWith(WARMUP_ITERATION)) {
                    StringTokenizer st = new StringTokenizer(line.substring(WARMUP_ITERATION.length()));
                    st.nextToken(); // iteration number
                    String value = st.nextToken();
                    String unit = st.nextToken();
                    if (!unit.equals("us/op")) {
                        throw new UnsupportedOperationException("Cannot work with units other than us/op: '" + unit);
                    }
                    lastBenchmarkData.add(parseDoubleIgnoringLocale(value) * 1e-6);
                } else if (line.startsWith(RESULT_START)) {
                    consumedBenchmarks.add(new JMHBenchmarkResult(lastBenchmarkName, lastBenchmarkParams, lastBenchmarkData));
                    state = 4;
                }
                break;
            }
            case 4: {
                if (line.startsWith(BENCHMARK_NAME_START)) {
                    consumeBenchmarkName(line);
                    state = 1;
                } else if (line.startsWith(RUN_COMPLETE_START)) {
                    state = 5;
                }
                break;
            }
            case 5: {
                break;
            }
        }
    }

    private JMHLogParser(Stream<String> lines) {
        lines.forEachOrdered(this::consumeLine);
        if (state != 5) {
            throw new IllegalStateException("Unexpected end of input data");
        }
    }

    private List<JMHBenchmarkResult> getResults() {
        return new ArrayList<>(consumedBenchmarks);
    }

    public static List<JMHBenchmarkResult> parse(String filename) throws IOException {
        return new JMHLogParser(Files.lines(Paths.get(filename))).getResults();
    }
}
