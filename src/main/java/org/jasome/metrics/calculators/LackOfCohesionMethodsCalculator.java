package org.jasome.metrics.calculators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
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
                if (methodAccessesVariable(method, variable)) {
                    numberOfMethodsAccessingVariable++;
                }
            }

            total = total.plus(numberOfMethodsAccessingVariable);
        }

        LargeInteger numberOfMethods = LargeInteger.valueOf(methods.size());
        LargeInteger numberOfVariables = LargeInteger.valueOf(variables.size());

        Rational averageNumberOfMethodsAccessingEachVariable = Rational.valueOf(total, numberOfVariables);

        Rational numberOfMethodsAsRational = Rational.valueOf(numberOfMethods, LargeInteger.ONE);
        Rational numerator = averageNumberOfMethodsAccessingEachVariable.minus(numberOfMethodsAsRational);

        Rational denominator = Rational.ONE.minus(numberOfMethodsAsRational);

        try {
            Rational lackOfCohesionMethods = numerator.divide(denominator);
            return ImmutableSet.of(Metric.of("LCOM*", "Lack of Cohesion Methods (H-S)", lackOfCohesionMethods));
        } catch(ArithmeticException e) {
            return ImmutableSet.of();
        }


    }

    private boolean methodAccessesVariable(MethodDeclaration method, VariableDeclarator variable) {
        if(!method.getBody().isPresent()) return false;

        List<FieldAccessExpr> fieldAccesses = method.getBody().get().getNodesByType(FieldAccessExpr.class);

        //If we have a field match we can just count it, it's directly prefixed with 'this.' so there's no room for shadowing

        boolean anyDirectAccess = fieldAccesses.stream().anyMatch(fieldAccessExpr -> fieldAccessExpr.getName().equals(variable.getName()));

        if(anyDirectAccess) return true;
        else {
            List<NameExpr> nameAccesses = method.getBody().get().getNodesByType(NameExpr.class);

            //stupid mode, just see if the names match
            boolean anyIndirectAccess = nameAccesses
                    .stream()
                    .anyMatch(nameAccessExpr -> {

                        boolean isVariableRedefinedInScope = false;
                        Node theNode = nameAccessExpr;

                        while(!(theNode instanceof MethodDeclaration)) {

                            List<VariableDeclarator> variablesDefined = theNode.getNodesByType(VariableDeclarator.class);

                            boolean variableIsDefinedInThisScope = variablesDefined.stream().anyMatch(variableDeclarator -> variableDeclarator.getName().equals(variable.getName()));

                            if(variableIsDefinedInThisScope) {
                                isVariableRedefinedInScope = true;
                            }
                            
                            if (theNode.getParentNode().isPresent()) {
                                theNode = theNode.getParentNode().get();
                            } else {
                                break;
                            }
                        }

                        if(isVariableRedefinedInScope) {
                             return false;
                        } else {
                            return nameAccessExpr.getName().equals(variable.getName());
                        }
                    });

            if(anyIndirectAccess) return true;
        }


        return false;
    }
}
