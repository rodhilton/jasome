package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.calculators.*;
import org.jasome.output.Output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Scanner {
    private Set<PackageMetricCalculator> packageCalculators;
    private Set<ClassMetricCalculator> classCalculators;
    private Set<MethodMetricCalculator> methodCalculators;

    public Scanner() {
        packageCalculators = new HashSet<PackageMetricCalculator>();
        classCalculators = new HashSet<ClassMetricCalculator>();
        methodCalculators = new HashSet<MethodMetricCalculator>();
    }

    public void registerPackageCalculator(PackageMetricCalculator calculator) {
        packageCalculators.add(calculator);
    }

    public void registerClassCalculator(ClassMetricCalculator calculator) {
        classCalculators.add(calculator);
    }

    public void registerMethodCalculator(MethodMetricCalculator calculator) {
        methodCalculators.add(calculator);
    }

    public Output scan(Collection<File> inputFiles) throws IOException {
        Map<TreeNode, Node> sourceCode = new HashMap<TreeNode, Node>();

        Multimap<TreeNode, Pair<String, String>> attributes = HashMultimap.create();

        ProjectNode projectNode = new ProjectNode();

        Map<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>> packages = gatherPackages(inputFiles);

        for (Map.Entry<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>> entry : packages.entrySet()) {

            PackageNode packageNode = new PackageNode(entry.getKey());
            projectNode.addPackage(packageNode);

            for (Pair<ClassOrInterfaceDeclaration, SourceContext> classAndContext : entry.getValue()) {
                ClassOrInterfaceDeclaration classDefinition = classAndContext.getLeft();

                ClassNode classNode = new ClassNode(getClassNameFromDeclaration(classDefinition));
                packageNode.addClass(classNode);

                sourceCode.put(classNode, classDefinition);
                attributes.put(classNode, Pair.of("sourceFile", classAndContext.getValue().getSourceFile().getPath()));

                for (MethodDeclaration methodDeclaration : classDefinition.getMethods()) {
                    MethodNode methodNode = new MethodNode(methodDeclaration.getDeclarationAsString());
                    classNode.addMethod(methodNode);

                    sourceCode.put(methodNode, methodDeclaration);

                }

            }
        }

        Multimap<TreeNode, Metric> metrics = HashMultimap.create();

        for (PackageNode packageNode : projectNode.getPackages()) {

            List<ClassOrInterfaceDeclaration> classCodeInPackage = new ArrayList<ClassOrInterfaceDeclaration>();

            for (ClassNode classNode : packageNode.getClasses()) {
                ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) sourceCode.get(classNode);
                classCodeInPackage.add(classOrInterfaceDeclaration);

                for (ClassMetricCalculator classMetricCalculator : classCalculators) {
                    Set<Metric> classMetrics = classMetricCalculator.calculate(classOrInterfaceDeclaration, null);
                    metrics.putAll(classNode, classMetrics);
                }

                for (MethodNode methodNode : classNode.getMethods()) {
                    for (MethodMetricCalculator methodMetricCalculator : methodCalculators) {
                        Set<Metric> methodMetrics = methodMetricCalculator.calculate((MethodDeclaration) sourceCode.get(methodNode), null);
                        metrics.putAll(methodNode, methodMetrics);
                    }
                }
            }

            //We collected all the classes in the package above, so now we can get the package metrics
            for (PackageMetricCalculator packageMetricCalculator : packageCalculators) {
                Set<Metric> packageMetrics = packageMetricCalculator.calculate(classCodeInPackage, null);
                metrics.putAll(packageNode, packageMetrics);
            }
        }


        for (PackageNode packageNode : projectNode.getPackages()) {
            System.out.println(packageNode.getName());
            System.out.println("+" + metrics.get(packageNode));

            for (ClassNode classNode : packageNode.getClasses()) {

                System.out.println("  " + classNode.getName());
                System.out.println("  +" + metrics.get(classNode));

                for (MethodNode methodNode : classNode.getMethods()) {

                    System.out.println("    " + methodNode.getName());
                    System.out.println("    +" + metrics.get(methodNode));

                }
            }

        }

        return new Output(projectNode, metrics, attributes);

    }

    private String getClassNameFromDeclaration(ClassOrInterfaceDeclaration classDefinition) {
        String className = classDefinition.getNameAsString();

        if (classDefinition.getParentNode().isPresent()) {
            Node parentNode = classDefinition.getParentNode().get();
            if (parentNode instanceof ClassOrInterfaceDeclaration) {
                className = ((ClassOrInterfaceDeclaration) parentNode).getNameAsString() + "." +
                        classDefinition.getNameAsString();
            }
        }
        return className;
    }

    private Map<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>> gatherPackages(Collection<File> sourceFiles) throws FileNotFoundException {

        Map<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>> packages = new HashMap<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>>();

        for (File sourceFile : sourceFiles) {
            FileInputStream in = new FileInputStream(sourceFile);

            CompilationUnit cu = JavaParser.parse(in);

            String packageName = cu.getPackageDeclaration().map((p) -> p.getName().asString()).orElse("default");
            List<ImportDeclaration> imports = cu.getImports();

            SourceContext sourceContext = new SourceContext();
            sourceContext.setPackageName(packageName);
            sourceContext.setImports(imports);
            sourceContext.setSourceFile(sourceFile);

            List<ClassOrInterfaceDeclaration> classes = cu.getNodesByType(ClassOrInterfaceDeclaration.class);

            if (!packages.containsKey(packageName)) {
                packages.put(packageName, new ArrayList<Pair<ClassOrInterfaceDeclaration, SourceContext>>());
            }

            for (ClassOrInterfaceDeclaration clazz : classes) {
                packages.get(packageName).add(Pair.of(clazz, sourceContext));
            }
        }

        return packages;
    }

}

