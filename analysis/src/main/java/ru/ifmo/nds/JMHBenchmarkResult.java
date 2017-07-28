package ru.ifmo.nds;

import java.util.*;

public class JMHBenchmarkResult {
    private final String name;
    private final Map<String, Integer> parameters;
    private final List<Double> results;

    public JMHBenchmarkResult(String name, Map<String, Integer> parameters, List<Double> results) {
        this.name = name;
        this.parameters = new HashMap<>(parameters);
        this.results = new ArrayList<>(results);
    }

    public String getBenchmarkName() {
        return name;
    }

    public Map<String, Integer> getParameters() {
        return new HashMap<>(parameters);
    }

    public List<Double> getResults() {
        return new ArrayList<>(results);
    }
}
