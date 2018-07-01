package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.metrics.value.NumericValue;
import org.jasome.metrics.value.NumericValueSummaryStatistics;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class TypeAggregatorCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type type) {

        NumericValueSummaryStatistics ciStats = methodMetrics(type.getMethods(), "Ci").collect(NumericValue.summarizingCollector());

        return ImmutableSet.of(
                Metric.of("ClTCi", "Class Total System Complexity", ciStats.getSum()),
                Metric.of("ClRCi", "Class Relative System Complexity", ciStats.getAverage())
        );

    }

    private Stream<NumericValue> methodMetrics(Set<Method> methods, String metricName) {
        return methods.stream().flatMap(method -> {
            Optional<Metric> metric = method.getMetric(metricName);
            if(metric.isPresent()) {
                return Stream.of(metric.get().getValue());
            } else {
                return Stream.empty();
            }
        });
    }
}
