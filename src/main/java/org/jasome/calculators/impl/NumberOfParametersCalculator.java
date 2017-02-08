package org.jasome.calculators.impl;

import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;
import org.jasome.parsing.Method;

import java.util.Set;

public class NumberOfParametersCalculator implements Calculator<Method> {
    @Override
    public Set<Metric> calculate(Method method) {
        return Metric.builder().with("NOP", "Number of Parameters", method.getSource().getParameters().size()).build();
    }
}
