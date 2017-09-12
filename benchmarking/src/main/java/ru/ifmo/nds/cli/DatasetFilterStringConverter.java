package ru.ifmo.nds.cli;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.beust.jcommander.converters.BaseConverter;

import ru.ifmo.nds.rundb.Dataset;

class DatasetFilterStringConverter extends BaseConverter<Predicate<String>> {
    public DatasetFilterStringConverter(String optionName) {
        super(optionName);
    }

    @Override
    public Predicate<String> convert(String value) {
        Pattern p = Pattern.compile(value);
        return r -> p.matcher(r).find();
    }
}
