package org.jasome.calculators.impl;

import com.google.common.collect.ImmutableSet;
import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;
import org.jasome.parsing.Type;
import org.jscience.mathematics.number.LargeInteger;

import java.util.Set;

public class WeightedMethodsCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type method) {
        LargeInteger total = method.getMethods().stream().map(m -> LargeInteger.valueOf(m.getMetric("VG").get().getValue().longValue())).reduce(LargeInteger.ZERO, LargeInteger::plus);

        return ImmutableSet.of(Metric.of("WMC", "Weighted methods per Class", total));
    }
}
