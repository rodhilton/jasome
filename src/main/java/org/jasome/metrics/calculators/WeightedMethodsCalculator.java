package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.metrics.value.NumericValue;

import java.util.Set;

public class WeightedMethodsCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type method) {
        NumericValue total = method.getMethods().parallelStream().map(m -> m.getMetric("VG").get().getValue()).reduce(NumericValue.ZERO, NumericValue::plus);

        return ImmutableSet.of(Metric.of("WMC", "Weighted methods per Class", total));
    }
}
