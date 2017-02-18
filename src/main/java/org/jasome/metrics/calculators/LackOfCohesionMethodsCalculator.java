package org.jasome.metrics.calculators;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.util.CalculationUtils;
import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Rational;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LackOfCohesionMethodsCalculator implements Calculator<Type> {
    @Override
    public Set<Metric> calculate(Type type) {
        List<FieldDeclaration> fieldDeclarations = type.getSource().getFields();
        Set<VariableDeclarator> variables = new HashSet<>();

        fieldDeclarations.stream().map(FieldDeclaration::getVariables).forEach(variables::addAll);

        List<MethodDeclaration> methods = type.getMethods().stream().map(Method::getSource).collect(Collectors.toList());

        LargeInteger total = LargeInteger.ZERO;

        for (VariableDeclarator variable : variables) {
            int numberOfMethodsAccessingVariable = 0;
            for (MethodDeclaration method : methods) {
                if (CalculationUtils.isFieldAccessedWithinMethod(method, variable)) {
                    numberOfMethodsAccessingVariable++;
                }
            }

            total = total.plus(numberOfMethodsAccessingVariable);
        }


        try {
            LargeInteger numberOfMethods = LargeInteger.valueOf(methods.size());
            LargeInteger numberOfVariables = LargeInteger.valueOf(variables.size());

            Rational averageNumberOfMethodsAccessingEachVariable = Rational.valueOf(total, numberOfVariables);

            Rational numberOfMethodsAsRational = Rational.valueOf(numberOfMethods, LargeInteger.ONE);
            Rational numerator = averageNumberOfMethodsAccessingEachVariable.minus(numberOfMethodsAsRational);

            Rational denominator = Rational.ONE.minus(numberOfMethodsAsRational);

            Rational lackOfCohesionMethods = numerator.divide(denominator);
            return ImmutableSet.of(Metric.of("LCOM*", "Lack of Cohesion Methods (H-S)", lackOfCohesionMethods));
        } catch (ArithmeticException e) {
            return ImmutableSet.of();
        }


    }

}
