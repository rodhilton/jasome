package org.jasome.metrics.metrics;

import org.jasome.metrics.Metric;
import org.jscience.mathematics.number.FloatingPoint;

import java.text.DecimalFormat;

public class RealMetric extends Metric<FloatingPoint> {
    private static final DecimalFormat METRIC_VALUE_FORMAT = new DecimalFormat("0.0########");

    public RealMetric(String name, String description, FloatingPoint value) {
        super(name, description, value);
    }

    @Override
    public String getFormattedValue() {
        return METRIC_VALUE_FORMAT.format(this.getValue().doubleValue());
    }
}
