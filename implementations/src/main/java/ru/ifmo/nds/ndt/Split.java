package ru.ifmo.nds.ndt;

public final class Split {
    public int coordinate;
    public double value;
    public Split good, weak;

    public void initialize(int coordinate, double value, Split good, Split weak) {
        this.coordinate = coordinate;
        this.value = value;
        this.good = good;
        this.weak = weak;
    }
}
