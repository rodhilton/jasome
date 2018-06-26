package org.jasome.input;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Type extends Code {
    private final ClassOrInterfaceDeclaration declaration;
    private Map<String, Method> methodLookup;

    public Type(ClassOrInterfaceDeclaration declaration) {
        super(getClassNameFromDeclaration(declaration));
        this.declaration = declaration;
        this.methodLookup = new HashMap<>();
    }

    public ClassOrInterfaceDeclaration getSource() {
        return declaration;
    }

    private static String getClassNameFromDeclaration(ClassOrInterfaceDeclaration classDefinition) {
        String className = classDefinition.getNameAsString();

        if (classDefinition.getParentNode().isPresent()) {
            Node parentNode = classDefinition.getParentNode().get();
            if (parentNode instanceof ClassOrInterfaceDeclaration) {
                className = ((ClassOrInterfaceDeclaration) parentNode).getNameAsString() + "." +
                        classDefinition.getNameAsString();
            }
        }
        return className;
    }

    @SuppressWarnings("unchecked")
    public Set<Method> getMethods() {
        return (Set<Method>)(Set<?>)getChildren();
    }

    public void addMethod(Method method) {
        methodLookup.put(method.getSource().getSignature().asString(), method);
        addChild(method);
    }

    public Package getParentPackage() {
        return (Package)getParent();
    }

    @Override
    public String toString() {
        return "Type("+this.getName()+")";
    }

    public Optional<Method> lookupMethodBySignature(String methodSignature) {
        if(methodLookup.containsKey(methodSignature)) {
            return Optional.of(methodLookup.get(methodSignature));
        } else {
            return Optional.empty();
        }
    }
}
