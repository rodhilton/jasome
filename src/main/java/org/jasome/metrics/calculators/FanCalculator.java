package org.jasome.metrics.calculators;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import org.jasome.input.Method;
import org.jasome.input.Package;
import org.jasome.input.Project;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.util.CalculationUtils;
import org.jasome.util.Distinct;
import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Rational;
import org.pcollections.HashPMap;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.util.*;
import java.util.stream.Collectors;

public class FanCalculator implements Calculator<Method> {

    //private Set<Project> cached = new HashSet<>();

    @Override
    public Set<Metric> calculate(Method method) {

        //Project rootProject = method.getParentType().getParentPackage().getParentProject();
//
//        if(!cached.contains(rootProject)) {
//            System.out.println("in cache check for "+rootProject);
//            cached.add(rootProject);
//        }


        //Network<Method, Distinct<Expression>> methodCalls = buildCallNetwork(rootProject);


        Network<Method, Distinct<Expression>> methodCalls = CalculationUtils.callNetwork.getUnchecked(method.getParentType().getParentPackage().getParentProject());
//        Network<Method, Distinct<Expression>> methodCalls = CalculationUtils.getCallNetwork(rootProject);

        Set<Method> methodsCalled = methodCalls.successors(method);

        int fanOut = 0;

        for(Method methodCalled: methodsCalled) {
            Set<Distinct<Expression>> calls = methodCalls.edgesConnecting(method, methodCalled);
            fanOut += calls.size();
        }

        Set<Method> methodsCalling = methodCalls.predecessors(method);

        int fanIn = 0;

        for(Method methodCalling: methodsCalling) {
            Set<Distinct<Expression>> calls = methodCalls.edgesConnecting(methodCalling, method);
            fanIn += calls.size();
        }



        return ImmutableSet.of(
                Metric.of("Fout", "Fan-out", fanOut),
                Metric.of("Si", "Structural Complexity", LargeInteger.valueOf(fanOut).pow(2) ),
                Metric.of("Fin", "Fan-in", fanIn)
        );


    }

    private Network<Method, Distinct<Expression>> buildCallNetwork(Project rootProject) {
        MutableNetwork<Method, Distinct<Expression>> network = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();

        Set<Type> allClasses = rootProject.getPackages()
                .stream()
                .map(Package::getTypes)
                .flatMap(Set::stream).collect(Collectors.toSet());

        //Build a map of class names to a list of packages containing that name
        HashMap<String, List<Type>> classNamesToTypesWithThatName = new HashMap<>();
        for(Type type: allClasses) {
            classNamesToTypesWithThatName.putIfAbsent(type.getName(), new ArrayList<>());
            classNamesToTypesWithThatName.get(type.getName()).add(type);
        }

        System.out.println(classNamesToTypesWithThatName);

        for(Type type: allClasses) {
            PMap<String, Type> variablesInScope = HashTreePMap.empty();
            List<FieldDeclaration> fieldDeclarations = type.getSource().getFields();

            Map<String, Type> fields = new HashMap<>();
            for(FieldDeclaration decl: fieldDeclarations) {
                com.github.javaparser.ast.type.Type declaredType = decl.getCommonType();
                for(VariableDeclarator varDecl: decl.getVariables()) {
                    String name = varDecl.getName().getIdentifier();
                    //fields.put(name, selectClosestType(declaredType.get))
                    System.out.println(name);
                }
            }

            //this gives us the types, (not done) but we actually needs methods on those types for a call network


            //fieldDeclarations.stream().collect(Collectors.toMap(FieldDeclaration::get, item -> item));

        }

        return null;
    }
}
