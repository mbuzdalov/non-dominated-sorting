package ru.ifmo.nds.cli;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.beust.jcommander.converters.BaseConverter;

import ru.ifmo.nds.rundb.IdUtils;
import ru.ifmo.nds.rundb.Record;

class FilterStringConverter extends BaseConverter<Predicate<Record>> {
    public FilterStringConverter(String optionName) {
        super(optionName);
    }

    // Pattern: field op value - for field value matching
    // Pattern: field/n op value - for field value factored by n matching

    private int findNonIdentifier(String s, int from) {
        int firstNonIdentifier = from;
        while (firstNonIdentifier < s.length() && Character.isJavaIdentifierPart(s.charAt(firstNonIdentifier))) {
            ++firstNonIdentifier;
        }
        return firstNonIdentifier;
    }

    private static final class Operator {
        private final String text;
        private final BiPredicate<Integer, Integer> operator;

        private Operator(String text, BiPredicate<Integer, Integer> operator) {
            this.text = text;
            this.operator = operator;
        }
    }

    private static final Operator[] operators = {
            new Operator("==", Integer::equals),
            new Operator("!=", (l, r) -> !l.equals(r)),
            new Operator("<=", (l, r) -> l <= r),
            new Operator(">=", (l, r) -> l >= r),
            new Operator("<", (l, r) -> l < r),
            new Operator(">", (l, r) -> l > r),
    };

    @Override
    public Predicate<Record> convert(String value) {
        int firstNonIdentifier = findNonIdentifier(value, 0);
        if (firstNonIdentifier == value.length()) {
            throw new IllegalArgumentException("Filter must contain an operation");
        }
        String fieldName = value.substring(0, firstNonIdentifier);
        Record.FieldAccessor accessor = Record.FieldAccessor.valueOf(fieldName);
        if (value.charAt(firstNonIdentifier) == '/') {
            // Factor
            int nextNonIdentifier = findNonIdentifier(value, firstNonIdentifier + 1);
            if (nextNonIdentifier == value.length()) {
                throw new IllegalArgumentException("Filter must contain an operation after a factor");
            }
            String factor = value.substring(firstNonIdentifier + 1, nextNonIdentifier);
            if (factor.length() == 0) {
                throw new IllegalArgumentException("Filter must contain a non-empty factor operation after the factor symbol");
            }
            for (Operator op : operators) {
                if (value.startsWith(op.text, nextNonIdentifier)) {
                    int match = Integer.parseInt(value.substring(nextNonIdentifier + op.text.length()).trim());
                    return r -> {
                        OptionalInt fv = IdUtils.extract(accessor.extractField(r).toString(), factor);
                        return fv.isPresent() && op.operator.test(fv.getAsInt(), match);
                    };
                }
            }
            throw new IllegalArgumentException("Cannot parse filter '" + value
                    + "': With a factored filter, one of the following operations are expected: "
                    + Arrays.stream(operators).map(o -> o.text).collect(Collectors.toList()));
        } else {
            // Direct value
            if (value.startsWith("==", firstNonIdentifier)) {
                // String equality
                String match = value.substring(firstNonIdentifier + 2);
                return r -> accessor.extractField(r).toString().equals(match);
            } else if (value.startsWith("!=", firstNonIdentifier)) {
                // String equality
                String match = value.substring(firstNonIdentifier + 2);
                return r -> !accessor.extractField(r).toString().equals(match);
            } else if (value.startsWith("~=", firstNonIdentifier)) {
                // Regex matching
                String regex = value.substring(firstNonIdentifier + 2);
                Pattern p = Pattern.compile(regex);
                return r -> p.matcher(accessor.extractField(r).toString()).find();
            } else {
                throw new IllegalArgumentException("Cannot parse filter '" + value
                        + "': Expected one of [==, !=, ~=] for non-factored operation");
            }
        }
    }
}
