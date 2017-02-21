package org.jasome.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.input.Package;
import org.jasome.input.Project;
import org.jasome.input.Type;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CalculationUtils {
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

                    boolean anyDirectAccess = fieldAccesses.parallelStream().anyMatch(fieldAccessExpr -> fieldAccessExpr.getName().equals(variable.getName()));

                    if (anyDirectAccess) return true;
                    else {
                        List<NameExpr> nameAccesses = method.getBody().get().getNodesByType(NameExpr.class);

                        boolean anyIndirectAccess = nameAccesses
                                .parallelStream()
                                .anyMatch(nameAccessExpr -> {


                                    List<BlockStmt> allBlocksFromMethodDeclarationToNameAccessExpr = getAllVariableDefinitionScopesBetweenMethodDefinitionAndNode(nameAccessExpr);

                                    List<VariableDeclarator> variablesDefinedInMethod = method.getNodesByType(VariableDeclarator.class);

                                    boolean isVariableRedefinedInScope = variablesDefinedInMethod
                                            .parallelStream()
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
                                            .parallelStream()
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

    private static LoadingCache<Project, Graph<Type>> inheritanceGraph = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Project, Graph<Type>>() {
                @Override
                public Graph<Type> load(Project project) throws Exception {
                    MutableGraph<Type> graph = GraphBuilder.directed().build();

                    Map<String, Type> allClassesByName = project.getPackages()
                            .parallelStream()
                            .map(Package::getTypes)
                            .flatMap(Set::stream)
                            .collect(Collectors.toMap(Type::getName, t -> t));

                    //TODO: make this a little smarter - basically when adding an edge based on a use, see if that 'use' is
                    //of a class that has the same name in multiple packages.  if so, use the one in the current type's package.
                    //otherwise just pick one deterministically, we're not parsing import statements.  Maybe pick the one
                    //whose package name has the most characters in common with the types ("closer"). What to do if equal?
                    //Can they be equal?  would that mean that both target types have the same package, which wouldn't be allowed?

                    for (Type type : allClassesByName.values()) {
                        List<ClassOrInterfaceType> extendedTypes = type.getSource().getExtendedTypes();
                        List<ClassOrInterfaceType> implementedTypes = type.getSource().getImplementedTypes();

                        for (ClassOrInterfaceType extendedType : extendedTypes) {
                            if (allClassesByName.containsKey(extendedType.getName().getIdentifier())) {
                                graph.putEdge(allClassesByName.get(extendedType.getName().getIdentifier()), type);
                            }
                        }

                        for (ClassOrInterfaceType implementedType : implementedTypes) {
                            if (allClassesByName.containsKey(implementedType.getName().getIdentifier())) {
                                graph.putEdge(allClassesByName.get(implementedType.getName().getIdentifier()), type);
                            }
                        }
                    }
                    return graph;
                }
            });

    public static Graph<Type> getInheritanceGraph(Project parentProject) {
        MutableGraph<Type> graph = GraphBuilder.directed().build();

        Multimap<String, Type> allClassesByName = HashMultimap.create();

        parentProject.getPackages()
                .parallelStream()
                .map(Package::getTypes)
                .flatMap(Set::stream)
                .forEach(type -> allClassesByName.put(type.getName(), type));
        
        for (Type type : allClassesByName.values()) {
            List<ClassOrInterfaceType> extendedTypes = type.getSource().getExtendedTypes();
            List<ClassOrInterfaceType> implementedTypes = type.getSource().getImplementedTypes();

            for (ClassOrInterfaceType extendedType : extendedTypes) {
                if (allClassesByName.containsKey(extendedType.getName().getIdentifier())) {
                    Optional<Type> closestType = getClosestTypeWithName(extendedType.getName().getIdentifier(), type, allClassesByName);

                    closestType.ifPresent(c ->
                        graph.putEdge(c, type)
                    );
                }
            }

            for (ClassOrInterfaceType implementedType : implementedTypes) {
                if (allClassesByName.containsKey(implementedType.getName().getIdentifier())) {
                    Optional<Type> closestType = getClosestTypeWithName(implementedType.getName().getIdentifier(), type, allClassesByName);

                    closestType.ifPresent(c ->
                        graph.putEdge(c, type)
                    );
                }
            }
        }
        return graph;
    }

    private static Optional<Type> getClosestTypeWithName(String identifier, Type source, Multimap<String, Type> allClassesByName) {
        Collection<Type> matchingTypes = allClassesByName.get(identifier);
        List<Type> matchingTypesSortedByNumberOfCharactersInCommonWithSourcePackage = matchingTypes.stream().sorted(new Comparator<Type>() {
            @Override
            public int compare(Type t1, Type t2) {
                int firstCharsInCommon = StringUtils.getCommonPrefix(source.getParentPackage().getName(), t1.getParentPackage().getName()).length();
                int secondCharsInCommon = StringUtils.getCommonPrefix(source.getParentPackage().getName(), t2.getParentPackage().getName()).length();

                return ((Integer) firstCharsInCommon).compareTo(secondCharsInCommon);
            }
        }).collect(Collectors.toList());

        if(matchingTypesSortedByNumberOfCharactersInCommonWithSourcePackage.size() > 0) {
            return Optional.of(matchingTypesSortedByNumberOfCharactersInCommonWithSourcePackage.get(0));
        } else {
            return Optional.empty();
        }
        
    }
}
