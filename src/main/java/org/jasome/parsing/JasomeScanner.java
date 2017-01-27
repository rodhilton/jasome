package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.Calculation;
import org.jasome.Calculator;
import org.jasome.SomeClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class JasomeScanner {
    private Set<Calculator<?>> calculators;

    public JasomeScanner() {
        calculators = new HashSet<>();
    }

    public void register(Calculator<?> calculator) {
        calculators.add(calculator);
    }

    public void scan(Collection<File> sourceFiles) throws IOException {

        Map<String, List<SomeClass>> packages = new HashMap<String, List<SomeClass>>();

        for(File sourceFile: sourceFiles) {
            FileInputStream in = new FileInputStream(sourceFile);

            // parse the file
            CompilationUnit cu = JavaParser.parse(in);

            String packageName = cu.getPackageDeclaration().map((p) -> p.getName().asString()).orElse("default");

            if(!packages.containsKey(packageName)) {
                packages.put(packageName, new ArrayList<SomeClass>());
            }

            List<ClassOrInterfaceDeclaration> classes = cu.getNodesByType(ClassOrInterfaceDeclaration.class);
            for(ClassOrInterfaceDeclaration clazz: classes) {
                packages.get(packageName).add(new SomeClass(clazz));
            }
        }

        for(Calculator calculator: calculators) {
            for(List<SomeClass> someClasses: packages.values()) {
                for(SomeClass someClass: someClasses) {
                    //System.out.println(someClass.getClassDeclaration().getName());
                    Set<Calculation> calcs = calculator.calculate(someClass);
                    System.out.println(someClass);
                    System.out.println("  "+calcs);
                }

            }
        }

//        System.out.println(packages);

    }


}
