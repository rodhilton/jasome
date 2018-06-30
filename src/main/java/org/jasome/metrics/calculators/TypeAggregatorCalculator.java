package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.MathOps;
import org.jasome.metrics.Metric;
import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Number;

import java.util.*;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Collectors.CollectorImpl;
import java.util.stream.Stream;

public class TypeAggregatorCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type type) {


        Number count = LargeInteger.ZERO;


        //TODO: we lose precision here, not a huge fan of this
        List<Number> ci1 = methodMetrics(type.getMethods(), "Ci");
        Number total = ci1.stream().reduce(LargeInteger.ZERO, MathOps::plus);

        DoubleSummaryStatistics stats = ((List<Number>) ci)
                .collect(Collectors.summarizingDouble(metric -> metric.getValue().doubleValue()));

        return ImmutableSet.of(
                Metric.of("ClTCi", "Class Total System Complexity", stats.getSum()),
                Metric.of("ClRCi", "Class Relative System Complexity", stats.getAverage())
        );

    }

    private List<Number> methodMetrics(Set<Method> methods, String metricName) {
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
