package org.jasome.calculators.impl;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;
import org.jasome.parsing.Type;

import java.util.Set;

public class NumberOfFieldsCalculator implements Calculator<Type> {

    @Override
    public Set<Metric> calculate(Type type) {
        ClassOrInterfaceDeclaration declaration = type.getSource();


        long numAttributes = declaration.getFields().stream().count();
        long numStaticAttributes = declaration.getFields().stream().filter(f -> f.getModifiers().contains(Modifier.STATIC)).count();
        long numPublicAttributes = declaration.getFields().stream().filter(f -> f.getModifiers().contains(Modifier.PUBLIC)).count();

        long numMethods = declaration.getMethods().stream().count();
        long numStaticMethods = declaration.getMethods().stream().filter(f -> f.getModifiers().contains(Modifier.STATIC)).count();
        long numPublicMethods = declaration.getMethods().stream().filter(f -> f.getModifiers().contains(Modifier.PUBLIC)).count();

        return Metric.builder()
                .with("NF", "Number of Attributes", numAttributes)
                .with("NSF", "Number of Static Attributes", numStaticAttributes)
                .with("NPF", "Number of Public Attributes", numPublicAttributes)
                .with("NM", "Number of Methods", numMethods)
                .with("NSM", "Number of Static Methods", numStaticMethods)
                .with("NPM", "Number of Public Methods", numPublicMethods)
                .build();
    }
}
