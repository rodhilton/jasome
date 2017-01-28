package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.calculators.*;
import org.jasome.SomeClass;
import org.jasome.SomeMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JasomeScanner {
    private Set<PackageMetricCalculator> packageCalculators;
    private Set<ClassMetricCalculator> classCalculators;
    private Set<MethodMetricCalculator> methodCalculators;

    public JasomeScanner() {
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

    public void scan(Collection<File> sourceFiles) throws IOException {

        Map<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>> packages = gatherPackages(sourceFiles);

        for(Map.Entry<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>> entry: packages.entrySet()) {
            String packageName = entry.getKey();


            { //First, get the package metrics
                SourceContext packageContext = new SourceContext();
                packageContext.setPackageName(packageName);

                HashSet<Calculation> packageMetrics = new HashSet<Calculation>();

                for (PackageMetricCalculator packageMetricCalculator : packageCalculators) {
                    List<ClassOrInterfaceDeclaration> classes = entry.getValue().stream().map(Pair::getKey).collect(Collectors.toList());
                    packageMetrics.addAll(packageMetricCalculator.calculate(classes, packageContext));
                }

                System.out.println("Package " + packageName);
                System.out.println("   " + packageMetrics);
            }


            //Now the class metrics
            for(Pair<ClassOrInterfaceDeclaration, SourceContext> classAndContext: entry.getValue()) {
                ClassOrInterfaceDeclaration classDefinition = classAndContext.getLeft();
                SourceContext classContext = classAndContext.getRight();

                {
                    HashSet<Calculation> classMetrics = new HashSet<Calculation>();

                    for (ClassMetricCalculator classMetricCalculator : classCalculators) {
                        classMetrics.addAll(classMetricCalculator.calculate(classDefinition, classContext));
                    }

                    System.out.println(" Class " + classDefinition.getNameAsString());
                    System.out.println("   " + classMetrics);
                }


                //And finally the method metrics
                for(MethodDeclaration methodDeclaration: classDefinition.getMethods()) {
                    SourceContext methodContext = new SourceContext();
                    methodContext.setPackageName(classContext.getPackageName());
                    methodContext.setImports(classContext.getImports());
                    methodContext.setClassDefinition(Optional.of(classDefinition));

                    HashSet<Calculation> methodMetrics = new HashSet<Calculation>();
                    for (MethodMetricCalculator methodMetricCalculator : methodCalculators) {
                        methodMetrics.addAll(methodMetricCalculator.calculate(methodDeclaration, methodContext));
                    }

                    System.out.println("  Method " + methodDeclaration.getNameAsString());
                    System.out.println("   " + methodMetrics);
                }

            }


        }


        //System.out.println(packages);

        //TODO: handle exceptions here.  a calculator can throw a runtime exception, want it to simply not be tablulated if that happens
//            for(List<SomeClass> someClasses: packages.values()) {
//                for(SomeClass someClass: someClasses) {
//                    for(ClassMetricCalculator calculator: classCalculators) {
//                    //System.out.println(someClass.getClassDeclaration().getName());
//                    Set<Calculation> calcs = calculator.calculate(someClass.getClassDeclaration(), SourceContext.NONE);
//                    System.out.println(someClass);
//                    System.out.println("  "+calcs);
//                }
//
//            }
        //}

//        System.out.println(packages);

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
