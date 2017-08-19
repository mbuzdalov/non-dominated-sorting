package ru.ifmo.nds.rundb;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import ru.ifmo.nds.jmh.AbstractBenchmark;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;

public final class Records {
    // JSON fields
    private static final String ALGORITHM_ID = "algorithmId";
    private static final String DATASET_ID = "datasetId";
    private static final String MEASUREMENT_METHOD = "measurementMethod";
    private static final String MEASUREMENT_AUTHOR = "measurementAuthor";
    private static final String MEASUREMENT_TIME = "measurementTime";
    private static final String CPU_FREQUENCY = "cpuFrequency";
    private static final String CPU_MODEL_NAME = "cpuModelName";
    private static final String WARM_UP_MEASUREMENTS = "warmUpMeasurements";
    private static final String RELEASE_MEASUREMENTS = "releaseMeasurements";
    private static final String COMMENT = "comment";

    private static final List<String> ALL_NAMES = Arrays.asList(
            ALGORITHM_ID, DATASET_ID, MEASUREMENT_METHOD, MEASUREMENT_AUTHOR, MEASUREMENT_TIME,
            CPU_FREQUENCY, CPU_MODEL_NAME, WARM_UP_MEASUREMENTS, RELEASE_MEASUREMENTS, COMMENT
    );

    // JMH constants for the parser.
    private static final String BENCHMARK_NAME_START = "# Benchmark: ";
    private static final String PARAMETERS_START = "# Parameters: ";
    private static final String FORK_START = "# Fork: ";
    private static final String RUN_COMPLETE_START = "# Run complete.";
    private static final String ITERATION = "Iteration";
    private static final String WARMUP_ITERATION = "# Warmup Iteration";
    private static final String RESULT_START = "Result";
    private static final String TOTAL_TIME = "Total time: ";
    private static final List<String> UNWANTED_PREFIXES = Arrays.asList("[info]", "[success]");

    private Records() {}

    private static String getAlgorithmIdFromBenchmarkClass(String benchmarkClass) {
        try {
            AbstractBenchmark benchmark = (AbstractBenchmark) Class.forName(benchmarkClass).newInstance();
            return benchmark.getFactory().getName();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static BiFunction<String, Map<String, String>, String> DEFAULT_EXTRACTOR =
            (methodName, runParams) -> runParams.get(DATASET_ID);

    public static List<Record> parseJMHRun(
            Reader reader,
            String authorOfEveryBenchmark,
            double cpuFrequency,
            String cpuModelName,
            String comment
    ) throws IOException {
        return parseJMHRun(reader, authorOfEveryBenchmark, cpuFrequency, cpuModelName, comment, DEFAULT_EXTRACTOR);
    }

    private static List<Record> parseJMHRun(
            Reader reader,
            String authorOfEveryBenchmark,
            double cpuFrequency,
            String cpuModelName,
            String comment,
            BiFunction<String, Map<String, String>, String> benchmarkNameExtractor
    ) throws IOException {
        BufferedReader bufferedReader = reader instanceof BufferedReader
                ? (BufferedReader) reader
                : new BufferedReader(reader);
        JMHLogParser parser = new JMHLogParser(bufferedReader);

        Map<String, String> algorithmIdMapper = new HashMap<>();
        LocalDateTime timeOfEveryBenchmark = parser.time;
        List<Record> rv = new ArrayList<>(parser.consumedBenchmarks.size());
        for (JMHBenchmarkResult result : parser.consumedBenchmarks) {
            String benchmarkClass = result.benchmarkClass;
            String algorithmId = algorithmIdMapper.computeIfAbsent(benchmarkClass, Records::getAlgorithmIdFromBenchmarkClass);
            List<Double> warm = result.warmUpResults;
            List<Double> release = result.releaseResults;
            String datasetId = benchmarkNameExtractor.apply(result.methodName, result.params);
            int divisor = AllDatasets.getDataset(datasetId).getNumberOfInstances();
            for (int i = 0; i < warm.size(); ++i) {
                warm.set(i, warm.get(i) / divisor);
            }
            for (int i = 0; i < release.size(); ++i) {
                release.set(i, release.get(i) / divisor);
            }
            rv.add(new Record(algorithmId, datasetId, "JMH", authorOfEveryBenchmark,
                    timeOfEveryBenchmark, cpuFrequency, cpuModelName, warm, release, comment));
        }
        return rv;
    }

    public static void saveToFile(List<Record> records, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            saveToWriter(records, writer);
        }
    }

    public static void saveToWriter(List<Record> records, Writer writer) throws IOException {
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.beginArray();
        for (Record record : records) {
            jsonWriter.beginObject();
            jsonWriter.name(ALGORITHM_ID).value(record.getAlgorithmId());
            jsonWriter.name(DATASET_ID).value(record.getDatasetId());
            jsonWriter.name(MEASUREMENT_METHOD).value(record.getMeasurementMethod());
            jsonWriter.name(MEASUREMENT_AUTHOR).value(record.getMeasurementAuthor());
            jsonWriter.name(MEASUREMENT_TIME).value(record.getMeasurementTime().toString());
            jsonWriter.name(CPU_FREQUENCY).value(record.getCpuFrequency());
            jsonWriter.name(CPU_MODEL_NAME).value(record.getCpuModelName());
            jsonWriter.name(WARM_UP_MEASUREMENTS).beginArray();
            for (double measurement : record.getWarmUpMeasurements()) {
                jsonWriter.value(measurement);
            }
            jsonWriter.endArray();
            jsonWriter.name(RELEASE_MEASUREMENTS).beginArray();
            for (double measurement : record.getReleaseMeasurements()) {
                jsonWriter.value(measurement);
            }
            jsonWriter.endArray();
            jsonWriter.name(COMMENT).value(record.getComment());
            jsonWriter.endObject();
        }
        jsonWriter.endArray();
    }

    public static List<Record> loadFromFile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return loadFromReader(reader);
        }
    }

    public static List<Record> loadFromReader(Reader reader) throws IOException {
        List<Record> rv = new ArrayList<>();
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            Map<String, String> stringFields = new HashMap<>();
            Map<String, Double> doubleFields = new HashMap<>();
            Map<String, LocalDateTime> timeFields = new HashMap<>();
            Map<String, List<Double>> doubleListFields = new HashMap<>();

            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String key = jsonReader.nextName();
                switch (key) {
                    case ALGORITHM_ID:
                    case DATASET_ID:
                    case MEASUREMENT_METHOD:
                    case MEASUREMENT_AUTHOR:
                    case CPU_MODEL_NAME:
                    case COMMENT:
                        stringFields.put(key, jsonReader.nextString());
                        break;
                    case MEASUREMENT_TIME:
                        timeFields.put(key, LocalDateTime.parse(jsonReader.nextString()));
                        break;
                    case CPU_FREQUENCY:
                        doubleFields.put(key, jsonReader.nextDouble());
                        break;
                    case WARM_UP_MEASUREMENTS:
                    case RELEASE_MEASUREMENTS: {
                        List<Double> list = new ArrayList<>();
                        jsonReader.beginArray();
                        while (jsonReader.hasNext()) {
                            list.add(jsonReader.nextDouble());
                        }
                        jsonReader.endArray();
                        doubleListFields.put(key, list);
                        break;
                    }
                    default:
                        throw new IOException("In record description, stream contains JSON name '" + key
                                + "' which is not allowed");
                }
            }
            jsonReader.endObject();

            for (String s : ALL_NAMES) {
                if (!stringFields.containsKey(s) && !doubleFields.containsKey(s)
                        && !timeFields.containsKey(s) && !doubleListFields.containsKey(s)) {
                    throw new IOException("In record description, JSON name '" + s + "' is missing");
                }
            }
            rv.add(new Record(stringFields.get(ALGORITHM_ID), stringFields.get(DATASET_ID),
                    stringFields.get(MEASUREMENT_METHOD), stringFields.get(MEASUREMENT_AUTHOR),
                    timeFields.get(MEASUREMENT_TIME), doubleFields.get(CPU_FREQUENCY),
                    stringFields.get(CPU_MODEL_NAME), doubleListFields.get(WARM_UP_MEASUREMENTS),
                    doubleListFields.get(RELEASE_MEASUREMENTS), stringFields.get(COMMENT)));
        }
        jsonReader.endArray();
        return rv;
    }

    private static class JMHLogParser {
        private static String cleanStringFromColors(String s) {
            StringBuilder rv = new StringBuilder();
            for (int i = 0; i < s.length(); ++i) {
                char ch = s.charAt(i);
                if (ch == '\u001b') {
                    if (s.charAt(++i) != '[') throw new AssertionError();
                    do {
                        ++i;
                    } while (s.charAt(i) != 'm');
                } else {
                    rv.append(ch);
                }
            }
            return rv.toString();
        }

        private List<JMHBenchmarkResult> consumedBenchmarks = new ArrayList<>();

        private int state = 0;
        private String lastBenchmarkClass = null;
        private String lastBenchmarkName = null;
        private Map<String, String> lastBenchmarkParams = new HashMap<>();
        private List<Double> lastBenchmarkReleaseData = new ArrayList<>();
        private List<Double> lastBenchmarkWarmUpData = new ArrayList<>();
        private LocalDateTime time = null;

        private void consumeBenchmarkName(String line) {
            lastBenchmarkReleaseData.clear();
            lastBenchmarkWarmUpData.clear();
            lastBenchmarkParams.clear();
            String lastBenchmarkName = line.substring(BENCHMARK_NAME_START.length()).trim();
            int lastDot = lastBenchmarkName.lastIndexOf('.');
            lastBenchmarkClass = lastBenchmarkName.substring(0, lastDot);
            lastBenchmarkName = lastBenchmarkName.substring(lastDot + 1);
            this.lastBenchmarkName = lastBenchmarkName;
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
                    lastBenchmarkParams.put(paramName, String.valueOf(paramValue));
                    lastBenchmarkName = lastBenchmarkName.substring(0, lastUnderscore);
                }
            }
        }

        private void consumeParameters(String line) {
            StringTokenizer st = new StringTokenizer(line.substring(PARAMETERS_START.length()).trim(), "(,)");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int equalSign = token.indexOf('=');
                if (equalSign >= 0) {
                    try {
                        String key = token.substring(0, equalSign).trim();
                        lastBenchmarkParams.put(key, token.substring(equalSign + 1).trim());
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

        private void addValueToBenchmarkResult(String s, List<Double> benchmarkData) {
            double value = parseDoubleIgnoringLocale(s);
            benchmarkData.add(value * 1e-6);
        }

        private void flushBenchmark() {
            consumedBenchmarks.add(new JMHBenchmarkResult(lastBenchmarkName,
                    lastBenchmarkClass,
                    lastBenchmarkWarmUpData, lastBenchmarkReleaseData, lastBenchmarkParams));
            lastBenchmarkWarmUpData.clear();
            lastBenchmarkReleaseData.clear();
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
                        addValueToBenchmarkResult(value, lastBenchmarkReleaseData);
                    } else if (line.startsWith(WARMUP_ITERATION)) {
                        StringTokenizer st = new StringTokenizer(line.substring(WARMUP_ITERATION.length()));
                        st.nextToken(); // iteration number
                        String value = st.nextToken();
                        String unit = st.nextToken();
                        if (!unit.equals("us/op")) {
                            throw new UnsupportedOperationException("Cannot work with units other than us/op: '" + unit);
                        }
                        addValueToBenchmarkResult(value, lastBenchmarkWarmUpData);
                    } else if (line.startsWith(FORK_START)) {
                        flushBenchmark();
                    } else if (line.startsWith(RESULT_START)) {
                        flushBenchmark();
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
                    if (line.startsWith(TOTAL_TIME)) {
                        StringTokenizer st = new StringTokenizer(line.substring(TOTAL_TIME.length()));
                        st.nextToken(); // how much
                        st.nextToken(); // s,
                        st.nextToken(); // completed
                        String date = st.nextToken();
                        String time = st.nextToken();
                        this.time = LocalDateTime.of(
                                LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.uuuu")),
                                LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm:ss")));
                        state = 6;
                    }
                    break;
                }
                case 6: {
                    break;
                }
            }
        }

        private JMHLogParser(BufferedReader lines) throws IOException {
            String line;
            while ((line = lines.readLine()) != null) {
                consumeLine(line);
            }
            if (state != 6) {
                throw new IllegalStateException("Unexpected end of input data");
            }
        }
    }

    private static class JMHBenchmarkResult {
        private final String methodName;
        private final String benchmarkClass;
        private final List<Double> warmUpResults;
        private final List<Double> releaseResults;
        private final Map<String, String> params;

        JMHBenchmarkResult(String methodName, String benchmarkClass,
                           List<Double> warmUpResults, List<Double> releaseResults,
                           Map<String, String> params) {
            this.methodName = methodName;
            this.benchmarkClass = benchmarkClass;
            this.warmUpResults = new ArrayList<>(warmUpResults);
            this.releaseResults = new ArrayList<>(releaseResults);
            this.params = new HashMap<>(params);
        }
    }
}
