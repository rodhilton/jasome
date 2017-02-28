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
                .stream()
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

            //TODO: not using this yet
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

        Optional<Type> scopeType  = determineTypeOf_CACHED(methodCallScope.orElse(null), containingMethod);

        if(scopeType.isPresent()) {
            Optional<Method> matching = scopeType.get().getMethods().parallelStream().filter(m->
                    m.getSource().getName().equals(methodCall.getName()) && m.getSource().getParameters().size() == methodCall.getArguments().size()
            ).findFirst();

            return matching;
        }

        return Optional.empty();
    }

    private static final Map<Pair<Distinct<Expression>, Method>, Optional<Type>> expressionTypes = new HashMap<>();

    private static Optional<Type> determineTypeOf_CACHED(Expression scope, Method containingMethod) {
        synchronized (expressionTypes) {
            if (!expressionTypes.containsKey(Pair.of(Distinct.of(scope), containingMethod))) {
                Optional<Type> type = determineTypeOf(scope, containingMethod);
                expressionTypes.put(Pair.of(Distinct.of(scope), containingMethod), type);
            }

            return expressionTypes.get(Pair.of(Distinct.of(scope), containingMethod));
        }
    }

    private static Optional<Type> determineTypeOf(Expression scope, Method containingMethod) {
//        System.out.println("determineTypeOf");
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

    private static Map<Pair<Distinct<SimpleName>, Distinct<Node>>, Optional<com.github.javaparser.ast.type.Type>> variableTypes = new HashMap<>();

    private static Optional<com.github.javaparser.ast.type.Type> findTypeOfVariableDeclaration(SimpleName variableName, Node scope) {

        if(!variableTypes.containsKey(Pair.of(Distinct.of(variableName), Distinct.of(scope)))) {

//            System.out.println("findTypeOfVariableDeclaration");
            List<Node> scopeParents = getAllParentsUpToClassDefinition(scope);

            Node topMostNode = scopeParents.get(0);

            Set<Node> scopeParentsSet = new HashSet<Node>(scopeParents);

            com.github.javaparser.ast.type.Type typeCandidate = null;
            int longestChainLength = 0;

            List<VariableDeclarator> variableDeclarations = topMostNode.getNodesByType(VariableDeclarator.class);
            for (VariableDeclarator variableDeclarator : variableDeclarations) {
                if (variableDeclarator.getName().equals(variableName)) {
                    List<Node> variableParents = getAllParentsUpToClassDefinition(variableDeclarator);
                    if (scopeParentsSet.containsAll(variableParents)) {
                        //This variable is defined in the scope we care about
                        if (variableParents.size() > longestChainLength) {
                            typeCandidate = variableDeclarator.getType();
                            longestChainLength = variableParents.size();
                        }
                    }
                }
            }

            List<VariableDeclarationExpr> variableDeclarationExprs = topMostNode.getNodesByType(VariableDeclarationExpr.class);
            for (VariableDeclarationExpr variableDeclarationExpr : variableDeclarationExprs) {
                for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                    if (variableDeclarator.getName().equals(variableName)) {
                        List<Node> variableParents = getAllParentsUpToClassDefinition(variableDeclarator);
                        if (scopeParentsSet.containsAll(variableParents)) {
                            //This variable is defined in the scope we care about
                            if (variableParents.size() > longestChainLength) {
                                typeCandidate = variableDeclarator.getType();
                                longestChainLength = variableParents.size();
                            }
                        }
                    }
                }
            }

            List<Parameter> parameters = topMostNode.getNodesByType(Parameter.class);
            for (Parameter parameter : parameters) {
                if (parameter.getName().equals(variableName)) {
                    List<Node> parameterParents = getAllParentsUpToClassDefinition(parameter);
                    if (scopeParentsSet.containsAll(parameterParents)) {
                        //This variable is defined in the scope we care about
                        if (parameterParents.size() > longestChainLength) {
                            typeCandidate = parameter.getType();
                            longestChainLength = parameterParents.size();
                        }
                    }
                }
            }

            Optional<com.github.javaparser.ast.type.Type> answer = Optional.ofNullable(typeCandidate);
            variableTypes.put(Pair.of(Distinct.of(variableName), Distinct.of(scope)), answer);
        } else {
//            System.out.println("!!!!cache hit in findTypeOfVariableDeclaration");
        }

        return variableTypes.get(Pair.of(Distinct.of(variableName), Distinct.of(scope)));
    }

    private static Map<Node, List<Node>> GETALLPARENTSCACHE=new HashMap<Node, List<Node>>();

    private static List<Node> getAllParentsUpToClassDefinition(Node scope) {
//        System.out.println("getAllParentsUpToClassDefinition");

        if(GETALLPARENTSCACHE.containsKey(scope)) {
            //System.out.println("!!!!cache hit in getAllParentsUpToClassDefinition");
            return GETALLPARENTSCACHE.get(scope);
        }
        else {

            List<Node> parents = new ArrayList<>();
            while (scope.getParentNode().isPresent()) {
                Node parent = scope.getParentNode().get();
                parents.add(parent);
                scope = parent;
            }
            List<Node> reversedParents = Lists.reverse(parents);
            GETALLPARENTSCACHE.put(scope, reversedParents);
            return reversedParents;
        }
    }


    private static final Map<Pair<Distinct<SimpleName>, Type>, Optional<Type>> closestTypeByNameCache = new HashMap<>();

    private static Optional<Type> getClosestTypeWithName(SimpleName identifier, Type source) {
        synchronized (closestTypeByNameCache) {
            if(!closestTypeByNameCache.containsKey(Pair.of(Distinct.of(identifier), source))) {
                Optional<Type> theType = getClosestTypeWithNameUncached(identifier, source);
                closestTypeByNameCache.put(Pair.of(Distinct.of(identifier), source), theType);
            }

            return closestTypeByNameCache.get(Pair.of(Distinct.of(identifier), source));
        }
        
    }

    private static Optional<Type> getClosestTypeWithNameUncached(SimpleName identifier, Type source) {
        Map<SimpleName, Collection<Type>> allClassesByName = getAllClassesForProject(source.getParentPackage().getParentProject());

        if(!allClassesByName.containsKey(identifier)) return Optional.empty(); //The referenced class is not in this project, might be in a dependency, or something from Java itself (Serializable, Comparable, etc)

        Collection<Type> matchingTypes = allClassesByName.get(identifier);
        List<Type> matchingTypesSortedByNumberOfCharactersInCommonWithSourcePackage = matchingTypes.parallelStream().sorted(new Comparator<Type>() {
            @Override
            public int compare(Type t1, Type t2) {
                int firstCharsInCommon = StringUtils.getCommonPrefix(source.getParentPackage().getName(), t1.getParentPackage().getName()).length();
                int secondCharsInCommon = StringUtils.getCommonPrefix(source.getParentPackage().getName(), t2.getParentPackage().getName()).length();

                if(firstCharsInCommon != secondCharsInCommon) {
                    return ((Integer) firstCharsInCommon).compareTo(secondCharsInCommon);
                } else {
                    int firstLength = t1.getParentPackage().getName().length();
                    int secondLength = t2.getParentPackage().getName().length();

                    if (firstLength != secondLength) {
                        return ((Integer) firstLength).compareTo(secondLength);
                    } else {
                        return t1.getParentPackage().getName().compareTo(t2.getParentPackage().getName());
                    }

                }
            }
        }).collect(Collectors.toList());

        if(matchingTypesSortedByNumberOfCharactersInCommonWithSourcePackage.size() > 0) {
            return Optional.of(matchingTypesSortedByNumberOfCharactersInCommonWithSourcePackage.get(0));
        } else {
            return Optional.empty();
        }
    }


    private static final Map<Project, Map<SimpleName, Collection<Type>>> allClassesByNameCache = new HashMap<>();

    private static Map<SimpleName, Collection<Type>> getAllClassesForProject(Project project) {
        if(allClassesByNameCache.containsKey(project)) {
            return allClassesByNameCache.get(project);
        } else {

            Multimap<SimpleName, Type> allClassesByName = HashMultimap.create();

            project.getPackages()
                    .stream()
                    .map(Package::getTypes)
                    .flatMap(Set::stream)
                    .forEach(type -> allClassesByName.put(type.getSource().getName(), type));


            allClassesByNameCache.put(project, allClassesByName.asMap());

            return allClassesByName.asMap();
        }
    }


    private static final Map<Pair<ClassOrInterfaceType, Type>, Optional<Type>> closestTypeCache = new HashMap<>();

    private static Optional<Type> getClosestType(ClassOrInterfaceType target, Type source) {
        synchronized (closestTypeCache) {
            if(!closestTypeCache.containsKey(Pair.of(target, source))) {
                Optional<Type> theType = getClosestTypeUncached(target, source);
                closestTypeCache.put(Pair.of(target, source), theType);
            }

            return closestTypeCache.get(Pair.of(target, source));
        }

    }

    private static Optional<Type> getClosestTypeUncached(ClassOrInterfaceType target, Type source) {
        Map<SimpleName, Collection<Type>> allClassesByName = getAllClassesForProject(source.getParentPackage().getParentProject());


        Collection<Type> matchingTypes = allClassesByName.get(target.getName());
        List<Type> matchingTypesSortedByNumberOfCharactersInCommonWithSourcePackage = matchingTypes.parallelStream().sorted(new Comparator<Type>() {
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
