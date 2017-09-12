package ru.ifmo.nds.cli;

import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.beust.jcommander.converters.BaseConverter;
import static ru.ifmo.nds.cli.FilterHelpers.*;

import ru.ifmo.nds.rundb.IdUtils;

class DatasetFilterStringConverter extends BaseConverter<Predicate<String>> {
    public DatasetFilterStringConverter(String optionName) {
        super(optionName);
    }

    private IllegalArgumentException newHellKnowsWhatException(String value) {
        return new IllegalArgumentException("Cannot parse filter '" + value
                + "': Filter does not start with '~=', '==' or '!=', thus it must be a factored filter, "
                + "where one of the following operations are expected: "
                + operators.stream().map(o -> o.text).collect(Collectors.toList()));
    }

    @Override
    public Predicate<String> convert(String value) {
        if (value.startsWith("~=")) {
            Pattern p = Pattern.compile(value.substring(2));
            return r -> p.matcher(r).find();
        } else if (value.startsWith("==")) {
            String match = value.substring(2);
            return r -> r.equals(match);
        } else if (value.startsWith("!=")) {
            String match = value.substring(2);
            return r -> !r.equals(match);
        } else {
            int firstNonIdentifier = findNonIdentifier(value, 0);
            if (firstNonIdentifier >= 0) {
                String factor = value.substring(0, firstNonIdentifier);
                for (Operator op : operators) {
                    if (value.startsWith(op.text, firstNonIdentifier)) {
                        int match = Integer.parseInt(value.substring(firstNonIdentifier + op.text.length()).trim());
                        return r -> {
                            OptionalInt fv = IdUtils.extract(r, factor);
                            return fv.isPresent() && op.operator.test(fv.getAsInt(), match);
                        };
                    }
                }
            }
            throw newHellKnowsWhatException(value);
        }
    }
}
