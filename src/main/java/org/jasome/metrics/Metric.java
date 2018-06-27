package org.jasome.metrics;

import com.google.common.base.Objects;
import org.jscience.mathematics.number.*;
import org.jscience.mathematics.number.Number;

import java.math.BigInteger;

public class Metric<N extends Number<N>> {
    private String name;
    private String description;
    private N value;

    private Metric(String name, String description, N value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public static <N extends Number<N>> Metric<N> of(String name, String description, N value) {
        return new Metric<>(name, description, value);
    }

    public static Metric<LargeInteger> of(String name, String description, BigInteger value) {
        return new Metric<>(name, description, LargeInteger.valueOf(value));
    }
    
    public static Metric<LargeInteger> of(String name, String description, LargeInteger value) {
        return new Metric<>(name, description, value);
    }

    public static Metric<Rational> of(String name, String description, Rational value) {
        return new Metric<>(name, description, value);
    }

    public static Metric<LargeInteger> of(String name, String description, long value) {
        return new Metric<LargeInteger>(name, description, LargeInteger.valueOf(value));
    }

    public static Metric<FloatingPoint> of(String name, String description, double value) {
        return new Metric<FloatingPoint>(name, description, FloatingPoint.valueOf(value));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public N getValue() {
        return value;
    }

    public Metric<N> plus(Metric<N> that) {
        return new Metric<>("Aggregated", "Aggregated", value.plus(that.getValue()));
    }

    public boolean isInteger() {
        return value instanceof LargeInteger;
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
}
