package ru.ifmo.nds.rundb.tests;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

public class RecordsTests {
    private String randomString(Random random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append((char) (random.nextInt(26) + 'a'));
        }
        return sb.toString();
    }

    private Record randomRecord(Random random) {
        return new Record(
                randomString(random),
                randomString(random),
                randomString(random),
                randomString(random),
                LocalDateTime.now(),
                randomString(random),
                randomString(random),
                random.doubles(random.nextInt(5)).boxed().collect(Collectors.toList()),
                randomString(random)
        );
    }

    @Test
    public void smokeTest() throws IOException {
        Random random = new Random(8386457);
        List<Record> records = Stream.generate(() -> randomRecord(random)).limit(129).collect(Collectors.toList());
        StringWriter writer = new StringWriter();
        Records.saveToWriter(records, writer);
        String firstWrite = writer.toString();
        StringReader reader = new StringReader(firstWrite);
        List<Record> readRecords = Records.loadFromReader(reader);
        Assert.assertEquals(records, readRecords);
        StringWriter secondWriter = new StringWriter();
        Records.saveToWriter(readRecords, secondWriter);
        Assert.assertEquals(firstWrite, secondWriter.toString());
    }
}
