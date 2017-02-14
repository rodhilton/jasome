package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.calculators.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Scanner {
    private Set<Calculator<Package>> packageCalculators;
    private Set<Calculator<Type>> typeCalculators;
    private Set<Calculator<Method>> methodCalculators;

    public Scanner() {
        packageCalculators = new HashSet<>();
        typeCalculators = new HashSet<>();
        methodCalculators = new HashSet<>();
    }

    public void registerPackageCalculator(Calculator<Package> calculator) {
        packageCalculators.add(calculator);
    }

    public void registerTypeCalculator(Calculator<Type> calculator) {
        typeCalculators.add(calculator);
    }

    public void registerMethodCalculator(Calculator<Method> calculator) {
        methodCalculators.add(calculator);
    }

    public Project scan(Collection<File> inputFiles) throws IOException {
        Multimap<Code, Pair<String, String>> attributes = HashMultimap.create();
        Multimap<Code, Metric> metrics = HashMultimap.create();

        Project project = new Project();

        Map<String, List<Pair<ClassOrInterfaceDeclaration, File>>> packages = gatherPackages(inputFiles);

        for (Map.Entry<String, List<Pair<ClassOrInterfaceDeclaration, File>>> entry : packages.entrySet()) {

            Package aPackage = new Package(entry.getKey());
            project.addPackage(aPackage);

            for (Pair<ClassOrInterfaceDeclaration, File> classAndSourceFile : entry.getValue()) {
                ClassOrInterfaceDeclaration classDefinition = classAndSourceFile.getLeft();

                Type type = new Type(classDefinition);
                aPackage.addType(type);

                type.addAttribute("sourceFile", classAndSourceFile.getValue().getPath());

                for (MethodDeclaration methodDeclaration : classDefinition.getMethods()) {
                    Method method = new Method(methodDeclaration);
                    type.addMethod(method);

                    attributes.put(method, Pair.of("lineStart", ""+methodDeclaration.getBegin().get().line));
                    attributes.put(method, Pair.of("lineEnd", ""+methodDeclaration.getEnd().get().line));
                }

            }
        }


        //TODO gather project metrics, go "recursively" so that class-level metrics can reference method metrics and so on
        for (Package aPackage : project.getPackages()) {

            for (Type type : aPackage.getTypes()) {

                for (Method method : type.getMethods()) {

                    for (Calculator<Method> methodMetricCalculator : methodCalculators) {
                        Set<Metric> methodMetrics = methodMetricCalculator.calculate(method);
                        method.addMetrics(methodMetrics);
                    }
                }

                for (Calculator<Type> typeMetricCalculator : typeCalculators) {
                    Set<Metric> classMetrics = typeMetricCalculator.calculate(type);
                    type.addMetrics(classMetrics);
                }
            }

            for (Calculator<Package> packageMetricCalculator : packageCalculators) {
                Set<Metric> packageMetrics = packageMetricCalculator.calculate(aPackage);
                aPackage.addMetrics(packageMetrics);
            }

        }


        for (Package aPackage : project.getPackages()) {
            System.out.println(aPackage.getName());
            System.out.println("+" + metrics.get(aPackage));

            for (Type type : aPackage.getTypes()) {

                System.out.println("  " + type.getName());
                System.out.println("  +" + metrics.get(type));

                for (Method method : type.getMethods()) {

                    System.out.println("    " + method.getName());
                    System.out.println("    +" + metrics.get(method));

                }
            }

        }

        return project;

    }


    private Map<String, List<Pair<ClassOrInterfaceDeclaration, File>>> gatherPackages(Collection<File> sourceFiles) throws FileNotFoundException {

        Map<String, List<Pair<ClassOrInterfaceDeclaration, File>>> packages = new HashMap<String, List<Pair<ClassOrInterfaceDeclaration, File>>>();

        for (File sourceFile : sourceFiles) {
            FileInputStream in = new FileInputStream(sourceFile);

            CompilationUnit cu = JavaParser.parse(in);

            String packageName = cu.getPackageDeclaration().map((p) -> p.getName().asString()).orElse("default");

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

