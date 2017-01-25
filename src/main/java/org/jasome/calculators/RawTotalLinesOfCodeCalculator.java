package org.jasome.calculators;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.Calculator;
import org.jasome.SomeClass;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Counts the raw number of lines of code within a class (excludes package
 * declaration, import statements, and comments outside of a class).  Within a
 * class declaration, will count whitespace, comments, multi-line statements,
 * and brackets.
 *
 * @author Rod Hilton
 * @since 0.1
 */
public class RawTotalLinesOfCodeCalculator implements Calculator<SomeClass> {

    @Override
    public Optional<BigDecimal> calculate(SomeClass someClass) {
        if (someClass == null || someClass.getClassDeclaration() == null)
            return Optional.empty();

        ClassOrInterfaceDeclaration decl = someClass.getClassDeclaration();

        Optional<Position> end = decl.getEnd();
        Optional<Position> begin = decl.getBegin();

        if (!begin.isPresent()) return Optional.empty();
        if (!end.isPresent()) return Optional.empty();

        return Optional.of(
                new BigDecimal(
                        end.get().line
                                - begin.get().line
                                + 1
                )
        );
    }
}
