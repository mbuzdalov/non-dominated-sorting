package ru.ifmo.nds.ndt;

public class Split {
    int coordinate;
    double value;
    Split good, weak;

    void initialize(int coordinate, double value, Split good, Split weak) {
        this.coordinate = coordinate;
        this.value = value;
        this.good = good;
        this.weak = weak;
    }

    static final Split NULL_MAX_DEPTH = new Split();
    static final Split NULL_POINTS = new Split();
}
