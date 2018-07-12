package org.jasome.metrics.calculators;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.*;
import com.google.common.collect.ImmutableSet;
import org.jasome.input.Method;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.metrics.value.NumericValue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.AND;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.OR;

public class CyclomaticComplexityCalculator implements Calculator<Method> {
    @Override
    public Set<Metric> calculate(Method method) {
        List<IfStmt> ifStmts = method.getSource().getNodesByType(IfStmt.class);
        List<ForStmt> forStmts = method.getSource().getNodesByType(ForStmt.class);
        List<WhileStmt> whileStmts = method.getSource().getNodesByType(WhileStmt.class);
        List<DoStmt> doStmts = method.getSource().getNodesByType(DoStmt.class);
        List<SwitchEntryStmt> catchStmts = method.getSource().getNodesByType(SwitchEntryStmt.class).stream().
                filter(s -> s.getLabel().isPresent()) //Don't include "default" statements, only labeled case statements
                .collect(Collectors.toList());
        List<ConditionalExpr> ternaryExprs = method.getSource().getNodesByType(ConditionalExpr.class);
        List<BinaryExpr> andExprs = method.getSource().getNodesByType(BinaryExpr.class).stream().
                filter(f -> f.getOperator() == AND).collect(Collectors.toList());
        List<BinaryExpr> orExprs = method.getSource().getNodesByType(BinaryExpr.class).stream().
                filter(f -> f.getOperator() == OR).collect(Collectors.toList());

        NumericValue total = NumericValue.of(ifStmts.size() +
                forStmts.size() +
                whileStmts.size() +
                doStmts.size() +
                catchStmts.size() +
                ternaryExprs.size() +
                andExprs.size() +
                orExprs.size() +
                1); //There's always at least 1 path through the method

        return ImmutableSet.of(Metric.of("VG", "McCabe Cyclomatic Complexity", total));
    }
}
