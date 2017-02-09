package org.jasome.calculators.impl;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;
import org.jasome.parsing.Package;
import org.jasome.parsing.Type;

import java.util.*;

public class SpecializationIndexCalculator implements Calculator<Type> {


    @Override
    public Set<Metric> calculate(Type type) {

        Multimap<String, Type> allTypesByName = HashMultimap.create();
        for (Package aPackage : type.getParentPackage().getParentProject().getPackages()) {
            for (Type aType : aPackage.getTypes()) {
                allTypesByName.put(aType.getName(), aType);
            }
        }

        int depth = calculateInheritanceDepth(type, allTypesByName);

        return Metric.builder().with("DIT", "Depth of Inheritance Tree", depth).build();
    }

    private int calculateInheritanceDepth(Type type, Multimap<String, Type> allTypesByName) {
        ClassOrInterfaceDeclaration decl = type.getSource();

        Set<ClassOrInterfaceType> parentClasses = new HashSet<>();

        parentClasses.addAll(decl.getExtendedTypes());
        parentClasses.addAll(decl.getImplementedTypes());

        List<Integer> maximums = new ArrayList<>();
        for (ClassOrInterfaceType parentClass : parentClasses) {
            Collection<Type> nextLevelTypes = allTypesByName.get(parentClass.getName().toString());

            for (Type t : nextLevelTypes) {
                maximums.add(1 + calculateInheritanceDepth(t, allTypesByName));
            }
        }

        return maximums.stream().mapToInt(i -> i.intValue()).max().orElse(1);
    }
}
