package org.jasome.metrics;

import com.google.common.base.Objects;
import org.jscience.mathematics.number.*;

import java.math.BigInteger;
import java.text.DecimalFormat;

public class Metric {
    private static final DecimalFormat METRIC_VALUE_FORMAT = new DecimalFormat("0.0########");

    private String name;
    private String description;
    private Real value;
    private boolean isInteger;

    protected Metric(String name, String description, Real value, boolean isInteger) {
        this.name = name;
        this.description = description;
        this.value = value;
        this.isInteger = isInteger;
    }

    public static Metric of(String name, String description, Real value) {
        return new Metric(name, description, value, false);
    }

    public static Metric of(String name, String description, BigInteger value) {
        return new Metric(name, description, Real.valueOf(value.toString()), true);
    }
    
    public static Metric of(String name, String description, LargeInteger value) {
        return new Metric(name, description, Real.valueOf(value.toString()), true);
    }

    public static Metric of(String name, String description, Rational value) {
        return new Metric(name, description, Real.valueOf(value.getDividend().toString()).divide(Real.valueOf(value.getDivisor().toString())), false);
    }

    public static Metric of(String name, String description, long value) {
        return new Metric(name, description, Real.valueOf(value), true);
    }

    public static Metric of(String name, String description, double value) {
        return new Metric(name, description, Real.valueOf(value), false);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Real getValue() {
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
        if(isInteger) {
            return value.round().toString();
        } else {
            return METRIC_VALUE_FORMAT.format(value.doubleValue());
        }
    }
}
