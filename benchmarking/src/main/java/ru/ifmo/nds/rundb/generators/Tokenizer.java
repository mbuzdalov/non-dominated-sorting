package ru.ifmo.nds.rundb.generators;

import java.util.StringTokenizer;

final class Tokenizer {
    private final StringTokenizer tokenizer;
    private final String data;
    private int tokenCount = 0;

    Tokenizer(String data) {
        this.data = data;
        tokenizer = new StringTokenizer(data, ".");
    }

    void expectNext(String expected, String errorPrefix) {
        if (!tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException(errorPrefix + " '" + data
                    + "'. Reason: token #" + tokenCount + " does not exist.");
        }
        String actual = tokenizer.nextToken();
        ++tokenCount;
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(errorPrefix + " '" + data
                    + "'. Reason: token #" + tokenCount + " is expected to be '" + expected
                    + "', but is '" + actual + "'.");
        }
    }

    int parseInt(String errorPrefix) {
        if (!tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException(errorPrefix + " '" + data
                    + "'. Reason: token #" + tokenCount + " does not exist.");
        }
        String actual = tokenizer.nextToken();
        ++tokenCount;
        try {
            return Integer.parseInt(actual);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorPrefix + " '" + data
                    + "'. Reason: token #" + tokenCount + " is expected to contain an int, but is '" + actual + "'.");
        }
    }

    int parseIntWithPrefix(String expectedPrefix, String errorPrefix) {
        if (!tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException(errorPrefix + " '" + data
                    + "'. Reason: token #" + tokenCount + " does not exist.");
        }
        String actual = tokenizer.nextToken();
        ++tokenCount;
        if (!actual.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException(errorPrefix + " '" + data
                    + "'. Reason: token #" + tokenCount + " is expected to start with '" + expectedPrefix
                    + "', but is '" + actual + "'.");
        }
        try {
            return Integer.parseInt(actual.substring(expectedPrefix.length()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorPrefix + " '" + data
                    + "'. Reason: token #" + tokenCount + " is expected to contain an int after '" + expectedPrefix
                    + "', but is '" + actual + "'.");
        }
    }

    void expectFinish(String errorPrefix) {
        if (tokenizer.hasMoreTokens()) {
            throw new IllegalArgumentException(errorPrefix + " '" + data + "'. Reason: there should be no more than "
                    + tokenCount + " tokens.");
        }
    }
}
