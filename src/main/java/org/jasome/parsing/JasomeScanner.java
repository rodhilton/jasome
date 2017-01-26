package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.SomeClass;
import org.jasome.calculators.TotalLinesOfCodePlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class JasomeScanner {

    public void scanFile(File file) {

    }

    public void scan(Path path) throws IOException {

        Map<String, List<SomeClass>> packages = new HashMap<String, List<SomeClass>>();

        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                //parser returns a package... which has many classes, each of which have many methods.  each of those things contains the javaparser root for that file.

                if(file.getFileName().toString().endsWith(".java")) {
                    System.out.println("Visiting " + file.toString());
                    FileInputStream in = new FileInputStream(file.toFile());

                    // parse the file
                    CompilationUnit cu = JavaParser.parse(in);
//                    cu.getNodesByType(ClassOrInterfaceDeclaration.class).
//                            forEach(f -> System.out.println("  Check field at line " + f.getBegin().get().line + " to "+f.getEnd().get().line));

                    //after we've parsed the file and converted it to a JasomeClass/Method/Package structure (ready to give it to plugins)...
                    //create a thread pool executor and make threads that are a pairing of a compilation unit and a plugin.
                    //so if we have 10 plugins and 20 files, we should make ~200 threads.  execute all in parallel and come back together when done
                    //scanning a directory

                    //TODO: this won't get anonymous classes or inline classes will it?
                    List<ClassOrInterfaceDeclaration> classes = cu.getNodesByType(ClassOrInterfaceDeclaration.class);

                    String packageName = cu.getPackageDeclaration().map((p) -> p.getName().asString()).orElse("default");

                    if(!packages.containsKey(packageName)) {
                        packages.put(packageName, new ArrayList<SomeClass>());
                    }

                    for(ClassOrInterfaceDeclaration clazz: classes) {
                        packages.get(packageName).add(new SomeClass(clazz));
                    }
                }

                return FileVisitResult.CONTINUE;
            }

        };

        Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);

        System.out.println(packages);

    }


}
