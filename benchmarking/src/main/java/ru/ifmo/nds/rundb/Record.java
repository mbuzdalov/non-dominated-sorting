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
    private final double cpuFrequency;
    private final String cpuModelName;
    private final List<Double> warmUpMeasurements;
    private final List<Double> releaseMeasurements;
    private final String comment;

    public Record(String algorithmID,
                  String datasetID,
                  String measurementMethod,
                  String measurementAuthor,
                  LocalDateTime measurementTime,
                  double cpuFrequency,
                  String cpuModelName,
                  List<Double> warmUpMeasurements,
                  List<Double> releaseMeasurements,
                  String comment) {
        this.algorithmID = algorithmID;
        this.datasetID = datasetID;
        this.measurementMethod = measurementMethod;
        this.measurementAuthor = measurementAuthor;
        this.measurementTime = measurementTime;
        this.cpuFrequency = cpuFrequency;
        this.cpuModelName = cpuModelName;
        this.warmUpMeasurements = Collections.unmodifiableList(new ArrayList<>(warmUpMeasurements));
        this.releaseMeasurements = Collections.unmodifiableList(new ArrayList<>(releaseMeasurements));
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

    public double getCpuFrequency() {
        return cpuFrequency;
    }

    public String getCpuModelName() {
        return cpuModelName;
    }

    public String getComment() {
        return comment;
    }

    public List<Double> getWarmUpMeasurements() {
        return warmUpMeasurements;
    }

    public List<Double> getReleaseMeasurements() {
        return releaseMeasurements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        return Double.compare(record.cpuFrequency, cpuFrequency) == 0
                && algorithmID.equals(record.algorithmID)
                && datasetID.equals(record.datasetID)
                && measurementMethod.equals(record.measurementMethod)
                && measurementAuthor.equals(record.measurementAuthor)
                && measurementTime.equals(record.measurementTime)
                && cpuModelName.equals(record.cpuModelName)
                && warmUpMeasurements.equals(record.warmUpMeasurements)
                && releaseMeasurements.equals(record.releaseMeasurements)
                && comment.equals(record.comment);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = algorithmID.hashCode();
        result = 31 * result + datasetID.hashCode();
        result = 31 * result + measurementMethod.hashCode();
        result = 31 * result + measurementAuthor.hashCode();
        result = 31 * result + measurementTime.hashCode();
        temp = Double.doubleToLongBits(cpuFrequency);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + cpuModelName.hashCode();
        result = 31 * result + warmUpMeasurements.hashCode();
        result = 31 * result + releaseMeasurements.hashCode();
        result = 31 * result + comment.hashCode();
        return result;
    }
}
