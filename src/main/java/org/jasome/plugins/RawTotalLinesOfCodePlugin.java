package org.jasome.plugins;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.SomeClass;
import org.jasome.Metric;

import java.math.BigDecimal;

/**
 * Counts the raw number of lines of code within a class (excludes package
 * declaration, import statements, and comments outside of a class).  Within a
 * class declaration, will count whitespace, comments, multi-line statements,
 * and brackets.
 *
 * @author Rod Hilton
 * @since 0.1
 */
public class RawTotalLinesOfCodePlugin implements Metric<SomeClass> {

    @Override
    public BigDecimal calculate(SomeClass someClass) {
        ClassOrInterfaceDeclaration decl = someClass.getClassDeclaration();
        return new BigDecimal(
                decl.getEnd().get().line
                        - decl.getBegin().get().line
                        + 1);
    }
}
