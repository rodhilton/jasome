package org.jasome.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.SomeClass;
import org.jasome.plugins.TotalLinesOfCodePlugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;

public class JasomeScanner {
    public static void main(String[] args) throws IOException {
        JasomeScanner scanner = new JasomeScanner();
        Path path = FileSystems.getDefault().getPath("/Users/air0day/Projects/ucd/calvary_projects/commons-codec/src");
        scanner.scan(path);
    }

    private void scan(Path path) throws IOException {

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

                    List<ClassOrInterfaceDeclaration> classes = cu.getNodesByType(ClassOrInterfaceDeclaration.class);

                    TotalLinesOfCodePlugin plugin = new TotalLinesOfCodePlugin();

                    System.out.println(cu.getPackageDeclaration().get().getName());

                    for(ClassOrInterfaceDeclaration clazz: classes) {
                        BigDecimal d = plugin.calculate(new SomeClass(clazz));
                        System.out.println(clazz.getName());
                        System.out.println(d);
                    }

                }

                return FileVisitResult.CONTINUE;
            }

        };

        Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);

    }


}
