package org.jasome.calculators;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Metrics extends HashMap<String, Metric> {

    public static final HashSet<Metric> EMPTY = new HashSet<Metric>();

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

        public Set<Metric> build() {
            return new HashSet<Metric>(metrics.values());
        }

    }
}
