package org.jasome.calculators;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.Collection;
import java.util.Set;

public interface PackageMetricCalculator {
    Set<Metric> calculate(Collection<ClassOrInterfaceDeclaration> classes, SourceContext context);
}
