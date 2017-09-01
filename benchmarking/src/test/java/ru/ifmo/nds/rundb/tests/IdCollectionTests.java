package ru.ifmo.nds.rundb.tests;

import org.junit.Assert;
import org.junit.Test;

import ru.ifmo.nds.IdCollection;

public class IdCollectionTests {
    @Test
    public void idCollectionConstructs() {
        Assert.assertTrue(IdCollection.getAllDatasetIds().size() > 0);
    }
}
