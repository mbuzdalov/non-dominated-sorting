package ru.ifmo;

import ru.ifmo.bos.Proteek;

public class BestOrderSort {
    private BestOrderSort() {}

    private static final NonDominatedSortingFactory PROTEEK = Proteek::new;

    public static NonDominatedSortingFactory getProteekImplementation() {
        return PROTEEK;
    }
}
