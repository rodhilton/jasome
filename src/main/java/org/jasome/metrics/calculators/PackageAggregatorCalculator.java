package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Package;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.DoubleSummaryStatistics;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackageAggregatorCalculator implements Calculator<Package> {
    @Override
    public Set<Metric> calculate(Package aPackage) {


        Stream<Method> allMethods = aPackage.getTypes().stream().flatMap(t->t.getMethods().stream());

        //TODO: we lose precision here, not a huge fan of this
        DoubleSummaryStatistics stats = methodMetrics(allMethods, "Ci")
                .collect(Collectors.summarizingDouble(metric -> metric.getValue().doubleValue()));

        Stream<Type> allTypes = aPackage.getTypes().stream();
        LongSummaryStatistics numberOfLinksSummary = typeMetrics(allTypes, "NOL")
                    .collect(Collectors.summarizingLong(metric->metric.getValue().longValue()));

        //double classCategoricalRelationalCohesion = 100 * (numberOfLinksSummary.getSum() / (double) aPackage.getTypes().size());
        double classCategoricalRelationalCohesion = 100 * numberOfLinksSummary.getAverage();

        return ImmutableSet.of(
                Metric.of("PkgTCi", "Package Total System Complexity", stats.getSum()),
                Metric.of("PkgRCi", "Package Relative System Complexity", stats.getAverage()),
                Metric.of("CCRC", "Class Categorical Relational Cohesion", classCategoricalRelationalCohesion)

        );

    }

    private Stream<Metric> typeMetrics(Stream<Type> types, String metricName) {
        return types.flatMap(type -> {
            Optional<Metric> metric = type.getMetric(metricName);
            if(metric.isPresent()) {
                return Stream.of(metric.get());
            } else {
                return Stream.empty();
            }
        });
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
