package org.jasome.metrics.calculators;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Network;
import com.google.common.graph.ValueGraph;
import org.jasome.input.Method;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.util.CalculationUtils;

import java.util.Set;

public class FanCalculator implements Calculator<Method> {
    @Override
    public Set<Metric> calculate(Method method) {

        ValueGraph<Method, Integer> methodCalls = CalculationUtils.getCallNetwork(method.getParentType().getParentPackage().getParentProject());

        Set<Method> methodsCalled = methodCalls.successors(method);

        int fanOut = 0;

        for(Method methodCalled: methodsCalled) {

            fanOut += methodCalls.edgeValue(method, methodCalled);

//            Set<Expression> calls = methodCalls.(method, methodCalled);
//
//            fanOut += calls.size();
        }

        Set<Method> methodsCalling = methodCalls.predecessors(method);

        int fanIn = 0;

        for(Method methodCalling: methodsCalling) {

            fanIn += methodCalls.edgeValue(methodCalling, method);

//            Set<Expression> calls = methodCalls.edgesConnecting(methodCalling, method);
//
//            fanIn += calls.size();
        }



        return ImmutableSet.of(
                Metric.of("Fout", "Fan-out", fanOut),
                Metric.of("Fin", "Fan-in", fanIn)
        );


    }
}
