package org.jasome.calculators;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.parsing.ProjectClass;

import java.util.Set;

public interface ClassMetricCalculator {

    Set<Metric> calculate(ClassOrInterfaceDeclaration declaration, ProjectClass projectClass);

}
