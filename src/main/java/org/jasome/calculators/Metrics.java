package org.jasome.calculators;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Metrics extends HashMap<String, Metric> {

    public static final Metrics EMPTY = new Metrics(ImmutableMap.of());

    public Metrics() {
        super();
    }

    private Metrics(Map<String, Metric> collection) {
        super(collection);
    }

    public static Metrics of(Metric result) {
        Map<String, Metric> map = new HashMap<String, Metric>();
        map.put(result.getName(), result);
        return new Metrics(map);
    }

    public static Metrics of(String name, String description, BigDecimal value) {
        return Metrics.of(new Metric(name, description, value));
    }

    public static Metrics.Builder builder() {
        return new Metrics.Builder();
    }

    public static class Builder {
        private Map<String, Metric> metrics;

        public Builder() {
            metrics = new HashMap<String, Metric>();
        }

        public Builder with(String name, String description, BigDecimal value) {
            metrics.put(name, new Metric(name, description, value));
            return this;
        }

        public Builder with(String name, String description, long value) {
            return with(name, description, new BigDecimal(value));
        }

        public Metrics build() {
            return new Metrics(metrics);
        }

    }
}
