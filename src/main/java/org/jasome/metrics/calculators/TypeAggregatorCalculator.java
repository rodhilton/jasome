package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jscience.mathematics.number.Real;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypeAggregatorCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type type) {

        //TODO: we lose precision here, not a huge fan of this
        List<Real> ci1 = methodMetrics(type.getMethods(), "Ci");
        Real total = ci1.stream().reduce(Real.ZERO, Real::plus);
//
//        DoubleSummaryStatistics stats = ((List<Number>) ci)
//                .collect(Collectors.summarizingDouble(metric -> metric.getValue().doubleValue()));

        Real avg = total.divide(ci1.size());

        return ImmutableSet.of(
                Metric.of("ClTCi", "Class Total System Complexity", total),
                Metric.of("ClRCi", "Class Relative System Complexity", avg)
        );

    }

    private List<Real> methodMetrics(Set<Method> methods, String metricName) {
        return methods.stream().flatMap(method -> {
            Optional<Metric> metric = method.getMetric(metricName);
            if(metric.isPresent()) {
                return Stream.of(metric.get().getValue());
            } else {
                return Stream.empty();
            }
        }).collect(Collectors.toList());
    }
}
