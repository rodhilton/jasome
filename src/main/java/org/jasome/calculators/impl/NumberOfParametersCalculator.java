package org.jasome.calculators.impl;

import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;
import org.jasome.parsing.Method;

import java.util.Set;

/**
 * Simply counts the number of parameters on a method
 *
 * @author Rod Hilton
 * @since 0.3
 */
public class NumberOfParametersCalculator implements Calculator<Method> {
    @Override
    public Set<Metric> calculate(Method method) {
        return Metric.builder().with("NOP", "Number of Parameters", method.getSource().getParameters().size()).build();
    }
}
