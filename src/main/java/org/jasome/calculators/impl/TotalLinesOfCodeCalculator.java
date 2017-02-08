package org.jasome.calculators.impl;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.google.common.collect.Sets;
import org.jasome.calculators.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

/**
 * Counts the number of lines of code in a file.  Attempts to normalize for
 * different formatting styles and whitespace differences so equivalent code
 * counts as the same number of lines.
 *
 * For example:
 * <code>
 * import com.library1.Thing;
 * public class ExampleClass {
 *
 *     public int field = 45;
 *
 *     public void aMethod() {
 *         for(int i=0;i<10;i++) {
 *             System.out.println(new Thing().getOutput());
 *         }
 *     }
 * }
 * </code>
 *
 * will count as the same number of lines (8) as:
 *
 * <code>
 * public class ExampleClass
 * {
 *
 *     public int
 *         field = 45;
 *
 *     public void aMethod()
 *     {
 *
 *         for (
 *             int i=0;
 *             i<10;
 *             i++)
 *         {
 *
 *             System.out.println(
 *                 new com.library1.Thing().getOutput()
 *             );
 *         }
 *     }
 *
 * }
 * </code>
 *
 * This method does make the count susceptible to differences in "code golf"
 * styles where code is compressed down to a "one-liner".  A one-liner will count
 * as one line, even if that line is incredibly complex.  For example, an if
 * statement with a body and an else clause will count as 5 or so lines, whereas
 * equivalent code using a ternary operator will count as 1.
 *
 * Similarly, code that makes use of Java's 1.8 functional features will, if
 * expressed as a single line of functional code, count as a single line,
 * regardless of how complexly map and stream functions might be chained together.
 *
 * @author Rod Hilton
 * @since 0.1
 */
public class TotalLinesOfCodeCalculator implements ClassMetricCalculator, PackageMetricCalculator, MethodMetricCalculator {

    @Override
    public Set<Metric> calculate(ClassOrInterfaceDeclaration decl, SourceContext context) {
        assert(decl != null);

        Stack<Node> nodeStack = new Stack<Node>();
        nodeStack.add(decl);

        Metric result = performCalculation(nodeStack);

        return Metrics.of(result);
    }

    @Override
    public Set<Metric> calculate(Collection<ClassOrInterfaceDeclaration> classes, SourceContext context) {
        assert(classes != null);

        Stack<Node> nodeStack = new Stack<Node>();
        nodeStack.addAll(classes);

        Metric result = performCalculation(nodeStack);

        return Metrics.of(result);
    }

    @Override
    public Set<Metric> calculate(MethodDeclaration declaration, SourceContext context) {
        assert(declaration != null);

        Stack<Node> nodeStack = new Stack<Node>();
        nodeStack.add(declaration);

        Metric result = performCalculation(nodeStack);

        return Metrics.of(result);
    }


    private Metric performCalculation(Stack<Node> nodeStack) {
        int count = 0;

        while (!nodeStack.empty()) {
            Node node = nodeStack.pop();

            if (node instanceof FieldDeclaration) {
                count = count + 1;
            } else if (node instanceof MethodDeclaration) {
                count = count + 1; //for the opening of the method
                Optional<BlockStmt> body = ((MethodDeclaration) node).getBody();
                if (body.isPresent()) {
                    count = count + 1; //for the closing, only happens if there's a body
                    nodeStack.add(body.get());
                }
            } else if (node instanceof InitializerDeclaration) {
                count = count + 2; //for the opening and closing of the initializer block
                nodeStack.add(((InitializerDeclaration) node).getBody());
            } else if (node instanceof ConstructorDeclaration) {
                count = count + 2; //for the opening and closing of the method
                nodeStack.add(((ConstructorDeclaration) node).getBody());
            } else if (node instanceof ClassOrInterfaceDeclaration) {
                count = count + 2; //for the opening and closing of the declaration
                ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) node;
                nodeStack.addAll(classOrInterfaceDeclaration.getMembers());
            } else if (node instanceof EnumDeclaration) {
                count = count + 2; //for the opening and closing of the declaration
                EnumDeclaration enumDeclaration = (EnumDeclaration) node;
                nodeStack.addAll(enumDeclaration.getEntries());
                nodeStack.addAll(enumDeclaration.getMembers());
            } else if (node instanceof EnumConstantDeclaration) {
                count = count + 1;
                EnumConstantDeclaration enumConstantDeclaration = (EnumConstantDeclaration) node;
                if (enumConstantDeclaration.getClassBody().size() > 0) {
                    count = count + 1; //for the closing, we already counted the opening
                    nodeStack.addAll(enumConstantDeclaration.getClassBody());
                }
            } else if (node instanceof AnnotationDeclaration) {
                count = count + 2; //for the opening and closing of the declaration
                AnnotationDeclaration annotationDeclaration = (AnnotationDeclaration) node;
                nodeStack.addAll(annotationDeclaration.getMembers());
            } else if (node instanceof EmptyMemberDeclaration) {
                //Ignore, it's empty
            } else if (node instanceof AnnotationMemberDeclaration) {
                count = count + 1;
            } else if (node instanceof LocalClassDeclarationStmt) {
                //Count nothing, this contains a ClassOrInterfaceDeclaration which will count the 2 lines
                nodeStack.add(((LocalClassDeclarationStmt) node).getClassDeclaration());
            } else if (node instanceof ExplicitConstructorInvocationStmt) {
                count = count + 1;
            } else if (node instanceof BlockStmt) {
                NodeList<Statement> statements = ((BlockStmt) node).getStatements();
                nodeStack.addAll(statements);
            } else if (node instanceof ForStmt) {
                count = count + 2; //2 for the opening and closing of the for statement, we ignore complexity within the loop condition itself
                nodeStack.add(((ForStmt) node).getBody());
            } else if (node instanceof ForeachStmt) {
                count = count + 2; //2 for the opening and closing of the foreach statement
                nodeStack.add(((ForeachStmt) node).getBody());
            } else if (node instanceof WhileStmt) {
                count = count + 2; //2 for the opening and closing of the while statement
                nodeStack.add(((WhileStmt) node).getBody());
            } else if (node instanceof DoStmt) {
                count = count + 2; //2 for the opening and closing of the do statement
                nodeStack.add(((DoStmt) node).getBody());
            } else if (node instanceof ClassOrInterfaceType) {
                //Ignore
            } else if (node instanceof SingleMemberAnnotationExpr || node instanceof MarkerAnnotationExpr || node instanceof NormalAnnotationExpr) {
                //Ignore, we don't count annotations
            } else if (node instanceof TypeParameter) {
                //Type parameters (generics) are not statements and don't count
            } else if (node instanceof SynchronizedStmt) {
                count = count + 2; //this is a synchronized block, not a modifier, so it's got an opening and closing
                nodeStack.add(((SynchronizedStmt) node).getBody());
            } else if (node instanceof ExpressionStmt) {
                count = count + 1;
                //nodeStack.add(((ExpressionStmt)node).getExpression()); //TODO: maybe be a little smarter, go into more detail here since there can be so much code in an expression.  same for the statements below?
            } else if (node instanceof EmptyStmt) {
                //It's empty, ignore.  Usually this is just a double semicolon
            } else if (node instanceof AssertStmt) {
                count = count + 1;
            } else if (node instanceof ReturnStmt) {
                count = count + 1;
            } else if (node instanceof ThrowStmt) {
                count = count + 1;
            } else if (node instanceof BreakStmt) {
                count = count + 1;
            } else if (node instanceof ContinueStmt) {
                count = count + 1;
            } else if (node instanceof LabeledStmt) {
                count = count + 1;
            } else if (node instanceof IfStmt) {
                count = count + 2;
                IfStmt ifStmt = (IfStmt) node;
                nodeStack.add(ifStmt.getThenStmt());
                if (ifStmt.getElseStmt().isPresent()) {
                    count = count + 1; //Only 1 because the close of the if is the start of the else
                    nodeStack.add(ifStmt.getElseStmt().get());
                }
            } else if (node instanceof TryStmt) {
                TryStmt tryStmt = (TryStmt) node;
                if (tryStmt.getTryBlock().isPresent()) {
                    count = count + 2;
                    nodeStack.add(tryStmt.getTryBlock().get());
                }
                for (CatchClause catchClause : tryStmt.getCatchClauses()) {
                    count = count + 1; //Only 1 becuase the close brace of the try is the start of the catch clause
                    nodeStack.add(catchClause.getBody());
                }

                if (tryStmt.getFinallyBlock().isPresent()) {
                    count = count + 1; //Only 1 because the close brace of the catches/try is the start of the finally clause
                    nodeStack.add(tryStmt.getFinallyBlock().get());
                }
            } else if (node instanceof SwitchStmt) {
                count = count + 2;
                SwitchStmt switchStmt = (SwitchStmt) node;
                nodeStack.addAll(switchStmt.getEntries());
            } else if (node instanceof SwitchEntryStmt) {
                count = count + 1;
                SwitchEntryStmt switchEntryStmt = (SwitchEntryStmt) node;
                nodeStack.addAll(switchEntryStmt.getStatements());
            } else {
                //TODO: logger?
                System.err.println("Encountered type I'm not ready for: " + node.getClass());
                System.err.println("Lines " + node.getBegin().get().line + " to " + node.getEnd().get().line + "\n");
            }
        }

        return new Metric(
                "TLOC",
                "Total Lines of Code",
                new BigDecimal(count)
        );
    }

}
