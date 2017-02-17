package org.jasome.metrics;

import com.google.common.base.Objects;
import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Rational;
import org.jscience.mathematics.number.Real;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Metric {
    private String name;
    private String description;
    private Number value;

    private Metric(String name, String description, Number value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public static Metric of(String name, String description, Number value) {
        return new Metric(name, description, value);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Number getValue() {
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
