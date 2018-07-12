package org.jasome.util;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.*;
import org.jasome.input.Method;
import org.jasome.input.Package;
import org.jasome.input.Project;
import org.jasome.input.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ProjectMetadata {

    private Project project;

    public ProjectMetadata(Project project) {
        this.project = project;
    }

    private volatile Graph<Type> inheritanceGraph;

    public Graph<Type> getInheritanceGraph() {
        if (inheritanceGraph == null) {
            synchronized(this) {
                if (inheritanceGraph == null) {
                    inheritanceGraph = buildInheritanceGraph();
                }
            }
        }
        return inheritanceGraph;
    }

    private Graph<Type> buildInheritanceGraph() {
        MutableGraph<Type> graph = GraphBuilder.directed().build();

        Multimap<String, Type> allClassesByName = HashMultimap.create();

        project.getPackages()
                .stream()
                .map(Package::getTypes)
                .flatMap(Set::stream)
                .forEach(type -> allClassesByName.put(type.getName(), type));

        Set<Type> allClasses = project.getPackages()
                .stream()
                .map(Package::getTypes)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        for (Type type : allClasses) {
            List<ClassOrInterfaceType> extendedTypes = type.getSource().getExtendedTypes();
            List<ClassOrInterfaceType> implementedTypes = type.getSource().getImplementedTypes();

            graph.addNode(type);

            List<ClassOrInterfaceType> parentTypes = new ArrayList<>();
            parentTypes.addAll(extendedTypes);
            parentTypes.addAll(implementedTypes);

            for (ClassOrInterfaceType parentType : parentTypes) {
                try {
                    ResolvedReferenceType refType = parentType.resolve();
                    Optional<Type> closestType = CalculationUtils.lookupType(project, refType);

                    closestType.ifPresent(c ->
                            graph.putEdge(c, type)
                    );
                } catch (Exception e) {
                    //Ignore if a symbol can't be resolved
                }

            }
        }
        return ImmutableGraph.copyOf(graph);
    }

    private volatile Graph<Type> clientGraph;

    public Graph<Type> getClientGraph() {
        if (clientGraph == null) {
            synchronized(this) {
                if (clientGraph == null) {
                    clientGraph = buildClientGraph();
                }
            }
        }
        return clientGraph;
    }

    private Graph<Type> buildClientGraph() {

        MutableGraph<Type> dependencyGraph = GraphBuilder.directed().allowsSelfLoops(false).build();

        Set<Type> allTypes = project.getPackages()
                .stream()
                .map(Package::getTypes).flatMap(Set::stream).collect(Collectors.toSet());

        for (Type type : allTypes) {
            dependencyGraph.addNode(type);

            //We can have class uses via chained method calls without referencing one of the types directly
            List<MethodCallExpr> calls = type.getSource().findAll(MethodCallExpr.class);

            for (MethodCallExpr methodCall : calls) {
                try {
                    ResolvedMethodDeclaration resolvedMethodDeclaration = methodCall.resolve();
                    ResolvedReferenceTypeDeclaration declaringType = resolvedMethodDeclaration.declaringType();

                    String packageName = declaringType.getPackageName();
                    String className = declaringType.getName();

                    project
                            .lookupPackageByName(packageName)
                            .flatMap(pkg -> pkg.lookupTypeByName(className))
                            .ifPresent(referencedType -> {
                                if (type != referencedType)
                                    dependencyGraph.putEdge(type, referencedType);
                            });
                } catch (Exception e) {
                    //Ignore anything unresolvable
                }

            }

            List<ReferenceType> parameters = type.getSource().findAll(ReferenceType.class);

            for (ReferenceType parameter : parameters) {

                try {
                    ResolvedType declaration = parameter.resolve();
                    String packageName = declaration.asReferenceType().asReferenceType().getTypeDeclaration().getPackageName();
                    String className = declaration.asReferenceType().asReferenceType().getTypeDeclaration().getName();

                    project.
                            lookupPackageByName(packageName)
                            .flatMap(pkg -> pkg.lookupTypeByName(className))
                            .ifPresent(referencedType -> {
                                if (type != referencedType)
                                    dependencyGraph.putEdge(type, referencedType);
                            });
                } catch (Exception e) {
                    //Ignore anything unresolvable
                }
            }

        }


        return ImmutableGraph.copyOf(dependencyGraph);
    }

    private volatile Network<Method, Distinct<Expression>> callNetwork;

    public Network<Method, Distinct<Expression>> getCallNetwork() {
        if (callNetwork == null) {
            synchronized(this) {
                if (callNetwork == null) {
                    callNetwork = buildCallNetwork();
                }
            }
        }
        return callNetwork;
    }

    private Network<Method, Distinct<Expression>> buildCallNetwork() {
        MutableNetwork<Method, Distinct<Expression>> network = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();

        Set<Method> allMethods = project.getPackages()
                .stream()
                .map(Package::getTypes)
                .flatMap(Set::stream).map(Type::getMethods).flatMap(Set::stream).collect(Collectors.toSet());

        for (Method method : allMethods) {
            network.addNode(method);

            List<MethodCallExpr> calls = method.getSource().findAll(MethodCallExpr.class);

            for (MethodCallExpr methodCall : calls) {

                Optional<Method> methodCalled = getMethodCalledByMethodExpression(methodCall);

                if (methodCalled.isPresent()) {
                    network.addEdge(method, methodCalled.orElse(Method.UNKNOWN), Distinct.of(methodCall));
                }

            }


            //This is not straightforward because the constructor being called might not actually be a Method on the Type - if it's a
            //default constructor that isn't defined in the source it's still possible to call it in the code, but it wasn't parsed
            //and added to the type's list of methods

//                            List<ObjectCreationExpr> constructions = method.getSource().findAll(ObjectCreationExpr.class);
//
//                            for (ObjectCreationExpr constructorCall : constructions) {
//
//                                Optional<Method> constructorCalled = getConstructorCalledByConstructorCall(method, constructorCall);
//
//                                if (constructorCalled.isPresent()) {
//                                    network.addEdge(method, constructorCalled.orElse(Method.UNKNOWN), Distinct.of(constructorCall));
//                                }
//
//                            }


            //TODO: Track these as well
            //List<MethodReferenceExpr> references = method.getSource().findAll(MethodReferenceExpr.class);
        }

        //TODO: should also check for method calls in static initializers


        return ImmutableNetwork.copyOf(network);
    }


    private Optional<Method> getMethodCalledByMethodExpression(MethodCallExpr methodCall) {
        try {
            ResolvedMethodDeclaration blah = methodCall.resolve();
            ResolvedReferenceTypeDeclaration declaringType = blah.declaringType();

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
