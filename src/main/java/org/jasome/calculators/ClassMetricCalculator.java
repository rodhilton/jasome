package org.jasome.calculators;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.Set;

public interface ClassMetricCalculator {

    Set<Calculation> calculate(ClassOrInterfaceDeclaration declaration, SourceContext context);

}
