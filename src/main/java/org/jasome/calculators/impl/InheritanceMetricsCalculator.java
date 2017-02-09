package org.jasome.calculators.impl;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;
import org.jasome.parsing.Method;
import org.jasome.parsing.Package;
import org.jasome.parsing.Type;

import java.util.*;

public class InheritanceMetricsCalculator implements Calculator<Type> {


    @Override
    public Set<Metric> calculate(Type type) {

        Multimap<String, Type> allTypesByName = HashMultimap.create();
        for (Package aPackage : type.getParentPackage().getParentProject().getPackages()) {
            for (Type aType : aPackage.getTypes()) {
                allTypesByName.put(aType.getName(), aType);
            }
        }

        int depth = calculateInheritanceDepth(type, allTypesByName);

        int overriddenMethods = calculateNumberofOverriddenMethods(type, allTypesByName);

        return Metric.builder()
                .with("DIT", "Depth of Inheritance Tree", depth)
                .with("NORM", "Number of Overridden Methods", overriddenMethods)
                .build();
    }

    private int calculateNumberofOverriddenMethods(Type type, Multimap<String, Type> allTypesByName) {

        Set<String> parentMethods = new HashSet<String>();

        Stack<Type> typesToCheck = new Stack<>();

        typesToCheck.addAll(getParentTypes(type, allTypesByName, true));

        while(!typesToCheck.empty()) {
            Type typeToCheck = typesToCheck.pop();

            for(Method method: typeToCheck.getMethods()) {
                parentMethods.add(method.getName());
            }

            typesToCheck.addAll(getParentTypes(typeToCheck, allTypesByName, true));
        }

        int numberOverridden = 0;

        //TODO: this would be better if it didn't just check for the name, but instead considered a method "overridden" if the parent
        //simply uses the same name, return type, same number of parameters, and the same types for those parameters (because you can rename the param)
        //Also it might be worth basically saying all object types count as the same type, since you can override methods with
        //more specific versions
        for(Method m: type.getMethods()) {
            if(parentMethods.contains(m.getName())) {
                numberOverridden++;
            }
        }

        return numberOverridden;
    }

    private Set<Type> getParentTypes(Type type, Multimap<String, Type> allTypesByName, boolean includeInterfaceTypes) {
        Set<ClassOrInterfaceType> parentClasses = new HashSet<>();

        parentClasses.addAll(type.getSource().getExtendedTypes());
        if(includeInterfaceTypes) {
            parentClasses.addAll(type.getSource().getImplementedTypes());
        }

        Set<Type> parentTypes = new HashSet<Type>();

        for (ClassOrInterfaceType parentClass : parentClasses) {
            Collection<Type> nextLevelTypes = allTypesByName.get(parentClass.getName().toString());
            parentTypes.addAll(nextLevelTypes);
        }
        return parentTypes;
    }

    private int calculateInheritanceDepth(Type type, Multimap<String, Type> allTypesByName) {
        ClassOrInterfaceDeclaration decl = type.getSource();

        Collection<Type> nextLevelTypes = getParentTypes(type, allTypesByName, true);


        List<Integer> maximums = new ArrayList<>();

        for (Type t : nextLevelTypes) {
            maximums.add(1 + calculateInheritanceDepth(t, allTypesByName));
        }


        return maximums.stream().mapToInt(i -> i.intValue()).max().orElse(1);
    }
}
