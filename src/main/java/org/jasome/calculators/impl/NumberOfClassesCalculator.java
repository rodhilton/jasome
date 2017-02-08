package org.jasome.calculators.impl;

import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;
import org.jasome.parsing.Package;

import java.util.Set;

public class NumberOfClassesCalculator implements Calculator<Package> {

    @Override
    public Set<Metric> calculate(Package aPackage) {
        return Metric.builder().with("NOC", "Number of Classes", aPackage.getTypes().size()).build();
    }
}
