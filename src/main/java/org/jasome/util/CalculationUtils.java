package org.jasome.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import com.google.common.graph.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.input.Method;
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

        Set<Type> allClasses = parentProject.getPackages()
                .parallelStream()
                .map(Package::getTypes)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        
        for (Type type :allClasses) {
            List<ClassOrInterfaceType> extendedTypes = type.getSource().getExtendedTypes();
            List<ClassOrInterfaceType> implementedTypes = type.getSource().getImplementedTypes();

            graph.addNode(type);

            List<ClassOrInterfaceType> parentTypes = new ArrayList<>();
            parentTypes.addAll(extendedTypes);
            parentTypes.addAll(implementedTypes);

            for (ClassOrInterfaceType parentType : parentTypes) {
                    Optional<Type> closestType = getClosestTypeWithName(parentType.getName(), type);

                    closestType.ifPresent(c ->
                        graph.putEdge(c, type)
                    );
            }
        }
        return ImmutableGraph.copyOf(graph);
    }

    public static Network<Method, Distinct<Expression>> getCallNetwork(Project parentProject) {
        MutableNetwork<Method, Distinct<Expression>> network = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();

//        Set<Method> allClassesByName = new HashSet<>();

        Set<Method> allMethods = parentProject.getPackages()
                .parallelStream()
                .map(Package::getTypes)
                .flatMap(Set::stream).map(Type::getMethods).flatMap(Set::stream).collect(Collectors.toSet());

        for(Method method: allMethods) {
            network.addNode(method);

            List<MethodCallExpr> calls = method.getSource().getNodesByType(MethodCallExpr.class);
            List<MethodReferenceExpr> references = method.getSource().getNodesByType(MethodReferenceExpr.class);

            for(MethodCallExpr methodCall: calls) {

                Optional<Method> methodCalled;

                methodCalled = getMethodCalledByMethodExpression(method, methodCall);

                network.addEdge(method, methodCalled.orElse(Method.UNKNOWN), Distinct.of(methodCall));

            }
        }


        return ImmutableNetwork.copyOf(network);
    }

    private static Optional<Method> getMethodCalledByMethodExpression(Method containingMethod, MethodCallExpr methodCall) {
        Optional<Expression> methodCallScope = methodCall.getScope();

        Optional<Type> scopeType  = determineTypeOf(methodCallScope.orElse(null), containingMethod);

        if(scopeType.isPresent()) {
            List<Method> inClassMethods = scopeType.get().getMethods().stream().collect(Collectors.toList());
            Optional<Method> matching = inClassMethods.stream().filter(m->
                    m.getSource().getName().equals(methodCall.getName()) && m.getSource().getParameters().size() == methodCall.getArguments().size()
            ).findFirst();

            return matching;
        }

        return Optional.empty();
    }

    private static Optional<Type> determineTypeOf(Expression scope, Method containingMethod) {
        if(scope == null || scope instanceof ThisExpr) return Optional.of(containingMethod.getParentType());

        if(scope instanceof NameExpr) {
            SimpleName variableName = ((NameExpr)scope).getName();
            Optional<com.github.javaparser.ast.type.Type> nameType = findTypeOfVariableDeclaration(variableName, scope);

            if(nameType.isPresent()) {
                if(nameType.get() instanceof ClassOrInterfaceType) {
                    Optional<Type> closestType = getClosestType((ClassOrInterfaceType) nameType.get(), containingMethod.getParentType());
                    return closestType;
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }

        }

        return Optional.empty();

    }

    private static Optional<com.github.javaparser.ast.type.Type> findTypeOfVariableDeclaration(SimpleName variableName, Node scope) {
        List<Node> scopeParents = getAllParentsUpToClassDefinition(scope);

        Node topMostNode = scopeParents.get(0);


        com.github.javaparser.ast.type.Type typeCandidate = null;
        int longestChainLength = 0;

        List<VariableDeclarator> variableDeclarations = topMostNode.getNodesByType(VariableDeclarator.class);
        for(VariableDeclarator variableDeclarator: variableDeclarations) {
            List<Node> variableParents = getAllParentsUpToClassDefinition(variableDeclarator);
            if(scopeParents.containsAll(variableParents)) {
                //This variable is defined in the scope we care about
                if(variableParents.size() > longestChainLength) {
                    typeCandidate = variableDeclarator.getType();
                    longestChainLength = variableParents.size();
                }
            }
        }

        List<VariableDeclarationExpr> variableDeclarationExprs = topMostNode.getNodesByType(VariableDeclarationExpr.class);
        for(VariableDeclarationExpr variableDeclarationExpr: variableDeclarationExprs) {
            List<Node> variableParents = getAllParentsUpToClassDefinition(variableDeclarationExpr);
            if(scopeParents.containsAll(variableParents)) {
                //This variable is defined in the scope we care about
                if(variableParents.size() > longestChainLength) {
                    typeCandidate = variableDeclarationExpr.getCommonType();
                    longestChainLength = variableParents.size();
                }
            }
        }

        List<Parameter> parameters = topMostNode.getNodesByType(Parameter.class);
        for(Parameter parameter: parameters) {
            List<Node> parameterParents = getAllParentsUpToClassDefinition(parameter);
            if(scopeParents.containsAll(parameterParents)) {
                //This variable is defined in the scope we care about
                if(parameterParents.size() > longestChainLength) {
                    typeCandidate = parameter.getType();
                    longestChainLength = parameterParents.size();
                }
            }
        }

        return Optional.ofNullable(typeCandidate);
    }

    private static List<Node> getAllParentsUpToClassDefinition(Node scope) {
        List<Node> parents = new ArrayList<>();
        while(scope.getParentNode().isPresent()) {
            Node parent = scope.getParentNode().get();
            parents.add(parent);
            scope = parent;
        }
        return Lists.reverse(parents);
    }

    //TODO: this desperately needs to be cached
    private static Optional<Type> getClosestTypeWithName(SimpleName identifier, Type source) {
        Multimap<SimpleName, Type> allClassesByName = HashMultimap.create();

        source.getParentPackage().getParentProject().getPackages()
                .parallelStream()
                .map(Package::getTypes)
                .flatMap(Set::stream)
                .forEach(type -> allClassesByName.put(type.getSource().getName(), type));


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

    private static Optional<Type> getClosestType(ClassOrInterfaceType target, Type source) {
        Multimap<SimpleName, Type> allClassesByName = HashMultimap.create();

        source.getParentPackage().getParentProject().getPackages()
                .parallelStream()
                .map(Package::getTypes)
                .flatMap(Set::stream)
                .forEach(type -> allClassesByName.put(type.getSource().getName(), type));


        Collection<Type> matchingTypes = allClassesByName.get(target.getName());
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
