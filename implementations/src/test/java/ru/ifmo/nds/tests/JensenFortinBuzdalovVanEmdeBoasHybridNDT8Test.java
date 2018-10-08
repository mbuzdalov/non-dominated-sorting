package ru.ifmo.nds.tests;

import ru.ifmo.nds.JensenFortinBuzdalov;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class JensenFortinBuzdalovVanEmdeBoasHybridNDT8Test extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return JensenFortinBuzdalov.getVanEmdeBoasHybridNDTImplementation(8);
    }
}
