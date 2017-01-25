package org.jasome;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class SomeClass {
    private ClassOrInterfaceDeclaration classDeclaration;

    public SomeClass(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

        this.classDeclaration = classOrInterfaceDeclaration;
    }

    public ClassOrInterfaceDeclaration getClassDeclaration() {
        return classDeclaration;
    }
}
