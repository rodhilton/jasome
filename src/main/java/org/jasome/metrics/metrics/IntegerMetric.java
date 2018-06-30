package org.jasome.metrics.metrics;

import org.jasome.metrics.Metric;
import org.jscience.mathematics.number.LargeInteger;

public class IntegerMetric extends Metric<LargeInteger> {
    public IntegerMetric(String name, String description, LargeInteger value) {
        super(name, description, value);
    }

    @Override
    public String getFormattedValue() {
        return this.getValue().toString();
    }
}
