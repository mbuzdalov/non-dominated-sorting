package ru.ifmo.nds.cli;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.beust.jcommander.Parameter;

import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

public class Merge extends JCommanderRunnable {
    private Merge() {}

    @Parameter(names = "--input", variableArity = true, required = true, description = "Specify input files to merge.")
    private List<String> inputFiles;

    @Parameter(names = "--output", required = true, description = "Specify output file to put merge results into.")
    private String outputFile;

    @Override
    protected void run() throws CLIWrapperException {
        Set<Record> merge = new HashSet<>();
        int expectedSize = 0;
        for (String inputFile : inputFiles) {
            try {
                List<Record> fileRecords = Records.loadFromFile(Paths.get(inputFile));
                expectedSize += fileRecords.size();
                merge.addAll(fileRecords);
            } catch (IOException ex) {
                throw new CLIWrapperException("Error reading input file '" + inputFile + "'.", ex);
            }
        }
        if (merge.size() != expectedSize) {
            System.out.println("[warning] " + (expectedSize - merge.size()) + " duplicates found and removed.");
        }
        try {
            Records.saveToFile(merge, Paths.get(outputFile));
        } catch (IOException ex) {
            throw new CLIWrapperException("Error writing output file '" + outputFile + "'.", ex);
        }
    }

    public static void main(String[] args) {
        JCommanderRunnable.run(new Merge(), args);
    }
}
