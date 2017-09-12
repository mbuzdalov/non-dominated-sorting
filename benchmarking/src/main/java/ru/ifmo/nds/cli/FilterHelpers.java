package ru.ifmo.nds.cli;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

final class FilterHelpers {
    private FilterHelpers() {}

    static int findNonIdentifier(String s, int from) {
        int firstNonIdentifier = from;
        while (firstNonIdentifier < s.length() && Character.isJavaIdentifierPart(s.charAt(firstNonIdentifier))) {
            ++firstNonIdentifier;
        }
        return firstNonIdentifier;
    }

    static final class Operator {
        final String text;
        final BiPredicate<Integer, Integer> operator;

        private Operator(String text, BiPredicate<Integer, Integer> operator) {
            this.text = text;
            this.operator = operator;
        }
    }

    static final List<Operator> operators = Collections.unmodifiableList(Arrays.asList(
            new Operator("==", Integer::equals),
            new Operator("!=", (l, r) -> !l.equals(r)),
            new Operator("<=", (l, r) -> l <= r),
            new Operator(">=", (l, r) -> l >= r),
            new Operator("<", (l, r) -> l < r),
            new Operator(">", (l, r) -> l > r)
    ));
}
