package ru.ifmo.nds.jmh.main;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public final class PatchWithOSHI {
    private PatchWithOSHI() {
        throw new UnsupportedOperationException("PatchWithOSHI is a static-only class");
    }

    private static final String gitCommand = System.getProperty("nds.git.command", "git rev-parse HEAD");

    private static String getCommitOrNull() {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(gitCommand.split(" "));
            Path gitOutput = Files.createTempFile("ngp-git-temporary", "");
            builder.redirectOutput(gitOutput.toFile());
            int exitCode = builder.start().waitFor();
            if (exitCode != 0) {
                return null;
            }
            String result = Files.lines(gitOutput).findFirst().orElse(null);
            Files.delete(gitOutput);
            return result;
        } catch (IOException | InterruptedException ex) {
            System.err.println("Exception while trying to get the current git commit:");
            ex.printStackTrace(System.err);
            return null;
        }
    }

    private static String toJsonString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, l = s.length(); i < l; ++i) {
            char ch = s.charAt(i);
            if (ch >= ' ') {
                if (ch == '"') {
                    sb.append('\'');
                } else {
                    sb.append(ch);
                }
            }
        }
        return sb.toString();
    }

    static void patch(File source, File target) throws IOException {
        try (PrintStream out = new PrintStream(target)) {
            out.println("{");
            out.println("  \"finish-time\" : \"" + toJsonString(LocalDateTime.now().toString()) + "\",");

            String gitCommit = getCommitOrNull();
            if (gitCommit == null) {
                out.println("  \"commit-hash\" : null,");
            } else {
                out.println("  \"commit-hash\" : \"" + gitCommit + "\",");
            }

            SystemInfo info = new SystemInfo();
            out.println("  \"hardware\" : {");
            HardwareAbstractionLayer hw = info.getHardware();
            out.println("    \"cpu\" : {");
            CentralProcessor cpu = hw.getProcessor();
            out.println("      \"vendor\" : \"" + toJsonString(cpu.getVendor()) + "\",");
            out.println("      \"model\" : \"" + toJsonString(cpu.getModel()) + "\",");
            out.println("      \"family\" : \"" + toJsonString(cpu.getFamily()) + "\",");
            out.println("      \"declared-frequency\" : " + cpu.getVendorFreq() + ",");
            out.println("      \"full-name\" : \"" + toJsonString(cpu.getName()) + "\"");
            out.println("    }");
            out.println("  },");
            out.println("  \"os\" : {");
            OperatingSystem os = info.getOperatingSystem();
            out.println("     \"family\" : \"" + toJsonString(os.getFamily()) + "\",");
            out.println("     \"manufacturer\" : \"" + toJsonString(os.getManufacturer()) + "\",");
            out.println("     \"bitness\" : " + os.getBitness() + ",");
            out.println("     \"version\" : {");
            OperatingSystemVersion osVersion = os.getVersion();
            out.println("       \"version-string\" : \"" + toJsonString(osVersion.getVersion()) + "\",");
            out.println("       \"codename\" : \"" + toJsonString(osVersion.getCodeName()) + "\",");
            out.println("       \"build\" : \"" + toJsonString(osVersion.getBuildNumber()) + "\"");
            out.println("     }");
            out.println("  },");
            out.println("  \"data\": ");
            Files.copy(source.toPath(), out);
            out.println("}");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: " + PatchWithOSHI.class.getName() + " <source file> <target file>");
            System.exit(1);
        } else {
            patch(new File(args[0]), new File(args[1]));
        }
    }
}
