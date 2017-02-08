package org.jasome.util

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.apache.commons.io.IOUtils
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

    public static Type typeFromStream(InputStream stream) {
        String snippet = IOUtils.toString(stream, "UTF-8");
        return typeFromSnippet(snippet);
    }
}
