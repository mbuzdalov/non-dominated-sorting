package ru.ifmo.nds.cli;

import java.util.Objects;

class CLIWrapperException extends Exception {
    CLIWrapperException(String message, Throwable cause) {
        super(Objects.requireNonNull(message), cause);
    }
}
