package org.jasome.calculators.impl;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.calculators.Metric;
import org.jasome.calculators.Metrics;
import org.jasome.calculators.PackageMetricCalculator;
import org.jasome.parsing.ProjectPackage;

import java.util.Collection;
import java.util.Set;

public class NumberOfClassesCalculator implements PackageMetricCalculator {

    @Override
    public Set<Metric> calculate(Collection<ClassOrInterfaceDeclaration> classes, ProjectPackage projectPackage) {
        return Metrics.builder().with("NOC", "Number of Classes", classes.size()).build();
    }
}
