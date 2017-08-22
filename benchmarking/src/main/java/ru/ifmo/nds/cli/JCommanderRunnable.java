package ru.ifmo.nds.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

abstract class JCommanderRunnable {
    protected abstract void run() throws CLIWrapperException;

    public static void run(JCommanderRunnable object, String[] args) {
        JCommander parser = JCommander.newBuilder()
                .programName(object.getClass().getName())
                .addObject(object)
                .acceptUnknownOptions(false)
                .allowParameterOverwriting(false)
                .build();
        try {
            parser.parse(args);
            object.run();
        } catch (ParameterException ex) {
            System.out.println(ex.getLocalizedMessage());
            System.out.println();
            parser.usage();
            System.exit(1);
        } catch (CLIWrapperException ex) {
            System.out.println(ex.getLocalizedMessage());
            System.out.println();
            if (ex.getCause() != null) {
                System.out.print("Reason: ");
                ex.getCause().printStackTrace(System.out);
                System.out.println();
            }
            parser.usage();
            System.exit(1);
        }

    }
}
