package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.Set;

public class LinkCalculator implements Calculator<Type> {

    @Override
    public Set<Metric> calculate(Type type) {

        Graph<Type> uses = type.getParentPackage().getParentProject().getMetadata().getClientGraph();

        Set<Type> links = uses.successors(type);

        return ImmutableSet.of(
                Metric.of("NOL", "Number of Links", links.size())
        );
    }
}
