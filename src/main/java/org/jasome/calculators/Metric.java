package org.jasome.calculators;

import com.google.common.base.Objects;
import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Number;
import org.jscience.mathematics.number.Rational;
import org.jscience.mathematics.number.Real;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Metric<T extends Number<T>> {
    private String name;
    private String description;
    private Number<T> value;

    private Metric(String name, String description, Number<T> value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    @Deprecated //Loss of precision is possible when using doubles or floats
    public static Metric<Real> of(String name, String description, double value) {
        return new Metric<>(name, description, Real.valueOf(value));
    }

    public static Metric<LargeInteger> of(String name, String description, long value) {
        return new Metric<>(name, description, LargeInteger.valueOf(value));
    }

    public static Metric<Real> of(String name, String description, String value) {
        return new Metric<>(name, description, Real.valueOf(value));
    }

    public static Metric<Real> of(String name, String description, BigDecimal value) {
        return new Metric<>(name, description, Real.valueOf(value.toString()));
    }

    public static Metric<LargeInteger> of(String name, String description, BigInteger value) {
        return new Metric<>(name, description, LargeInteger.valueOf(value));
    }

    public static Metric<Rational> of(String name, String description, Rational value) {
        return new Metric<>(name, description, value);
    }

    public static Metric<LargeInteger> of(String name, String description, LargeInteger value) {
        return new Metric<>(name, description, value);
    }

    public static Metric<Real> of(String name, String description, Real value) {
        return new Metric<>(name, description, value);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Number<T> getValue() {
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
}
