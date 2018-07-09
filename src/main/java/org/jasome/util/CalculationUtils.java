package org.jasome.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.input.Method;
import org.jasome.input.Package;
import org.jasome.input.Project;
import org.jasome.input.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CalculationUtils {
    //TODO: Can likely make this faster/more accurate using java resolver
    public static LoadingCache<Pair<MethodDeclaration, VariableDeclarator>, Boolean> isFieldAccessedWithinMethod = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Pair<MethodDeclaration, VariableDeclarator>, Boolean>() {
                @Override
                public Boolean load(Pair<MethodDeclaration, VariableDeclarator> key) throws Exception {
                    MethodDeclaration method = key.getLeft();
                    VariableDeclarator variable = key.getRight();

                    if (!method.getBody().isPresent()) return false;

                    List<FieldAccessExpr> fieldAccesses = method.getBody().get().getNodesByType(FieldAccessExpr.class);

                    //If we have a field match we can just count it, it's directly prefixed with 'this.' so there's no room for shadowing

                    boolean anyDirectAccess = fieldAccesses.stream().anyMatch(fieldAccessExpr -> fieldAccessExpr.getName().equals(variable.getName()));

                    if (anyDirectAccess) return true;
                    else {
                        List<NameExpr> nameAccesses = method.getBody().get().getNodesByType(NameExpr.class);

                        boolean anyIndirectAccess = nameAccesses
                                .stream()
                                .anyMatch(nameAccessExpr -> {


                                    List<BlockStmt> allBlocksFromMethodDeclarationToNameAccessExpr = getAllVariableDefinitionScopesBetweenMethodDefinitionAndNode(nameAccessExpr);

                                    List<VariableDeclarator> variablesDefinedInMethod = method.getNodesByType(VariableDeclarator.class);

                                    boolean isVariableRedefinedInScope = variablesDefinedInMethod
                                            .stream()
                                            .anyMatch(variableDeclaration -> {
                                                //if any of these variables have all their parents in the allBlocks list above, then that variable shadows nameExpr (as long as the name matches)
                                                //It essentially means that this variable declaration is BETWEEN the variable access and the method declaration, which means this variable
                                                //shadows the field when doing the name access.  If this variable declaration were LOWER on the nesting chain or divergent entirely, it would
                                                //have at least one block between it and the method declaration that ISN'T between the name access and the method

                                                if (variableDeclaration.getName().equals(nameAccessExpr.getName())) {
                                                    List<BlockStmt> allBlocksFromMethodDeclarationToVariableDeclaration = getAllVariableDefinitionScopesBetweenMethodDefinitionAndNode(variableDeclaration);
                                                    return allBlocksFromMethodDeclarationToNameAccessExpr.containsAll(allBlocksFromMethodDeclarationToVariableDeclaration);
                                                } else {
                                                    return false;
                                                }
                                            });

                                    boolean isVariableRedefinedByMethodSignature = method.getParameters()
                                            .stream()
                                            .anyMatch(parameter -> parameter.getName().equals(nameAccessExpr.getName()));


                                    if (isVariableRedefinedInScope || isVariableRedefinedByMethodSignature) {
                                        return false;
                                    } else {
                                        return nameAccessExpr.getName().equals(variable.getName());
                                    }
                                });

                        if (anyIndirectAccess) return true;
                    }


                    return false;
                }

            });


    public static LoadingCache<Project, Graph<Type>> clientNetwork = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Project, Graph<Type>>() {
                       @Override
                       public Graph<Type> load(Project project) throws Exception {

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
                   }
            );

    public static LoadingCache<Project, Network<Method, Distinct<Expression>>> callNetwork = CacheBuilder.newBuilder()
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

                            Optional<Method> methodCalled = getMethodCalledByMethodExpression(parentProject, methodCall);

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
            });


    private static List<BlockStmt> getAllVariableDefinitionScopesBetweenMethodDefinitionAndNode(Node theNode) {
        List<BlockStmt> blocksOnPathToMethodDeclaration = new ArrayList<>();

        while (!(theNode instanceof MethodDeclaration)) {

            if (theNode instanceof BlockStmt) {
                blocksOnPathToMethodDeclaration.add((BlockStmt) theNode);
            }

            if (theNode.getParentNode().isPresent()) {
                theNode = theNode.getParentNode().get();
            } else {
                break;
            }
        }

        return blocksOnPathToMethodDeclaration;
    }

    public static LoadingCache<Project, Graph<Type>> inheritanceGraph = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Project, Graph<Type>>() {
                @Override
                public Graph<Type> load(Project parentProject) throws Exception {
                    MutableGraph<Type> graph = GraphBuilder.directed().build();

                    Multimap<String, Type> allClassesByName = HashMultimap.create();

                    parentProject.getPackages()
                            .stream()
                            .map(Package::getTypes)
                            .flatMap(Set::stream)
                            .forEach(type -> allClassesByName.put(type.getName(), type));

                    Set<Type> allClasses = parentProject.getPackages()
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
                                Optional<Type> closestType = CalculationUtils.lookupType(parentProject, refType);

                                closestType.ifPresent(c ->
                                        graph.putEdge(c, type)
                                );
                            } catch (UnsolvedSymbolException e) {
                                //Ignore if a symbol can't be resolved
                            }

                        }
                    }
                    return ImmutableGraph.copyOf(graph);
                }
            });

    private static Optional<Type> lookupType(Project parentProject, ResolvedReferenceType refType) {
        try {
            Optional<Package> optPackage = parentProject.lookupPackageByName(refType.getTypeDeclaration().getPackageName());
            return optPackage.flatMap(pkg -> pkg.lookupTypeByName(refType.getTypeDeclaration().getClassName()));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

//    private static Optional<Method> getConstructorCalledByConstructorCall(Method containingMethod, ObjectCreationExpr constructorCall) {
//        try {
//            ResolvedConstructorDeclaration blah = constructorCall.resolve();
//            ResolvedReferenceTypeDeclaration declaringType = blah.declaringType();
//
//            Project project = containingMethod.getParentType().getParentPackage().getParentProject();
//            Optional<Package> pkg = project.lookupPackageByName(declaringType.getPackageName());
//            if (!pkg.isPresent()) return Optional.empty();
//
//            Optional<Type> typ = pkg.get().lookupTypeByName(declaringType.getName());
//
//            if (!typ.isPresent()) return Optional.empty();
//
//            Optional<Method> method = typ.get().lookupMethodBySignature(blah.getSignature());
//
//            return method;
//        } catch (Exception e) {
//            return Optional.empty();
//        }
//    }

    private static Optional<Method> getMethodCalledByMethodExpression(Project project, MethodCallExpr methodCall) {
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
