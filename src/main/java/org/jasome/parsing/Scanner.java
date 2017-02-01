package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.calculators.*;
import org.jasome.output.Output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

        Map<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>> packages = gatherPackages(inputFiles);

        Output output = new Output();

        for(Map.Entry<String, List<Pair<ClassOrInterfaceDeclaration, SourceContext>>> entry: packages.entrySet()) {
            String packageName = entry.getKey();


            { //First, get the package metrics
                SourceContext packageContext = new SourceContext();
                packageContext.setPackageName(packageName);

                for (PackageMetricCalculator packageMetricCalculator : packageCalculators) {
                    List<ClassOrInterfaceDeclaration> classes = entry.getValue().stream().map(Pair::getKey).collect(Collectors.toList());
                    Metrics metrics = packageMetricCalculator.calculate(classes, packageContext);
                    output.addCalculations(metrics, packageName);
                }

            }


            //Now the class metrics
            for(Pair<ClassOrInterfaceDeclaration, SourceContext> classAndContext: entry.getValue()) {
                ClassOrInterfaceDeclaration classDefinition = classAndContext.getLeft();

                String className = classDefinition.getNameAsString();

                if(classDefinition.getParentNode().isPresent()) {
                    Node parentNode  = classDefinition.getParentNode().get();
                    if(parentNode instanceof ClassOrInterfaceDeclaration) {
                        className = ((ClassOrInterfaceDeclaration)parentNode).getNameAsString() + "." +
                                classDefinition.getNameAsString();
                    }
                }

                SourceContext classContext = classAndContext.getRight();

                {
                    for (ClassMetricCalculator classMetricCalculator : classCalculators) {
                        Metrics metrics = classMetricCalculator.calculate(classDefinition, classContext);
                        Map<String, String> attributes = Maps.newHashMap();
                        attributes.put("sourceFile", classContext.getSourceFile().getName());
                        attributes.put("sourceDir", classContext.getSourceFile().getParent());
                        output.addCalculations(metrics, attributes, packageName, className);
                    }

                }

                //And finally the method metrics
                for(MethodDeclaration methodDeclaration: classDefinition.getMethods()) {
                    SourceContext methodContext = new SourceContext();
                    methodContext.setPackageName(classContext.getPackageName());
                    methodContext.setImports(classContext.getImports());
                    methodContext.setClassDefinition(classDefinition);

                    for (MethodMetricCalculator methodMetricCalculator : methodCalculators) {
                        Metrics metrics = methodMetricCalculator.calculate(methodDeclaration, methodContext);
                        output.addCalculations(metrics, packageName, className, methodDeclaration.getDeclarationAsString());
                    }

                }

            }
        }

        return output;

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

