package org.jasome.metrics;

import com.google.common.base.Objects;
import org.jasome.metrics.value.NumericValue;

public class Metric {
    private String name;
    private String description;
    private NumericValue value;

    protected Metric(String name, String description, NumericValue value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public static Metric of(String name, String description, NumericValue value) {
        return new Metric(name, description, value);
    }
    
    public static Metric of(String name, String description, long value) {
        return new Metric(name, description, NumericValue.of(value));
    }

    public static Metric of(String name, String description, double value) {
        return new Metric(name, description, NumericValue.of(value));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public NumericValue getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return name + ": " + value;
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

    public String getFormattedValue() {
        return value.toString();
    }
}
