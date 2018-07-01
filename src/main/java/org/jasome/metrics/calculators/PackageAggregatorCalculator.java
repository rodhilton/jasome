package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Package;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.metrics.value.NumericValue;
import org.jasome.metrics.value.NumericValueSummaryStatistics;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class PackageAggregatorCalculator implements Calculator<Package> {
    @Override
    public Set<Metric> calculate(Package aPackage) {


        Stream<Method> allMethods = aPackage.getTypes().stream().flatMap(t->t.getMethods().stream());
        NumericValueSummaryStatistics stats = methodMetrics(allMethods, "Ci")
                .collect(NumericValue.summarizingCollector());

        Stream<Type> allTypes = aPackage.getTypes().stream();
        NumericValueSummaryStatistics numberOfLinksSummary = typeMetrics(allTypes, "NOL")
                .collect(NumericValue.summarizingCollector());

        NumericValue classCategoricalRelationalCohesion = NumericValue.of(100).times(numberOfLinksSummary.getAverage());

        return ImmutableSet.of(
                Metric.of("PkgTCi", "Package Total System Complexity", stats.getSum()),
                Metric.of("PkgRCi", "Package Relative System Complexity", stats.getAverage()),
                Metric.of("CCRC", "Class Categorical Relational Cohesion", classCategoricalRelationalCohesion)

        );

    }

    private Stream<NumericValue> typeMetrics(Stream<Type> types, String metricName) {
        return types.flatMap(type -> {
            Optional<Metric> metric = type.getMetric(metricName);
            if(metric.isPresent()) {
                return Stream.of(metric.get().getValue());
            } else {
                return Stream.empty();
            }
        });
    }

    private Stream<NumericValue> methodMetrics(Stream<Method> methods, String metricName) {
        return methods.flatMap(type -> {
            Optional<Metric> metric = type.getMetric(metricName);
            if(metric.isPresent()) {
                return Stream.of(metric.get().getValue());
            } else {
                return Stream.empty();
            }
        });
    }
}
