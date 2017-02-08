package org.jasome.calculators;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.Set;

public interface MethodMetricCalculator {

    Set<Metric> calculate(MethodDeclaration declaration, SourceContext context);
}
