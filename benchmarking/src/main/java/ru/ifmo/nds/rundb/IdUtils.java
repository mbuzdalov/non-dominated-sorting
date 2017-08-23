package ru.ifmo.nds.rundb;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public final class IdUtils {
    private IdUtils() {}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static Set<String> getPossibleFactors(String s) {
        Set<String> rv = new HashSet<>();
        StringTokenizer st = new StringTokenizer(s, ".");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int start = 0;
            while (start < token.length() && !Character.isDigit(token.charAt(start))) {
                ++start;
            }
            boolean isNumber = true;
            for (int i = start; isNumber && i < token.length(); ++i) {
                isNumber = Character.isDigit(token.charAt(i));
            }
            if (isNumber) {
                try {
                    Long.parseLong(token.substring(start));
                    rv.add(token.substring(0, start));
                } catch (NumberFormatException ignored) {
                    // this token cannot be a factor
                }
            }
        }
        return rv;
    }

    public static String factorize(String s, String factor) {
        StringBuilder rv = new StringBuilder();
        boolean first = true;
        StringTokenizer st = new StringTokenizer(s, ".");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (!token.startsWith(factor) || (token.length() != factor.length() && !Character.isDigit(token.charAt(factor.length())))) {
                if (first) {
                    first = false;
                } else {
                    rv.append('.');
                }
                rv.append(token);
            }
        }
        return rv.toString();
    }

    public static int extract(String s, String factor) {
        StringTokenizer st = new StringTokenizer(s, ".");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.startsWith(factor)) {
                try {
                    return Integer.parseInt(token.substring(factor.length()));
                } catch (NumberFormatException ex) {
                    // continue
                }
            }
        }
        throw new IllegalArgumentException("Cannot extract '" + factor + "' from string '" + s + "'");
    }


    public static Comparator<String> getLexicographicalIdComparator() {
        return ID_LEX_COMPARATOR;
    }

    private static final Comparator<String> ID_LEX_COMPARATOR = (o1, o2) -> {
        StringTokenizer s1 = new StringTokenizer(o1, ".");
        StringTokenizer s2 = new StringTokenizer(o2, ".");
        while (true) {
            if (s1.hasMoreTokens() && s2.hasMoreTokens()) {
                String t1 = s1.nextToken();
                String t2 = s2.nextToken();
                int l1 = t1.length() - 1, l2 = t2.length() - 1;
                while (l1 >= 0 && Character.isDigit(t1.charAt(l1))) --l1;
                while (l2 >= 0 && Character.isDigit(t2.charAt(l2))) --l2;
                ++l1;
                ++l2;
                if (l1 != t1.length() && l2 != t2.length()) {
                    String prefix1 = t1.substring(0, l1);
                    String prefix2 = t2.substring(0, l2);
                    int prefixCmp = prefix1.compareTo(prefix2);
                    if (prefixCmp != 0) {
                        return prefixCmp;
                    }
                    long suffix1 = Long.parseLong(t1.substring(l1));
                    long suffix2 = Long.parseLong(t2.substring(l2));
                    if (suffix1 != suffix2) {
                        return Long.compare(suffix1, suffix2);
                    }
                } else {
                    int cmp = t1.compareTo(t2);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
            } else if (s1.hasMoreTokens()) {
                return -1;
            } else if (s2.hasMoreTokens()) {
                return 1;
            } else {
                return 0;
            }
        }
    };
}
