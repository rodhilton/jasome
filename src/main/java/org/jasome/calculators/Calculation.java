package org.jasome.calculators;

import com.google.common.base.Objects;

import java.math.BigDecimal;

public class Calculation {
    private String name;
    private String description;
    private BigDecimal value;

    public Calculation(String name, String description, BigDecimal value) {
        this.name = name;
        this.description = description;
        this.value = value;
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
        if (!(o instanceof Calculation)) return false;
        Calculation that = (Calculation) o;
        return Objects.equal(name, that.name) &&
                Objects.equal(description, that.description) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, description, value);
    }
}
