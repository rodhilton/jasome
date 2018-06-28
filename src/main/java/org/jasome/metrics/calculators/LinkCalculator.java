package org.jasome.metrics.calculators;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.jasome.input.Method;
import org.jasome.input.Package;
import org.jasome.input.Project;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LinkCalculator implements Calculator<Type> {

    @Override
    public Set<Metric> calculate(Type type) {

        Graph<Type> uses = linkNetwork.getUnchecked(type.getParentPackage().getParentProject());

        Set<Type> links = uses.successors(type);

        return ImmutableSet.of(
                Metric.of("NOL", "Number of Links", links.size())
        );
    }

    private static LoadingCache<Project, Graph<Type>> linkNetwork = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Project, Graph<Type>>() {
                @Override
                public Graph<Type> load(Project parentProject) throws Exception {
                    MutableGraph<Type> graph = GraphBuilder.directed().allowsSelfLoops(false).build();

                    Set<Type> allTypes = parentProject.getPackages()
                            .stream()
                            .map(Package::getTypes)
                            .flatMap(Set::stream).collect(Collectors.toSet());

                    for (Type type : allTypes) {
                        graph.addNode(type);

                        //Direct class references
                        List<com.github.javaparser.ast.type.Type> classes = type.getSource().findAll(com.github.javaparser.ast.type.Type.class);

                        for (com.github.javaparser.ast.type.Type clazz : classes) {
                            try {
                                ResolvedReferenceTypeDeclaration typeDeclaration = clazz.resolve().asReferenceType().getTypeDeclaration();

                                Optional<Type> typeOpt = lookupType(parentProject, typeDeclaration);
                                typeOpt.ifPresent(typ -> graph.putEdge(type, typ));
                            } catch (Exception e) {
                                //Ignore
                            }
                        }

                        List<MethodCallExpr> methodCalls = type.getSource().findAll(MethodCallExpr.class);

                        for (MethodCallExpr methodCall : methodCalls) {
                            try {
                                ResolvedMethodDeclaration resolvedMethodDeclaration = methodCall.resolve();
                                ResolvedReferenceTypeDeclaration declaringType = resolvedMethodDeclaration.declaringType();

                                Optional<Type> typeOpt = lookupType(parentProject, declaringType);
                                typeOpt.ifPresent(typ -> graph.putEdge(type, typ));
                            } catch (Exception e) {
                                //Ignore
                            }
                        }

                    }


                    return ImmutableGraph.copyOf(graph);
                }
            });

    private static Optional<Type> lookupType(Project project, ResolvedReferenceTypeDeclaration typeDeclaration) {
        try {
            String packageName = typeDeclaration.getPackageName();
            String typeName = typeDeclaration.getName();

            return project.lookupPackageByName(packageName).flatMap(aPackage -> aPackage.lookupTypeByName(typeName));
        } catch (Exception e) {
            return Optional.empty();
        }

    }

    private static Optional<Method> getMethodCalledByMethodExpression(Method containingMethod, MethodCallExpr methodCall) {
        try {
            ResolvedMethodDeclaration blah = methodCall.resolve();
            ResolvedReferenceTypeDeclaration declaringType = blah.declaringType();

            Project project = containingMethod.getParentType().getParentPackage().getParentProject();
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
