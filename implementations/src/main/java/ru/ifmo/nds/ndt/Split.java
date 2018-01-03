package ru.ifmo.nds.ndt;

public final class Split {
    public final int coordinate;
    public final double value;
    public final Split good, weak;

    public Split(int coordinate, double value, Split good, Split weak) {
        this.coordinate = coordinate;
        this.value = value;
        this.good = good;
        this.weak = weak;
    }
}
