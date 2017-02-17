package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.input.Type;
import org.jscience.mathematics.number.LargeInteger;

import java.util.Set;

public class WeightedMethodsCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type method) {
        LargeInteger total = method.getMethods().parallelStream().map(m -> LargeInteger.valueOf(m.getMetric("VG").get().getValue().longValue())).reduce(LargeInteger.ZERO, LargeInteger::plus);

        return ImmutableSet.of(Metric.of("WMC", "Weighted methods per Class", total));
    }
}
