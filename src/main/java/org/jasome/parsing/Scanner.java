package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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
    private Set<Calculator<Project>> projectCalculators;
    private Set<Calculator<Package>> packageCalculators;
    private Set<Calculator<Type>> typeCalculators;
    private Set<Calculator<Method>> methodCalculators;

    public Scanner() {
        projectCalculators = new HashSet<>();
        packageCalculators = new HashSet<>();
        typeCalculators = new HashSet<>();
        methodCalculators = new HashSet<>();
    }

    public void registerProjectCalculator(Calculator<Project> calculator) {
        projectCalculators.add(calculator);
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

                //We need to convert the constructor declarations to method declarations because we treat them the same, but javaparser don't have them sharing a useful common type
                for (ConstructorDeclaration constructorDeclaration : classDefinition.getNodesByType(ConstructorDeclaration.class)) {
                    MethodDeclaration constructorMethodDeclaration = new MethodDeclaration(
                            constructorDeclaration.getModifiers(),
                            constructorDeclaration.getAnnotations(),
                            constructorDeclaration.getTypeParameters(),
                            new ClassOrInterfaceType(classDefinition.getName().getIdentifier()),
                            constructorDeclaration.getName(),
                            false,
                            constructorDeclaration.getParameters(),
                            constructorDeclaration.getThrownExceptions(),
                            constructorDeclaration.getBody()
                    );
                    Method constructor = new Method(constructorMethodDeclaration);
                    type.addMethod(constructor);

                    constructor.addAttribute("lineStart", ""+constructorDeclaration.getBegin().get().line);
                    constructor.addAttribute("lineEnd", ""+constructorDeclaration.getEnd().get().line);
                    constructor.addAttribute("constructor", "true");
                }

                for (MethodDeclaration methodDeclaration : classDefinition.getMethods()) {
                    Method method = new Method(methodDeclaration);
                    type.addMethod(method);

                    method.addAttribute("lineStart", ""+methodDeclaration.getBegin().get().line);
                    method.addAttribute("lineEnd", ""+methodDeclaration.getEnd().get().line);
                    method.addAttribute("constructor", "false");

                }

            }
        }

        
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

        for (Calculator<Project> projectMetricCalculator : projectCalculators) {
            Set<Metric> projectMetrics = projectMetricCalculator.calculate(project);
            project.addMetrics(projectMetrics);
        }


        for (Package aPackage : project.getPackages()) {
            System.out.println(aPackage.getName());
            System.out.println("+" + aPackage.getMetrics());

            for (Type type : aPackage.getTypes()) {

                System.out.println("  " + type.getName());
                System.out.println("  +" + type.getMetrics());

                for (Method method : type.getMethods()) {

                    System.out.println("    " + method.getName());
                    System.out.println("    +" + aPackage.getMetrics());

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

