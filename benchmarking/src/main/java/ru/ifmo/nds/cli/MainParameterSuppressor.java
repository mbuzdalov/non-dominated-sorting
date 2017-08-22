package ru.ifmo.nds.cli;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public final class MainParameterSuppressor implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        throw new ParameterException("Error: Unknown parameter name '" + value + "'.");
    }

    @SuppressWarnings("unused")
    @Parameter(validateWith = MainParameterSuppressor.class)
    private String mainParameterSuppressor;
}
