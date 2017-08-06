package ru.ifmo;

import ru.ifmo.bos.Improved;
import ru.ifmo.bos.Proteek;

public class BestOrderSort {
    private BestOrderSort() {}

    private static final NonDominatedSortingFactory PROTEEK = Proteek::new;
    private static final NonDominatedSortingFactory IMPROVED = Improved::new;

    public static NonDominatedSortingFactory getProteekImplementation() {
        return PROTEEK;
    }
    public static NonDominatedSortingFactory getImprovedImplementation() {
        return IMPROVED;
    }
}
