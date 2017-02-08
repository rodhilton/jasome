package org.jasome.calculators;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.parsing.ProjectPackage;

import java.util.Collection;
import java.util.Set;

public interface PackageMetricCalculator {
    Set<Metric> calculate(Collection<ClassOrInterfaceDeclaration> classes, ProjectPackage projectPackage);
}
