package org.jasome.calculators;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.Set;

public interface MethodMetricCalculator {

    Set<Calculation> calculate(MethodDeclaration declaration, SourceContext context);
}
