package org.jasome.calculators;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class SourceContext {

    public static SourceContext NONE=new SourceContext();
    private String packageName;
    private List<ImportDeclaration> imports;
    private ClassOrInterfaceDeclaration classDefinition = null;
    private File sourceFile;

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

    public void setClassDefinition(ClassOrInterfaceDeclaration classDefinition) {
        this.classDefinition = classDefinition;
    }

    public Optional<ClassOrInterfaceDeclaration> getClassDefinition() {
        return Optional.of(classDefinition);
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public File getSourceFile() {
        return sourceFile;
    }
    //package
    //import statements
    //whatever else needs to be crammed in?
}
