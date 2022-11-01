package org.jasome.input;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public abstract class Scanner<T> {
    private static final Logger logger = LoggerFactory.getLogger(Scanner.class);

    protected Project doScan(Collection<Pair<String, Map<String, String>>> sourceCode, String projectPath) {

        JavaSymbolSolver symbolSolver = configureParserAndResolver(sourceCode, projectPath);

        Project project = new Project(FilenameUtils.getBaseName(projectPath));
        project.setSymbolSolver(symbolSolver);
        Map<String, List<Pair<ClassOrInterfaceDeclaration, Map<String, String>>>> packages = gatherPackages(sourceCode);

        for (Map.Entry<String, List<Pair<ClassOrInterfaceDeclaration, Map<String, String>>>> entry : packages.entrySet()) {

            Package aPackage = new Package(entry.getKey());
            project.addPackage(aPackage);

            for (Pair<ClassOrInterfaceDeclaration, Map<String, String>> classAndAttributes : entry.getValue()) {
                ClassOrInterfaceDeclaration classDefinition = classAndAttributes.getLeft();
                Map<String, String> attributes = classAndAttributes.getRight();

                Type type = new Type(classDefinition);
                aPackage.addType(type);

                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    type.addAttribute(attribute);
                }

                type.addAttribute("lineStart", "" + classDefinition.getBegin().get().line);
                type.addAttribute("lineEnd", "" + classDefinition.getEnd().get().line);

                //We need to convert the constructor declarations to method declarations because we treat them the same, but javaparser don't have them sharing a useful common type
                for (ConstructorDeclaration constructorDeclaration : classDefinition.findAll(ConstructorDeclaration.class)) {
                    MethodDeclaration constructorMethodDeclaration = new MethodDeclaration(
                            constructorDeclaration.getModifiers(),
                            constructorDeclaration.getAnnotations(),
                            constructorDeclaration.getTypeParameters(),
                            JavaParser.parseClassOrInterfaceType(classDefinition.getName().getIdentifier()),
                            constructorDeclaration.getName(),
                            false,
                            constructorDeclaration.getParameters(),
                            constructorDeclaration.getThrownExceptions(),
                            constructorDeclaration.getBody()
                    );
                    Method constructor = new Method(constructorMethodDeclaration);
                    type.addMethod(constructor);

                    constructor.addAttribute("lineStart", "" + constructorDeclaration.getBegin().get().line);
                    constructor.addAttribute("lineEnd", "" + constructorDeclaration.getEnd().get().line);
                    constructor.addAttribute("constructor", "true");
                }

                for (MethodDeclaration methodDeclaration : classDefinition.getMethods()) {
                    Method method = new Method(methodDeclaration);
                    type.addMethod(method);

                    method.addAttribute("lineStart", "" + methodDeclaration.getBegin().get().line);
                    method.addAttribute("lineEnd", "" + methodDeclaration.getEnd().get().line);
                    method.addAttribute("constructor", "false");

                }

            }
        }

        return project;

    }

    private JavaSymbolSolver configureParserAndResolver(Collection<Pair<String, Map<String, String>>> sourceCode, String projectPath) {
        Set<File> sourceDirs = new HashSet<>();

        for (Pair<String, Map<String, String>> sourceFile : sourceCode) {
            String sourceCodeContent = sourceFile.getLeft();
            Map<String, String> attributes = sourceFile.getRight();

            try {
                CompilationUnit cu = JavaParser.parse(sourceCodeContent);

                String sourceFileName = attributes.get("sourceFile");

                Optional<String> packageName = cu.getPackageDeclaration().map((p) -> p.getName().asString());

                if (packageName.isPresent()) {
                    //String packagePrefix = packageName.get().replaceAll("[.]", File.separator) + "/";
                    String packagePrefix = "";
                    String[] pieces = packageName.get().split("\\.");
                    for (String piece : pieces) {
                        packagePrefix = packagePrefix.concat(piece).concat(File.separator);
                    }
                    
                    
                    String sourceDir = FilenameUtils.getPath(sourceFileName);
                    String baseSourceDir = sourceDir.replace(packagePrefix, "");
                    String finalSourceBaseDir = baseSourceDir.replace(".", projectPath);
                    sourceDirs.add(new File(finalSourceBaseDir));
                } else {
                    sourceDirs.add(new File(FilenameUtils.getPath(sourceFileName)));
                }

            } catch (ParseProblemException e) {
                //TODO
//                String file = attributes.get("sourceFile");
//                logger.warn("Unable to parse code from file %s, ignoring\n", file);
//                logger.warn(e.getProblems().toString());
            }
        }


        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);

        for (File sourceDir : sourceDirs) {
            try {
                combinedTypeSolver.add(new JavaParserTypeSolver(sourceDir));
            } catch (IllegalStateException e) {
                logger.warn("Unable to parse code from dir %s, ignoring\n", sourceDir);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.warn(sw.toString());
            }
        }

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);


        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setAttributeComments(false)
                .setSymbolResolver(symbolSolver);
        JavaParser.setStaticConfiguration(parserConfiguration);
        return symbolSolver;
    }


    private Map<String, List<Pair<ClassOrInterfaceDeclaration, Map<String, String>>>> gatherPackages(Collection<Pair<String, Map<String, String>>> sourcesAndAttributes) {

        Map<String, List<Pair<ClassOrInterfaceDeclaration, Map<String, String>>>> packages = new HashMap<>();

        for (Pair<String, Map<String, String>> sourceFile : sourcesAndAttributes) {
            String sourceCode = sourceFile.getLeft();
            Map<String, String> attributes = sourceFile.getRight();

            try {
                CompilationUnit cu = JavaParser.parse(sourceCode);

                String packageName = cu.getPackageDeclaration().map((p) -> p.getName().asString()).orElse("default");

                List<ClassOrInterfaceDeclaration> classes = cu.getNodesByType(ClassOrInterfaceDeclaration.class);

                if (!packages.containsKey(packageName)) {
                    packages.put(packageName, new ArrayList<>());
                }

                for (ClassOrInterfaceDeclaration clazz : classes) {
                    packages.get(packageName).add(Pair.of(clazz, attributes));
                }
            } catch (ParseProblemException e) {
                //TODO
//                String file = attributes.get("sourceFile");
//                logger.warn("Unable to parse code from file %s, ignoring\n", file);
//                logger.warn(e.getProblems().toString());
            }
        }

        return packages;
    }

}

