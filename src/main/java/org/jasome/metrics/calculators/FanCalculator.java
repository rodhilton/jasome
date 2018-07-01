package org.jasome.metrics.calculators;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import org.jasome.input.Method;
import org.jasome.input.Package;
import org.jasome.input.Project;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.metrics.value.NumericValue;
import org.jasome.util.Distinct;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FanCalculator implements Calculator<Method> {

    @Override
    public synchronized Set<Metric> calculate(Method method) {

        Network<Method, Distinct<Expression>> methodCalls = callNetwork.getUnchecked(method.getParentType().getParentPackage().getParentProject());

        Set<Method> methodsCalled = methodCalls.successors(method);

        int fanOut = 0;

        for (Method methodCalled : methodsCalled) {
            Set<Distinct<Expression>> calls = methodCalls.edgesConnecting(method, methodCalled);
            fanOut += calls.size();
        }

        Set<Method> methodsCalling = methodCalls.predecessors(method);

        int fanIn = 0;

        for (Method methodCalling : methodsCalling) {
            Set<Distinct<Expression>> calls = methodCalls.edgesConnecting(methodCalling, method);
            fanIn += calls.size();
        }

        int returns = method.getSource().getType() instanceof VoidType ? 0 : 1;
        int parameters = method.getSource().getParameters().size();
        int iovars = parameters + returns;

        NumericValue dataComplexity = NumericValue.of(iovars).divide(NumericValue.ONE.plus(NumericValue.of(fanOut)));
        NumericValue structuralComplexity = NumericValue.of(fanOut).pow(2);
        NumericValue systemComplexity = dataComplexity.plus(structuralComplexity.divide(NumericValue.ONE));

        return ImmutableSet.of(
                Metric.of("Fout", "Fan-out", fanOut),
                Metric.of("Fin", "Fan-in", fanIn),
                Metric.of("Si", "Structural Complexity", structuralComplexity),
                Metric.of("IOVars", "Input/Output Variables", iovars),
                Metric.of("Di", "Data Complexity", dataComplexity),
                Metric.of("Ci", "System Complexity", systemComplexity)
        );


    }
    
    private static LoadingCache<Project, Network<Method, Distinct<Expression>>> callNetwork = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<Project, Network<Method, Distinct<Expression>>>() {
            @Override
            public Network<Method, Distinct<Expression>> load(Project parentProject) throws Exception {
                MutableNetwork<Method, Distinct<Expression>> network = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();

                Set<Method> allMethods = parentProject.getPackages()
                        .stream()
                        .map(Package::getTypes)
                        .flatMap(Set::stream).map(Type::getMethods).flatMap(Set::stream).collect(Collectors.toSet());

                for (Method method : allMethods) {
                    network.addNode(method);

                    List<MethodCallExpr> calls = method.getSource().findAll(MethodCallExpr.class);

                    for (MethodCallExpr methodCall : calls) {

                        Optional<Method> methodCalled = getMethodCalledByMethodExpression(method, methodCall);

                        if (methodCalled.isPresent()) {
                            network.addEdge(method, methodCalled.orElse(Method.UNKNOWN), Distinct.of(methodCall));
                        }

                    }


                    //TODO: Track these as well
                    //List<MethodReferenceExpr> references = method.getSource().findAll(MethodReferenceExpr.class);
                }


                return ImmutableNetwork.copyOf(network);
            }
        });

    private static Optional<Method> getMethodCalledByMethodExpression(Method containingMethod, MethodCallExpr methodCall) {
        try {
            ResolvedMethodDeclaration blah = methodCall.resolve();
            ResolvedReferenceTypeDeclaration declaringType = blah.declaringType();

            Project project = containingMethod.getParentType().getParentPackage().getParentProject();
            Optional<Package> pkg = project.lookupPackageByName(declaringType.getPackageName());
            if (!pkg.isPresent()) return Optional.empty();

            Optional<Type> typ = pkg.get().lookupTypeByName(declaringType.getName());

            if (!typ.isPresent()) return Optional.empty();

            Optional<Method> method = typ.get().lookupMethodBySignature(blah.getSignature());

            return method;
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
