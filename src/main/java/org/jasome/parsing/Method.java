package org.jasome.parsing;

import com.github.javaparser.ast.body.MethodDeclaration;

public class Method extends TreeNode {
    private final MethodDeclaration declaration;

    public Method(MethodDeclaration declaration) {
        super(declaration.getDeclarationAsString());
        this.declaration = declaration;
    }

    public MethodDeclaration getSource() {
        return declaration;
    }

    public Type getParentClass() {
        return (Type)getParent();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.METHOD;
    }
}
