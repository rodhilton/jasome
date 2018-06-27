package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jscience.mathematics.number.Number;

import java.util.DoubleSummaryStatistics;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jasome.input.Package;


public class TypeMetricAggregator implements Calculator<Package> {
    @Override
    public Set<Metric> calculate(Package aPackage) {


        Stream<Method> allMethods = aPackage.getTypes().stream().flatMap(t->t.getMethods().stream());

        DoubleSummaryStatistics stats = methodMetrics(allMethods, "Ci")
                .collect(Collectors.summarizingDouble(metric -> metric.getValue().doubleValue()));

        return ImmutableSet.of(
                Metric.of("PkgTCi", "Package Total System Complexity", stats.getSum()),
                Metric.of("PkgRCi", "Package Relative System Complexity", stats.getAverage())
        );

    }

    private Stream<Metric> methodMetrics(Stream<Method> methods, String metricName) {
        return methods.flatMap(type -> {
            Optional<Metric> metric = type.getMetric(metricName);
            if(metric.isPresent()) {
                return Stream.of(metric.get());
            } else {
                return Stream.empty();
            }
        });
    }
}
