package org.jasome.calculators;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.List;
import java.util.Optional;

public class SourceContext {

    public static SourceContext NONE=new SourceContext();
    private String packageName;
    private List<ImportDeclaration> imports;
    private Optional<ClassOrInterfaceDeclaration> classDefinition;

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setImports(List<ImportDeclaration> imports) {
        this.imports = imports;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<ImportDeclaration> getImports() {
        return imports;
    }

    public <T> void setClassDefinition(Optional<ClassOrInterfaceDeclaration> classDefinition) {
        this.classDefinition = classDefinition;
    }

    public Optional<ClassOrInterfaceDeclaration> getClassDefinition() {
        return classDefinition;
    }
    //package
    //import statements
    //whatever else needs to be crammed in?
}
