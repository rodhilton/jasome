package org.jasome.metrics.metrics;

import org.jasome.metrics.Metric;
import org.jscience.mathematics.number.Rational;

import java.text.DecimalFormat;

public class RationalMetric extends Metric<Rational> {
    private static final DecimalFormat METRIC_VALUE_FORMAT = new DecimalFormat("0.0########");

    public RationalMetric(String name, String description, Rational value) {
        super(name, description, value);
    }

    @Override
    public String getFormattedValue() {
        return METRIC_VALUE_FORMAT.format(this.getValue().doubleValue());
    }
}
