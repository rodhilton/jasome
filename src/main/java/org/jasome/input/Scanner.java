package org.jasome.input;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public abstract class Scanner<T> {
    
    protected Project doScan(Collection<Pair<String, Map<String, String>>> sourceCode) {
        Project project = new Project();

        Map<String, List<Pair<ClassOrInterfaceDeclaration, Map<String, String>>>> packages = gatherPackages(sourceCode);

        for (Map.Entry<String, List<Pair<ClassOrInterfaceDeclaration, Map<String, String>>>> entry : packages.entrySet()) {

            Package aPackage = new Package(entry.getKey());
            project.addPackage(aPackage);

            for (Pair<ClassOrInterfaceDeclaration, Map<String, String>> classAndAttributes : entry.getValue()) {
                ClassOrInterfaceDeclaration classDefinition = classAndAttributes.getLeft();
                Map<String, String> attributes = classAndAttributes.getRight();

                Type type = new Type(classDefinition);
                aPackage.addType(type);

                for(Map.Entry<String, String> attribute: attributes.entrySet()) {
                    type.addAttribute(attribute);
                }

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
        
        return project;

    }


    private Map<String, List<Pair<ClassOrInterfaceDeclaration, Map<String, String>>>> gatherPackages(Collection<Pair<String, Map<String, String>>> sourcesAndAttributes) {

        Map<String, List<Pair<ClassOrInterfaceDeclaration, Map<String, String>>>> packages = new HashMap<>();

        for (Pair<String, Map<String, String>> sourceFile : sourcesAndAttributes) {
            String sourceCode = sourceFile.getLeft();
            Map<String, String> attributes = sourceFile.getRight();
            
            CompilationUnit cu = JavaParser.parse(sourceCode);

            String packageName = cu.getPackageDeclaration().map((p) -> p.getName().asString()).orElse("default");

            List<ClassOrInterfaceDeclaration> classes = cu.getNodesByType(ClassOrInterfaceDeclaration.class);

            if (!packages.containsKey(packageName)) {
                packages.put(packageName, new ArrayList<>());
            }

            for (ClassOrInterfaceDeclaration clazz : classes) {
                packages.get(packageName).add(Pair.of(clazz, attributes));
            }
        }

        return packages;
    }

}

