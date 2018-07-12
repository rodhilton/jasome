package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

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
        return ImmutableSet.of(Metric.of("NOP", "Number of Parameters", method.getSource().getParameters().size()));
    }
}
