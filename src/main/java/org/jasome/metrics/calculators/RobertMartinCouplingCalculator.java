package org.jasome.metrics.calculators;

import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.collect.ImmutableSet;
import org.jasome.input.Package;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;
import org.jasome.metrics.value.NumericValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RobertMartinCouplingCalculator implements Calculator<Package> {
    @Override
    public Set<Metric> calculate(Package aPackage) {
        Map<String, List<Type>> allClassesOutsideOfPackage = aPackage.getParentProject().getPackages()
                .parallelStream()
                .filter(p -> p != aPackage)
                .map(Package::getTypes)
                .flatMap(Set::stream)
                .filter(type -> type.getSource().isPublic()) //only public classes count, nothing else is visible outside of the package)
                .collect(Collectors.groupingBy(Type::getName));

        Map<String, Type> allClassesInsideOfPackage = aPackage.getTypes()
                .parallelStream()
                .filter(type -> type.getSource().isPublic())
                .collect(Collectors.toMap(Type::getName, t -> t));

        NumericValue afferentCoupling = NumericValue.ZERO; //The number of classes outside a package that depend on classes inside the package.
        NumericValue efferentCoupling = NumericValue.ZERO; //The number of classes inside a package that depend on classes outside the package.

        for (Type typeInsidePackage : aPackage.getTypes()) {
            List<ClassOrInterfaceType> referencedTypes = typeInsidePackage.getSource().getNodesByType(ClassOrInterfaceType.class);

            long numberOfTypesReferencedThatAreInsideAnotherPackage = referencedTypes
                    .parallelStream()
                    .map(typ -> typ.getName().getIdentifier())
                    .filter(typeName ->
                            allClassesOutsideOfPackage.containsKey(typeName) && !allClassesInsideOfPackage.containsKey(typeName)
                    ).count();

            //TODO: this should be more restrictive, we only need these inside of certain kinds of expressions
            List<SimpleName> referencedNames = typeInsidePackage.getSource().getNodesByType(SimpleName.class);

            long numberOfSimpleNamesReferencedThatCorrespondToTypesInsideAnotherPackage = referencedNames
                    .parallelStream()
                    .map(SimpleName::getIdentifier)
                    .filter(typeName ->
                            allClassesOutsideOfPackage.containsKey(typeName) && !allClassesInsideOfPackage.containsKey(typeName)
                    ).count();

            if (numberOfTypesReferencedThatAreInsideAnotherPackage + numberOfSimpleNamesReferencedThatCorrespondToTypesInsideAnotherPackage > 0) {
                efferentCoupling = efferentCoupling.plus(NumericValue.ONE);
            }
        }

        for (List<Type> typesOutsidePackage : allClassesOutsideOfPackage.values()) {
            for (Type typeOutsidePackage : typesOutsidePackage) {
                List<ClassOrInterfaceType> referencedTypes = typeOutsidePackage.getSource().getNodesByType(ClassOrInterfaceType.class);

                long numberOfTypesReferencedThatAreInsideThisPackage = referencedTypes
                        .parallelStream()
                        .map(typ -> typ.getName().getIdentifier())
                        .filter(typeName ->
                                allClassesInsideOfPackage.containsKey(typeName) && !allClassesOutsideOfPackage.containsKey(typeName)
                        ).count();

                List<SimpleName> referencedNames = typeOutsidePackage.getSource().getNodesByType(SimpleName.class);

                long numberOfSimpleNamesReferencedThatCorrespondToTypesInsideThisPackage = referencedNames
                        .parallelStream()
                        .map(SimpleName::getIdentifier)
                        .filter(typeName ->
                                allClassesInsideOfPackage.containsKey(typeName) && !allClassesOutsideOfPackage.containsKey(typeName)
                        ).count();

                if (numberOfTypesReferencedThatAreInsideThisPackage + numberOfSimpleNamesReferencedThatCorrespondToTypesInsideThisPackage > 0) {
                    afferentCoupling = afferentCoupling.plus(NumericValue.ONE);
                }
            }
        }

        ImmutableSet.Builder<Metric> metrics = ImmutableSet.<Metric>builder()
                .add(Metric.of("Ca", "Afferent Coupling", afferentCoupling))
                .add(Metric.of("Ce", "Efferent Coupling", efferentCoupling));

        Optional<NumericValue> instabilityOpt = Optional.ofNullable(
                afferentCoupling.plus(efferentCoupling).isGreaterThan(NumericValue.ZERO) ?
                        efferentCoupling.divide(afferentCoupling.plus(efferentCoupling))
                        : null
        );

        instabilityOpt.ifPresent(i -> metrics.add(Metric.of("I", "Instability", i)));

        NumericValue numberOfAbstractClassesAndInterfacesInPackage = NumericValue.of(
                aPackage.getTypes()
                        .parallelStream()
                        .filter(type -> type.getSource().isInterface() || type.getSource().isAbstract())
                        .count()
        );

        metrics.add(Metric.of("NOI", "Number of Interfaces and Abstract Classes", numberOfAbstractClassesAndInterfacesInPackage));

        Optional<NumericValue> abstractnessOpt = Optional.ofNullable(
                aPackage.getTypes().size() > 0 ?
                        numberOfAbstractClassesAndInterfacesInPackage.divide(NumericValue.of(aPackage.getTypes().size()))
                        : null
        );


        abstractnessOpt.ifPresent(a -> metrics.add(Metric.of("A", "Abstractness", a)));


        if (instabilityOpt.isPresent() && abstractnessOpt.isPresent()) {
            metrics.add(Metric.of("DMS", "Normalized Distance from Main Sequence", abstractnessOpt.get().plus(instabilityOpt.get()).minus(NumericValue.ONE).abs()));
        }

        return metrics.build();


    }
}
