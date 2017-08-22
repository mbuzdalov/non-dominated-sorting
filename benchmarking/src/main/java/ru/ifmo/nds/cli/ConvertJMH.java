package ru.ifmo.nds.cli;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.beust.jcommander.Parameter;

import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

public class ConvertJMH extends JCommanderRunnable {
    private ConvertJMH() {}

    @SuppressWarnings("unused")
    @Parameter(validateWith = MainParameterSuppressor.class)
    private String mainParameterSuppressor;

    @Parameter(names = "--input", required = true, description = "Specify the file with JMH logs to convert.")
    private String inputFileName;

    @Parameter(names = "--output", required = true, description = "Specify the file to write benchmark results (JSON format).")
    private String outputFileName;

    @Parameter(names = "--java-runtime", required = true, description = "Specify the Java runtime version.")
    private String javaRuntimeVersion;

    @Parameter(names = "--cpu-name", required = true, description = "Specify the CPU name.")
    private String cpuName;

    @Parameter(names = "--author", required = true, description = "Specify the author of the measurement.")
    private String author;

    @Parameter(names = "--comment", required = true, description = "Specify the comment to the measurement.")
    private String comment;

    @Override
    protected void run() throws CLIWrapperException {
        List<Record> records;
        try (Reader reader = Files.newBufferedReader(Paths.get(inputFileName))) {
            records = Records.parseJMHRun(reader,
                    author,
                    cpuName,
                    javaRuntimeVersion,
                    comment);
        } catch (Throwable ex) {
            throw new CLIWrapperException("Error: could not parse JMH input from file '" + inputFileName + "'.", ex);
        }
        try {
            Records.saveToFile(records, Paths.get(outputFileName));
        } catch (Throwable ex) {
            throw new CLIWrapperException("Error: could not write benchmark results to file '" + outputFileName + "'.", ex);
        }
    }

    public static void main(String[] args) {
        JCommanderRunnable.run(new ConvertJMH(), args);
    }
}
