package org.jasome.input;

import com.github.javaparser.ast.body.MethodDeclaration;

public class Method extends Code {
    private final MethodDeclaration declaration;

    public final static Method UNKNOWN = new Method();

    private Method() {
        super("unknownMethod");
        this.declaration = null;
    }

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
