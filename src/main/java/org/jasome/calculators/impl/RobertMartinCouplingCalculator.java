package org.jasome.calculators.impl;

import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.apfloat.Apfloat;
import org.jasome.calculators.Calculator;
import org.jasome.calculators.Metric;

import org.jasome.parsing.Package;
import org.jasome.parsing.Type;
import org.jscience.mathematics.number.Real;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RobertMartinCouplingCalculator implements Calculator<Package> {
    @Override
    public Set<Metric> calculate(Package aPackage) {
        Map<String, Type> allClassesOutsideOfPackage = aPackage.getParentProject().getPackages()
                .stream()
                .filter(p->p != aPackage)
                .map(Package::getTypes)
                .flatMap(Set::stream)
                .filter(type -> type.getSource().isPublic()) //only public classes count, nothing else is visible outside of the  package
                .collect(Collectors.toMap(Type::getName, t->t));

        Map<String, Type> allClassesInsideOfPackage = aPackage.getTypes()
                .stream()
                .filter(type -> type.getSource().isPublic())
                .collect(Collectors.toMap(Type::getName, t->t));

        Apfloat afferentCoupling = Apfloat.ZERO; //The number of classes outside a package that depend on classes inside the package.
        Apfloat efferentCoupling = Apfloat.ZERO; //The number of classes inside a package that depend on classes outside the package.

        for(Type typeInsidePackage: aPackage.getTypes()) {
            List<ClassOrInterfaceType> referencedTypes = typeInsidePackage.getSource().getNodesByType(ClassOrInterfaceType.class);

            long numberOfTypesReferencedThatAreInsideAnotherPackage = referencedTypes
                    .stream()
                    .map(typ->typ.getName().getIdentifier())
                    .filter(typeName ->
                            allClassesOutsideOfPackage.containsKey(typeName) && !allClassesInsideOfPackage.containsKey(typeName)
                    ).count();

            //TODO: this should be more restrictive, we only need these inside of certain kinds of expressions
            List<SimpleName> referencedNames = typeInsidePackage.getSource().getNodesByType(SimpleName.class);

            long numberOfSimpleNamesReferencedThatCorrespondToTypesInsideAnotherPackage = referencedNames
                    .stream()
                    .map(typ->typ.getIdentifier())
                    .filter(typeName ->
                            allClassesOutsideOfPackage.containsKey(typeName) && !allClassesInsideOfPackage.containsKey(typeName)
                    ).count();

            if(numberOfTypesReferencedThatAreInsideAnotherPackage + numberOfSimpleNamesReferencedThatCorrespondToTypesInsideAnotherPackage > 0) {
                efferentCoupling = efferentCoupling.add(Apfloat.ONE);
            }
        }

        for(Type typeOutsidePackage: allClassesOutsideOfPackage.values()) {
            List<ClassOrInterfaceType> referencedTypes = typeOutsidePackage.getSource().getNodesByType(ClassOrInterfaceType.class);

            long numberOfTypesReferencedThatAreInsideThisPackage = referencedTypes
                    .stream()
                    .map(typ->typ.getName().getIdentifier())
                    .filter(typeName ->
                            allClassesInsideOfPackage.containsKey(typeName) && !allClassesOutsideOfPackage.containsKey(typeName)
                    ).count();

            List<SimpleName> referencedNames = typeOutsidePackage.getSource().getNodesByType(SimpleName.class);

            long numberOfSimpleNamesReferencedThatCorrespondToTypesInsideThisPackage = referencedNames
                    .stream()
                    .map(typ->typ.getIdentifier())
                    .filter(typeName ->
                            allClassesInsideOfPackage.containsKey(typeName) && !allClassesOutsideOfPackage.containsKey(typeName)
                    ).count();

            if(numberOfTypesReferencedThatAreInsideThisPackage + numberOfSimpleNamesReferencedThatCorrespondToTypesInsideThisPackage > 0) {
                afferentCoupling = afferentCoupling.add(Apfloat.ONE);
            }
        }

        Metric.Builder metrics = Metric.builder()
                .with("Ca", "Afferent Coupling", afferentCoupling)
                .with("Ce", "Efferent Coupling", efferentCoupling);

        Apfloat instabilityDenominator = afferentCoupling.add(efferentCoupling);
        boolean instabilityCalculationSafe = instabilityDenominator.compareTo(Apfloat.ZERO) > 0;

        if(instabilityCalculationSafe) {
            metrics = metrics.with("I", "Instability", efferentCoupling.divide(instabilityDenominator));
        }

        Apfloat numberOfAbstractClassesAndInterfacesInPackage = new Apfloat(
                aPackage.getTypes()
                        .stream()
                        .filter(type -> type.getSource().isInterface() || type.getSource().isAbstract())
                        .count()
        );

        metrics = metrics.with("NOI", "Number of Interfaces and Abstract Classes", numberOfAbstractClassesAndInterfacesInPackage);

        Apfloat numberOfClassesInPackage = new Apfloat(aPackage.getTypes().size());

        boolean abstractnessCalculationSafe = numberOfClassesInPackage.compareTo(Apfloat.ZERO) > 0;

        if(abstractnessCalculationSafe) {
            metrics = metrics.with("A", "Abstractness", numberOfAbstractClassesAndInterfacesInPackage.divide(numberOfClassesInPackage));
        }

        if(instabilityCalculationSafe && abstractnessCalculationSafe) {
            //TODO duplication
            Apfloat instability = efferentCoupling.divide(instabilityDenominator);
            Apfloat abstractness = numberOfAbstractClassesAndInterfacesInPackage.divide(numberOfClassesInPackage);
            Apfloat distance = abstractness.add(instability).subtract(Apfloat.ONE);
            metrics = metrics.with("DMS", "Normalized Distance from Main Sequence", distance.compareTo(Apfloat.ZERO) < 0 ? distance.negate() : distance);
        }

        return metrics.build();


    }
}
