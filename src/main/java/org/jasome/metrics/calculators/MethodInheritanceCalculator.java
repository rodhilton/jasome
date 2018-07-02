package org.jasome.metrics.calculators;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.Graph;
import org.apache.commons.lang3.tuple.Triple;
import org.jasome.input.Method;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.metrics.value.NumericValue;
import org.jasome.util.CalculationUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodInheritanceCalculator implements Calculator<Type> {


    @Override
    public Set<Metric> calculate(Type type) {
        Graph<Type> inheritanceGraph = CalculationUtils.inheritanceGraph.getUnchecked(type.getParentPackage().getParentProject());

        ClassOrInterfaceDeclaration declaration = type.getSource();

        Set<Type> ancestors = new HashSet<Type>();

        Set<Type> parents = inheritanceGraph.predecessors(type);

        Stack<Type> typesToCheck = new Stack<>();
        typesToCheck.addAll(parents);

        while (!typesToCheck.empty()) {
            Type typeToCheck = typesToCheck.pop();
            ancestors.add(typeToCheck);

            typesToCheck.addAll(inheritanceGraph.predecessors(typeToCheck));
        }


        Set<Method> inheritedMethods = ancestors.stream()
                .flatMap(p->p.getMethods().stream())
                .filter(method -> {
                    //We only want to count a method as inherited if it's a parent method that has an implementation
                    //In other words we want to exclude anything on an interface unless it's got a default impl
                    //And we want to exclude any abstract methods
                    boolean isDefinedOnAbstractClass = method.getParentType().getSource().isAbstract();
                    boolean isAbstract = isDefinedOnAbstractClass && method.getSource().isAbstract();
                    boolean isDefinedOnInterface = method.getParentType().getSource().isInterface();
                    boolean isDefaultImpl = isDefinedOnInterface && method.getSource().isDefault();

                    if(isDefinedOnInterface) {
                        return isDefaultImpl;
                    } else if(isDefinedOnAbstractClass) {
                        return !isAbstract;
                    } else {
                        return true;
                    }
                })
                .collect(Collectors.toSet());

        Set<Method> definedMethods = type.getMethods().stream()
                .filter(method -> {
                    boolean isDefinedOnAbstractClass = method.getParentType().getSource().isAbstract();
                    boolean isAbstract = isDefinedOnAbstractClass && method.getSource().isAbstract();
                    boolean isDefinedOnInterface = method.getParentType().getSource().isInterface();
                    boolean isDefaultImpl = isDefinedOnInterface && method.getSource().isDefault();

                    if(isAbstract) {
                        return false;
                    } else if(isDefinedOnInterface && !isDefaultImpl) {
                        return false;
                    } else {
                        return true;
                    }
                })
                .collect(Collectors.toSet());

        Set<String> inheritedMethodSignatures = inheritedMethods.stream()
                .map(im->im.getSource().getSignature().asString())
                .collect(Collectors.toSet());

        Set<Method> overriddenMethods = definedMethods.stream()
                .filter(dm -> inheritedMethodSignatures.contains(dm.getSource().getSignature().asString()))
                .collect(Collectors.toSet());

        Set<String> overriddenMethodSignatures = overriddenMethods.stream()
                .map(im->im.getSource().getSignature().asString())
                .collect(Collectors.toSet());

        Set<Method> inheritedAndNotOverridden = inheritedMethods.stream()
                .filter(im -> !overriddenMethodSignatures.contains(im.getSource().getSignature().asString()))
                .collect(Collectors.toSet());

        Set<Method> allMethods = Sets.union(definedMethods, inheritedAndNotOverridden);

        Set<Method> publicDefinedMethods = definedMethods.stream()
                .filter(dm->dm.getSource().isPublic())
                .collect(Collectors.toSet());

        Set<Method> publicInheritedNotOverridenMethods = inheritedAndNotOverridden.stream()
                .filter(dm->dm.getSource().isPublic())
                .collect(Collectors.toSet());

        ImmutableSet.Builder<Metric> metricBuilder = ImmutableSet.<Metric>builder()
                .add(Metric.of("Mit", "Number of Methods Inherited (Total)", inheritedMethods.size()))
                .add(Metric.of("Mi", "Number of Methods Inherited and Not Overridden", inheritedAndNotOverridden.size()))
                .add(Metric.of("Md", "Number of Methods Defined", definedMethods.size()))
                .add(Metric.of("Mo", "Number of Methods Overridden", overriddenMethods.size()))
                .add(Metric.of("Ma", "Number of Methods (All)", allMethods.size()))
                .add(Metric.of("PMi", "Number of Public Methods Inherited and Not Overridden", publicInheritedNotOverridenMethods.size()))
                .add(Metric.of("PMd", "Number of Public Methods Defined", publicDefinedMethods.size()));

        if(!allMethods.isEmpty()) {
            metricBuilder.add(Metric.of("MIF", "Method Inheritance Factor", NumericValue.of(inheritedAndNotOverridden.size()).divide(NumericValue.of(allMethods.size()))));
            NumericValue publicMethods = NumericValue.of(publicInheritedNotOverridenMethods.size()).plus(NumericValue.of(publicDefinedMethods.size()));
            metricBuilder.add(Metric.of("MHF", "Method Hiding Factor", publicMethods.divide(NumericValue.of(allMethods.size()))));
        }

        return metricBuilder.build();
    }
}
