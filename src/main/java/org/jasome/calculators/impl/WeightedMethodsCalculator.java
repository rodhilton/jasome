package org.jasome.calculators.impl;

import com.google.common.collect.ImmutableSet;
import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;
import org.jasome.parsing.Type;

import java.math.BigDecimal;
import java.util.Set;

public class WeightedMethodsCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type method) {
        BigDecimal total = method.getMethods().stream().map(m -> m.getMetric("VG").get().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);

        return ImmutableSet.of(new Metric("WMC", "Weighted methods per Class", total));
    }
}
