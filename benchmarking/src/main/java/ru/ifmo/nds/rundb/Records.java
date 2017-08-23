package ru.ifmo.nds.rundb;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public final class Records {
    // JSON fields
    private static final String ALGORITHM_ID = "algorithmId";
    private static final String DATASET_ID = "datasetId";
    private static final String MEASUREMENT_METHOD = "measurementMethod";
    private static final String MEASUREMENT_AUTHOR = "measurementAuthor";
    private static final String MEASUREMENT_TIME = "measurementTime";
    private static final String CPU_MODEL_NAME = "cpuModelName";
    private static final String JAVA_RUNTIME_VERSION = "javaRuntimeVersion";
    private static final String MEASUREMENTS = "measurements";
    @Deprecated
    private static final String RELEASE_MEASUREMENTS = "releaseMeasurements";
    private static final String COMMENT = "comment";

    private static final List<String> ALL_NAMES = Arrays.asList(
            ALGORITHM_ID, DATASET_ID, MEASUREMENT_METHOD, MEASUREMENT_AUTHOR, MEASUREMENT_TIME,
            CPU_MODEL_NAME, JAVA_RUNTIME_VERSION, MEASUREMENTS, COMMENT
    );

    private Records() {}

    public static void saveToFile(Collection<? extends Record> records, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            saveToWriter(records, writer);
        }
    }

    public static void saveToWriter(Collection<? extends Record> records, Writer writer) throws IOException {
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.beginArray();
        for (Record record : records) {
            jsonWriter.beginObject();
            jsonWriter.name(ALGORITHM_ID).value(record.getAlgorithmId());
            jsonWriter.name(DATASET_ID).value(record.getDatasetId());
            jsonWriter.name(MEASUREMENT_METHOD).value(record.getMeasurementMethod());
            jsonWriter.name(MEASUREMENT_AUTHOR).value(record.getMeasurementAuthor());
            jsonWriter.name(MEASUREMENT_TIME).value(record.getMeasurementTime().toString());
            jsonWriter.name(CPU_MODEL_NAME).value(record.getCpuModelName());
            jsonWriter.name(JAVA_RUNTIME_VERSION).value(record.getJavaRuntimeVersion());
            jsonWriter.name(MEASUREMENTS).beginArray();
            for (double measurement : record.getMeasurements()) {
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
                    case JAVA_RUNTIME_VERSION:
                        stringFields.put(key, jsonReader.nextString());
                        break;
                    case MEASUREMENT_TIME:
                        timeFields.put(key, LocalDateTime.parse(jsonReader.nextString()));
                        break;
                    case MEASUREMENTS:
                    case RELEASE_MEASUREMENTS: {
                        if (key.equals(RELEASE_MEASUREMENTS)) {
                            System.out.println("[warning] 'releaseMeasurements' is deprecated, use 'measurements'.");
                        }
                        List<Double> list = new ArrayList<>();
                        jsonReader.beginArray();
                        while (jsonReader.hasNext()) {
                            list.add(jsonReader.nextDouble());
                        }
                        jsonReader.endArray();
                        doubleListFields.put(MEASUREMENTS, list);
                        break;
                    }
                    default:
                        throw new IOException("In record description, stream contains JSON name '" + key
                                + "' which is not allowed");
                }
            }
            jsonReader.endObject();

            for (String s : ALL_NAMES) {
                if (!stringFields.containsKey(s) && !timeFields.containsKey(s) && !doubleListFields.containsKey(s)) {
                    throw new IOException("In record description, JSON name '" + s + "' is missing");
                }
            }
            rv.add(new Record(stringFields.get(ALGORITHM_ID), stringFields.get(DATASET_ID),
                    stringFields.get(MEASUREMENT_METHOD), stringFields.get(MEASUREMENT_AUTHOR),
                    timeFields.get(MEASUREMENT_TIME), stringFields.get(CPU_MODEL_NAME),
                    stringFields.get(JAVA_RUNTIME_VERSION), doubleListFields.get(MEASUREMENTS),
                    stringFields.get(COMMENT)));
        }
        jsonReader.endArray();
        return rv;
    }
}
