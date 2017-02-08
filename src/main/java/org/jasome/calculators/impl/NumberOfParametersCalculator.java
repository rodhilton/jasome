package org.jasome.calculators.impl;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.jasome.calculators.MethodMetricCalculator;
import org.jasome.calculators.Metric;
import org.jasome.calculators.Metrics;
import org.jasome.parsing.Method;

import java.util.Set;

public class NumberOfParametersCalculator implements MethodMetricCalculator {
    @Override
    public Set<Metric> calculate(Method method) {
        return Metrics.builder().with("NOP", "Number of Parameters", method.getSource().getParameters().size()).build();
    }
}
