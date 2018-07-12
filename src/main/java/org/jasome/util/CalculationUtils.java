package org.jasome.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.apache.commons.lang3.tuple.Pair;
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
                            } catch (Exception e) {
                                //Ignore if a symbol can't be resolved
                            }

                        }
                    }
                    return ImmutableGraph.copyOf(graph);
                }
            });

    public static Optional<Type> lookupType(Project parentProject, ResolvedReferenceType refType) {
        try {
            Optional<Package> optPackage = parentProject.lookupPackageByName(refType.getTypeDeclaration().getPackageName());
            return optPackage.flatMap(pkg -> pkg.lookupTypeByName(refType.getTypeDeclaration().getClassName()));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

}
