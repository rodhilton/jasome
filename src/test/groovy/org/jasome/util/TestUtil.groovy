package org.jasome.util

import com.github.javafaker.Faker
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.PackageDeclaration
import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.jasome.input.FileScanner
import org.jasome.input.Method
import org.jasome.input.Package
import org.jasome.input.Project

import org.jasome.input.Type


class TestUtil {

    static Type typeFromSnippet(String sourceCode) {
        Package p = packageFromSnippet(sourceCode)
        assert p.getTypes().size() == 1

        return new ArrayList<Type>(p.types).get(0)
    }

    static Type typeFromStream(InputStream stream) {
        String snippet = IOUtils.toString(stream, "UTF-8");
        return typeFromSnippet(snippet);
    }

    static Method methodFromSnippet(String sourceCode) {
        String fakeSourceCode = """
        package org.test.example;
        
        public class MyClass {
            ${sourceCode}
        }
        """.trim()

        Type t = typeFromSnippet(fakeSourceCode)
        assert t.getMethods().size() == 1

        return new ArrayList<Method>(t.methods).get(0)
    }

    static Package packageFromSnippet(String sourceCode) {
        Project p = projectFromSnippet(sourceCode)
        assert p.getPackages().size() == 1

        return new ArrayList<Package>(p.packages).get(0)
    }

    static Project projectFromResources(String path) {
        File srcPath = new File(new File( "." ).getCanonicalPath(), "src/test/resources/"+path);

        augmentProjectMetaclass();

        return new FileScanner(srcPath).scan();
    }
    

    static Project projectFromSnippet(String... sourceCodes) {

        //File tempDir = File.createTempFile("jasome", "test")

        File tempDir = new File(Files.createTempDir(), new Faker().letterify("Test?????"))
        tempDir.mkdirs()
        System.err.println("Running tests using source dir "+tempDir)

        for(String sourceCode: sourceCodes) {
            CompilationUnit cu = JavaParser.parse(sourceCode);
            Optional<PackageDeclaration> packageDecOpt = cu.getPackageDeclaration();
            File rootDir;
            if(packageDecOpt.isPresent()) {
                PackageDeclaration packageDeclaration = packageDecOpt.get()
                String packageName = packageDeclaration.name.asString()
                String targetDir = packageName.replaceAll("[.]", File.separator)
                File targetFile = new File(tempDir, targetDir);
                targetFile.mkdirs()
                rootDir = targetFile;
            } else {
                rootDir = tempDir;
            }

            File sourceFile = new File(rootDir, new Faker().letterify("Source?????.java"))
            FileUtils.write(sourceFile, sourceCode, "UTF-8")

        }


        augmentProjectMetaclass();

        return new FileScanner(tempDir).scan();
    }

    static def augmentProjectMetaclass() {
        Project.metaClass.locateType = { String typeName ->
            Project p = (Project) delegate

            if(typeName.contains(".")) {
                int lastPeriod = typeName.lastIndexOf('.')
                String targetPkg = typeName.substring(0, lastPeriod)
                String targetTyp = typeName.substring(lastPeriod+1)

                for(Package pkg: p.getPackages()) {
                    if(pkg.name == targetPkg) {
                        for (Type typ : pkg.getTypes()) {
                            if (typ.name == targetTyp) return typ;
                        }
                    }
                }

            } else {
                for(Package pkg: p.getPackages()) {
                    for(Type typ: pkg.getTypes()) {
                        if(typ.name == typeName) return typ;
                    }
                }
            }


            return null;
        }
    }
}
