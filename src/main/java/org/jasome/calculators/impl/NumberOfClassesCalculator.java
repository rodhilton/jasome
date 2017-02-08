package org.jasome.calculators.impl;

import org.jasome.calculators.Metric;
import org.jasome.calculators.Metrics;
import org.jasome.calculators.PackageMetricCalculator;
import org.jasome.parsing.Package;

import java.util.Set;

public class NumberOfClassesCalculator implements PackageMetricCalculator {

    @Override
    public Set<Metric> calculate(Package aPackage) {
        return Metrics.builder().with("NOC", "Number of Classes", aPackage.getTypes().size()).build();
    }
}
