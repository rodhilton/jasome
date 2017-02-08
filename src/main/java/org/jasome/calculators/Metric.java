package org.jasome.calculators;

import com.google.common.base.Objects;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Metric {

    public static final HashSet<Metric> NONE = new HashSet<Metric>();

    private String name;
    private String description;
    private BigDecimal value;

    public Metric(String name, String description, BigDecimal value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name+": "+value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Metric)) return false;
        Metric that = (Metric) o;
        return Objects.equal(name, that.name) &&
                Objects.equal(description, that.description) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, description, value);
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
