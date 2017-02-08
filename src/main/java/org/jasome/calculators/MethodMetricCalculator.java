package org.jasome.calculators;

import org.jasome.parsing.Method;

import java.util.Set;

public interface MethodMetricCalculator {

    Set<Metric> calculate(Method aMethod);
}
