package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
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
        Map<TreeNode, Node> sourceCode = new HashMap<>();
        Multimap<TreeNode, Pair<String, String>> attributes = HashMultimap.create();
        Multimap<TreeNode, Metric> metrics = HashMultimap.create();

        Project project = new Project();

        Map<String, List<Pair<ClassOrInterfaceDeclaration, File>>> packages = gatherPackages(inputFiles);

        for (Map.Entry<String, List<Pair<ClassOrInterfaceDeclaration, File>>> entry : packages.entrySet()) {

            ProjectPackage projectPackage = new ProjectPackage(entry.getKey());
            project.addPackage(projectPackage);

            for (Pair<ClassOrInterfaceDeclaration, File> classAndSourceFile : entry.getValue()) {
                ClassOrInterfaceDeclaration classDefinition = classAndSourceFile.getLeft();

                ProjectClass projectClass = new ProjectClass(getClassNameFromDeclaration(classDefinition));
                projectPackage.addClass(projectClass);

                sourceCode.put(projectClass, classDefinition);
                attributes.put(projectClass, Pair.of("sourceFile", classAndSourceFile.getValue().getPath()));

                for (MethodDeclaration methodDeclaration : classDefinition.getMethods()) {
                    ProjectMethod projectMethod = new ProjectMethod(methodDeclaration.getDeclarationAsString());
                    projectClass.addMethod(projectMethod);

                    sourceCode.put(projectMethod, methodDeclaration);

                }

            }
        }

        for (ProjectPackage projectPackage : project.getPackages()) {

            List<ClassOrInterfaceDeclaration> classCodeInPackage = new ArrayList<ClassOrInterfaceDeclaration>();

            for (ProjectClass projectClass : projectPackage.getClasses()) {
                ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) sourceCode.get(projectClass);
                classCodeInPackage.add(classOrInterfaceDeclaration);

                for (ClassMetricCalculator classMetricCalculator : classCalculators) {
                    Set<Metric> classMetrics = classMetricCalculator.calculate(classOrInterfaceDeclaration, null);
                    metrics.putAll(projectClass, classMetrics);
                }

                for (ProjectMethod projectMethod : projectClass.getMethods()) {
                    for (MethodMetricCalculator methodMetricCalculator : methodCalculators) {
                        Set<Metric> methodMetrics = methodMetricCalculator.calculate((MethodDeclaration) sourceCode.get(projectMethod), null);
                        metrics.putAll(projectMethod, methodMetrics);
                    }
                }
            }

            //We collected all the classes in the package above, so now we can get the package metrics
            for (PackageMetricCalculator packageMetricCalculator : packageCalculators) {
                Set<Metric> packageMetrics = packageMetricCalculator.calculate(classCodeInPackage, null);
                metrics.putAll(projectPackage, packageMetrics);
            }
        }


        for (ProjectPackage projectPackage : project.getPackages()) {
            System.out.println(projectPackage.getName());
            System.out.println("+" + metrics.get(projectPackage));

            for (ProjectClass projectClass : projectPackage.getClasses()) {

                System.out.println("  " + projectClass.getName());
                System.out.println("  +" + metrics.get(projectClass));

                for (ProjectMethod projectMethod : projectClass.getMethods()) {

                    System.out.println("    " + projectMethod.getName());
                    System.out.println("    +" + metrics.get(projectMethod));

                }
            }

        }

        return new Output(project, metrics, attributes);

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

    private Map<String, List<Pair<ClassOrInterfaceDeclaration, File>>> gatherPackages(Collection<File> sourceFiles) throws FileNotFoundException {

        Map<String, List<Pair<ClassOrInterfaceDeclaration, File>>> packages = new HashMap<String, List<Pair<ClassOrInterfaceDeclaration, File>>>();

        for (File sourceFile : sourceFiles) {
            FileInputStream in = new FileInputStream(sourceFile);

            CompilationUnit cu = JavaParser.parse(in);

            String packageName = cu.getPackageDeclaration().map((p) -> p.getName().asString()).orElse("default");
            //List<ImportDeclaration> imports = cu.getImports();

            List<ClassOrInterfaceDeclaration> classes = cu.getNodesByType(ClassOrInterfaceDeclaration.class);

            if (!packages.containsKey(packageName)) {
                packages.put(packageName, new ArrayList<Pair<ClassOrInterfaceDeclaration, File>>());
            }

            for (ClassOrInterfaceDeclaration clazz : classes) {
                packages.get(packageName).add(Pair.of(clazz, sourceFile));
            }
        }

        return packages;
    }

}

