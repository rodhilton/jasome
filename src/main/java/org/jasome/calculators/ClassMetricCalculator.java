package org.jasome.calculators;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public interface ClassMetricCalculator {

    Metrics calculate(ClassOrInterfaceDeclaration declaration, SourceContext context);

}
