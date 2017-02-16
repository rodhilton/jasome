package org.jasome.input;

import com.github.javaparser.ast.body.MethodDeclaration;

public class Method extends Code {
    private final MethodDeclaration declaration;

    public Method(MethodDeclaration declaration) {
        super(declaration.getDeclarationAsString());
        this.declaration = declaration;
    }

    public MethodDeclaration getSource() {
        return declaration;
    }

    public Type getParentType() {
        return (Type) getParent();
    }

    @Override
    public String toString() {
        return "Method(" + this.getName() + ")";
    }
}
