package org.jasome.calculators;

import org.jasome.parsing.Type;

import java.util.Set;

public interface TypeMetricCalculator {

    Set<Metric> calculate(Type aType);

}
