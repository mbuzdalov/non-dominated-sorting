package ru.ifmo.nds.rundb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a record which stores the identifying information about the benchmark along with its results.
 */
public class Record {
    private final String algorithmID;
    private final String datasetID;
    private final String measurementMethod;
    private final String measurementAuthor;
    private final LocalDateTime measurementTime;
    private final String cpuModelName;
    private final String javaRuntimeVersion;
    private final List<Double> measurements;
    private final String comment;

    public Record(String algorithmID,
                  String datasetID,
                  String measurementMethod,
                  String measurementAuthor,
                  LocalDateTime measurementTime,
                  String cpuModelName,
                  String javaRuntimeVersion,
                  List<Double> measurements,
                  String comment) {
        this.algorithmID = algorithmID;
        this.datasetID = datasetID;
        this.measurementMethod = measurementMethod;
        this.measurementAuthor = measurementAuthor;
        this.measurementTime = measurementTime;
        this.cpuModelName = cpuModelName;
        this.javaRuntimeVersion = javaRuntimeVersion;
        this.measurements = new ArrayList<>(measurements);
        this.comment = comment;
    }

    public String getAlgorithmId() {
        return algorithmID;
    }

    public String getDatasetId() {
        return datasetID;
    }

    public String getMeasurementMethod() {
        return measurementMethod;
    }

    public String getMeasurementAuthor() {
        return measurementAuthor;
    }

    public LocalDateTime getMeasurementTime() {
        return measurementTime;
    }

    public String getCpuModelName() {
        return cpuModelName;
    }

    public String getJavaRuntimeVersion() {
        return javaRuntimeVersion;
    }

    public String getComment() {
        return comment;
    }

    public List<Double> getMeasurements() {
        return Collections.unmodifiableList(measurements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        return algorithmID.equals(record.algorithmID)
                && datasetID.equals(record.datasetID)
                && measurementMethod.equals(record.measurementMethod)
                && measurementAuthor.equals(record.measurementAuthor)
                && measurementTime.equals(record.measurementTime)
                && cpuModelName.equals(record.cpuModelName)
                && javaRuntimeVersion.equals(record.javaRuntimeVersion)
                && measurements.equals(record.measurements)
                && comment.equals(record.comment);
    }

    @Override
    public int hashCode() {
        int result;
        result = algorithmID.hashCode();
        result = 31 * result + datasetID.hashCode();
        result = 31 * result + measurementMethod.hashCode();
        result = 31 * result + measurementAuthor.hashCode();
        result = 31 * result + measurementTime.hashCode();
        result = 31 * result + cpuModelName.hashCode();
        result = 31 * result + javaRuntimeVersion.hashCode();
        result = 31 * result + measurements.hashCode();
        result = 31 * result + comment.hashCode();
        return result;
    }
}
