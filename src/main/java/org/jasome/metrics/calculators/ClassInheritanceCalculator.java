package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class ClassInheritanceCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type type) {
        Graph<Type> inheritanceGraph = type.getParentPackage().getParentProject().getMetadata().getInheritanceGraph();

        Set<Type> parents = inheritanceGraph.predecessors(type);
        Set<Type> children = inheritanceGraph.successors(type);

        //Cyclic inheritance is prevented in Java so we don't have to worry here
        HashSet<Type> ancestors = new HashSet<>();
        Stack<Type> ancestorStack = new Stack<Type>();
        ancestorStack.push(type);
        while(!ancestorStack.isEmpty()) {
            Type t = ancestorStack.pop();
            Set<Type> ancestorsForType = inheritanceGraph.predecessors(t);
            ancestors.addAll(ancestorsForType);
            ancestorStack.addAll(ancestorsForType);
        }

        HashSet<Type> descendants = new HashSet<>();
        Stack<Type> descendantStack = new Stack<Type>();
        descendantStack.push(type);
        while(!descendantStack.isEmpty()) {
            Type t = descendantStack.pop();
            Set<Type> descendantsForType = inheritanceGraph.successors(t);
            descendants.addAll(descendantsForType);
            descendantStack.addAll(descendantsForType);
        }


        return ImmutableSet.of(
                Metric.of("NOPa", "Number of Parents", parents.size()),
                Metric.of("NOCh", "Number of Children", children.size()),
                Metric.of("NOD", "Number of Descendants", descendants.size()),
                Metric.of("NOA", "Number of Ancestors", ancestors.size())
        );
    }
}
