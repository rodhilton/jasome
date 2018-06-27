package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.DoubleSummaryStatistics;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodMetricAggregator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type type) {


        DoubleSummaryStatistics stats = methodMetrics(type.getMethods(), "Ci")
                .collect(Collectors.summarizingDouble(metric -> metric.getValue().doubleValue()));

        return ImmutableSet.of(
                Metric.of("ClTCi", "Class Total System Complexity", stats.getSum()),
                Metric.of("ClRCi", "Class Relative System Complexity", stats.getAverage())
        );

    }

    private Stream<Metric> methodMetrics(Set<Method> methods, String metricName) {
        return methods.stream().flatMap(method -> {
            Optional<Metric> metric = method.getMetric(metricName);
            if(metric.isPresent()) {
                return Stream.of(metric.get());
            } else {
                return Stream.empty();
            }
        });
    }
}
