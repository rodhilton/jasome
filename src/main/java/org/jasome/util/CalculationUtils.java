package org.jasome.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
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

                    for (Type type :allClasses) {
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
                            } catch(UnsolvedSymbolException e) {
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
        } catch(Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static LoadingCache<Project, Network<Method, Distinct<Expression>>> callNetwork = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Project, Network<Method, Distinct<Expression>>>() {
                @Override
                public Network<Method, Distinct<Expression>> load(Project parentProject) throws Exception {
                    MutableNetwork<Method, Distinct<Expression>> network = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();

                    System.out.println("should only be here once...");

//        Set<Method> allClassesByName = new HashSet<>();

                    Set<Method> allMethods = parentProject.getPackages()
                            .stream()
                            .map(Package::getTypes)
                            .flatMap(Set::stream).map(Type::getMethods).flatMap(Set::stream).collect(Collectors.toSet());

                    for(Method method: allMethods) {
                        network.addNode(method);

                        List<MethodCallExpr> calls = method.getSource().getNodesByType(MethodCallExpr.class);

                        //TODO: not using this yet
                        List<MethodReferenceExpr> references = method.getSource().getNodesByType(MethodReferenceExpr.class);

                        for(MethodCallExpr methodCall: calls) {


                            Optional<Method> methodCalled = getMethodCalledByMethodExpression(method, methodCall);

                            if(methodCalled.isPresent()) {
                                network.addEdge(method, methodCalled.orElse(Method.UNKNOWN), Distinct.of(methodCall));
                            }

                        }
                    }


                    return ImmutableNetwork.copyOf(network);
                }
            });

    public static Network<Method, Distinct<Expression>> getCallNetwork(Project parentProject) {
        MutableNetwork<Method, Distinct<Expression>> network = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();

//        Set<Method> allClassesByName = new HashSet<>();

        Map<String, List<String>> mapOfTypeNamesToPackagesContainingThem = new HashMap<>();

        parentProject.getPackages()
                .stream()
                .map(Package::getTypes).flatMap(Set::stream).forEach(type -> {
                   mapOfTypeNamesToPackagesContainingThem.putIfAbsent(type.getName(), new ArrayList<String>());
                   mapOfTypeNamesToPackagesContainingThem.get(type.getName()).add(type.getParentPackage().getName());
        });

        Set<Method> allMethods = parentProject.getPackages()
                .stream()
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
        //if(methodCall.getScope().isPresent()) {
            //Expression methodCallScope = methodCall.getScope().get();

            //ResolvedType resolvedType = JavaParser.getStaticConfiguration().getSymbolResolver().get().calculateType(methodCall.getScope().get());

            try {
                ResolvedMethodDeclaration blah = methodCall.resolve();
                ResolvedReferenceTypeDeclaration declaringType = blah.declaringType();

                Project project = containingMethod.getParentType().getParentPackage().getParentProject();
                Optional<Package> pkg = project.lookupPackageByName(declaringType.getPackageName());
                if (!pkg.isPresent()) return Optional.empty();

                Optional<Type> typ = pkg.get().lookupTypeByName(declaringType.getName());

                if (!typ.isPresent()) return Optional.empty();

                Optional<Method> method = typ.get().lookupMethodBySignature(blah.getSignature());

                //System.out.println(method);

                return method;
            } catch(Exception e) {
                //e.printStackTrace();
                return Optional.empty();
            }

//        } else {
//            System.out.println("stuff");
//        }
       // return Optional.empty();
    }

    private static Map<Pair<Distinct<SimpleName>, Distinct<Node>>, Optional<com.github.javaparser.ast.type.Type>> variableTypes = new HashMap<>();

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
        List<Type> matchingTypesSortedByNumberOfCharactersInCommonWithSourcePackage = matchingTypes.stream().sorted(new Comparator<Type>() {
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


}
