package ru.ifmo.nds.cli;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.beust.jcommander.Parameter;

import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

public class Edit extends JCommanderRunnable {
    @Parameter(names = "--input", variableArity = true, required = true, description = "Specify input files to merge.")
    private List<String> inputFiles;

    @Parameter(names = "--remove",
            variableArity = true,
            description = "Specify which records to remove.",
            converter = FilterStringConverter.class)
    private List<Predicate<Record>> removeFilters;

    @Parameter(names = "--retain",
            variableArity = true,
            description = "Specify which records to retain.",
            converter = FilterStringConverter.class)
    private List<Predicate<Record>> retainFilters;

    @Parameter(names = "--output", description = "Specify output file to put merge results into.")
    private String outputFile;

    @Parameter(names = "--append", description = "Append to the output file instead of overwriting it.")
    private boolean shouldAppendToOutput;

    @Parameter(names = "--list-fields", description = "List all values for the specified fields.")
    private Record.FieldAccessor listFields;

    @Parameter(names = "--list-distinct-fields", description = "List all distinct values for the specified fields.")
    private Record.FieldAccessor listDistinctFields;

    @Override
    protected void run() throws CLIWrapperException {
        List<Record> records = new ArrayList<>();
        for (String file : inputFiles) {
            try {
                records.addAll(Records.loadFromFile(Paths.get(file)));
            } catch (IOException ex) {
                throw new CLIWrapperException("Cannot load input file '" + file + "'.", ex);
            }
        }

        if (removeFilters != null) {
            for (Predicate<Record> filter : removeFilters) {
                List<Record> newRecords = new ArrayList<>(records.size());
                for (Record r : records) {
                    if (!filter.test(r)) {
                        newRecords.add(r);
                    }
                }
                records = newRecords;
            }
        }

        if (retainFilters != null) {
            for (Predicate<Record> filter : retainFilters) {
                List<Record> newRecords = new ArrayList<>(records.size());
                for (Record r : records) {
                    if (filter.test(r)) {
                        newRecords.add(r);
                    }
                }
                records = newRecords;
            }
        }

        if (listFields != null) {
            System.out.println("Printing all values of field '" + listFields.name() + "':");
            for (Record r : records) {
                System.out.println("  " + listFields.extractField(r));
            }
        }

        if (listDistinctFields != null) {
            System.out.println("Printing all distinct values of field '" + listDistinctFields.name() + "':");
            Set<Object> values = new HashSet<>();
            for (Record r : records) {
                values.add(listDistinctFields.extractField(r));
            }
            for (Object v : values) {
                System.out.println("  " + v);
            }
        }

        if (outputFile != null) {
            List<Record> output = new ArrayList<>();

            if (shouldAppendToOutput) {
                try {
                    output.addAll(Records.loadFromFile(Paths.get(outputFile)));
                } catch (IOException ex) {
                    throw new CLIWrapperException("Cannot load existing records from output file '" + outputFile + "'", ex);
                }
            }

            output.addAll(records);

            try {
                Records.saveToFile(output, Paths.get(outputFile));
            } catch (IOException ex) {
                throw new CLIWrapperException("Cannot write to output file '" + outputFile + "'", ex);
            }
        }
    }

    public static void main(String[] args) {
        JCommanderRunnable.run(new Edit(), args);
    }
}
