package org.jasome.metrics.calculators;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class McclureCalculator implements Calculator<Method> {
    @Override
    public Set<Metric> calculate(Method method) {
        MethodDeclaration methodDeclaration = method.getSource();

        List<BinaryExpr> comparisons = methodDeclaration.findAll(BinaryExpr.class);

        List<Expression> conditionalExprs = new ArrayList<>();

        conditionalExprs.addAll(
                methodDeclaration.findAll(IfStmt.class).stream().map(IfStmt::getCondition).collect(Collectors.toList())
        );

        conditionalExprs.addAll(
                methodDeclaration.findAll(WhileStmt.class).stream().map(WhileStmt::getCondition).collect(Collectors.toList())
        );

        conditionalExprs.addAll(
                methodDeclaration.findAll(DoStmt.class).stream().map(DoStmt::getCondition).collect(Collectors.toList())
        );

        conditionalExprs.addAll(
                methodDeclaration.findAll(ForStmt.class)
                        .stream()
                        .map(ForStmt::getCompare)
                        .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
                        .collect(Collectors.toList())
        );

        conditionalExprs.addAll(
            methodDeclaration.findAll(SwitchStmt.class).stream().map(SwitchStmt::getSelector).collect(Collectors.toList())
        );


        List<NameExpr> controlVariables = conditionalExprs.stream().flatMap(cond->cond.findAll(NameExpr.class).stream()).collect(Collectors.toList());

        Set<String> uniqueControlVariableNames = controlVariables.stream().map(NameExpr::getNameAsString).collect(Collectors.toSet());

        return ImmutableSet.of(
                Metric.of("NCOMP", "Number of Comparisons", comparisons.size()),
                Metric.of("NVAR", "Number of Control Variables", uniqueControlVariableNames.size()),
                Metric.of("MCLC", "McClure's Complexity Metric", uniqueControlVariableNames.size() + comparisons.size())
        );
    }
}
