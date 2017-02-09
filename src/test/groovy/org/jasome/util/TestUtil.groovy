package org.jasome.util

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.apache.commons.io.IOUtils
import org.jasome.parsing.Method
import org.jasome.parsing.Package
import org.jasome.parsing.Project
import org.jasome.parsing.Type

class TestUtil {

    public static Type typeFromSnippet(String sourceCode) {
        CompilationUnit cu = JavaParser.parse(sourceCode);

        String packageName = cu.getPackageDeclaration().map{decl -> decl.getName().asString()}.orElseGet{"default"}

        org.jasome.parsing.Package aPackage = new org.jasome.parsing.Package(packageName);

        List<ClassOrInterfaceDeclaration> nodes = cu.getNodesByType(ClassOrInterfaceDeclaration.class)

        assert(nodes.size() > 0)

        Type t = new Type((ClassOrInterfaceDeclaration)nodes.get(0))

        aPackage.addType(t)

        return t;
    }

    public static Method methodFromSnippet(String sourceCode) {
        BodyDeclaration<MethodDeclaration> methodDeclaration = (BodyDeclaration<MethodDeclaration>)JavaParser.parseClassBodyDeclaration(sourceCode)

        Method m = new Method((MethodDeclaration)methodDeclaration);

        org.jasome.parsing.Package p = new org.jasome.parsing.Package("org.test.example")

        ClassOrInterfaceDeclaration myClass = new ClassOrInterfaceDeclaration(
            EnumSet.of(Modifier.PUBLIC), false, "MyClass");

        myClass.addMember((MethodDeclaration)methodDeclaration);

        Type t = new Type(myClass)
        t.addMethod(m)
        p.addType(t)

        return m;
    }

    public static org.jasome.parsing.Package packageFromSnippet(String sourceCode) {
        CompilationUnit cu = JavaParser.parse(sourceCode.trim());

        String packageName = cu.getPackageDeclaration().map{decl -> decl.getName().asString()}.orElseGet{"default"}

        org.jasome.parsing.Package aPackage = new org.jasome.parsing.Package(packageName);

        List<ClassOrInterfaceDeclaration> nodes = cu.getNodesByType(ClassOrInterfaceDeclaration.class)

        for(ClassOrInterfaceDeclaration classOrInterfaceDeclaration: nodes) {
            Type t = new Type(classOrInterfaceDeclaration)
            aPackage.addType(t);
        }

        return aPackage;
    }

    //TODO: lots of repeated code here from the parser, can probably refactor to a common place
    public static Project projectFromSnippet(String... sourceCodes) {
        Multimap<String, ClassOrInterfaceDeclaration> packagesToClasses = HashMultimap.create()

        for(String sourceCode: sourceCodes) {
            CompilationUnit cu = JavaParser.parse(sourceCode);
            String packageName = cu.getPackageDeclaration().map{decl -> decl.getName().asString()}.orElseGet{"default"}
            List<ClassOrInterfaceDeclaration> nodes = cu.getNodesByType(ClassOrInterfaceDeclaration.class)
            packagesToClasses.putAll(packageName, nodes)
        }

        Project project = new Project()

        for(String packageName: packagesToClasses.keySet()) {

            Package aPackage = new Package(packageName);
            project.addPackage(aPackage);

            for (ClassOrInterfaceDeclaration classDefinition : packagesToClasses.get(packageName)) {

                Type type = new Type(classDefinition);
                aPackage.addType(type);

                for (MethodDeclaration methodDeclaration : classDefinition.getMethods()) {
                    Method method = new Method(methodDeclaration);
                    type.addMethod(method);
                }

            }
        }

        return project;
    }

    public static Type typeFromStream(InputStream stream) {
        String snippet = IOUtils.toString(stream, "UTF-8");
        return typeFromSnippet(snippet);
    }
}
