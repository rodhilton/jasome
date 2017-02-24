package org.jasome.metrics.calculators;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Network;
import org.jasome.input.Method;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.util.CalculationUtils;

import java.util.Set;

public class FanCalculator implements Calculator<Method> {
    @Override
    public Set<Metric> calculate(Method method) {

        Network<Method, Expression> methodCalls = CalculationUtils.getCallNetwork(method.getParentType().getParentPackage().getParentProject());

        Set<Method> methodsCalled = methodCalls.successors(method);

        int fanOut = 0;

        for(Method methodCalled: methodsCalled) {

            Set<Expression> calls = methodCalls.edgesConnecting(method, methodCalled);

            fanOut += calls.size();
        }

        Set<Method> methodsCalling = methodCalls.predecessors(method);

        int fanIn = 0;

        for(Method methodCalling: methodsCalling) {

            Set<Expression> calls = methodCalls.edgesConnecting(method, methodCalling);

            fanIn += calls.size();
        }



        return ImmutableSet.of(
                Metric.of("Fout", "Fan-out", fanOut),
                Metric.of("Fin", "Fan-in", fanIn)
        );


    }
}
