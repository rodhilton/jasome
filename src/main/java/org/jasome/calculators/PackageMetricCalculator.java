package org.jasome.calculators;

import org.jasome.parsing.Package;

import java.util.Set;

public interface PackageMetricCalculator {
    Set<Metric> calculate(Package aPackage);
}
