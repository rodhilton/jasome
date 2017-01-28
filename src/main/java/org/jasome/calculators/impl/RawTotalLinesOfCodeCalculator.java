package org.jasome.calculators.impl;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.common.collect.Sets;
import org.jasome.calculators.Calculation;
import org.jasome.calculators.Calculator;
import org.jasome.SomeClass;
import org.jasome.calculators.ClassMetricCalculator;
import org.jasome.calculators.SourceContext;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

/**
 * Counts the raw number of lines of code within a class (excludes package
 * declaration, import statements, and comments outside of a class).  Within a
 * class declaration, will count whitespace, comments, multi-line statements,
 * and brackets.
 *
 * @author Rod Hilton
 * @since 0.1
 */
public class RawTotalLinesOfCodeCalculator implements ClassMetricCalculator {

    @Override
    public Set<Calculation> calculate(ClassOrInterfaceDeclaration decl, SourceContext context) {
        assert decl != null;

        Optional<Position> end = decl.getEnd();
        Optional<Position> begin = decl.getBegin();

        if (!begin.isPresent()) return Sets.newHashSet();
        if (!end.isPresent()) return Sets.newHashSet();

        Calculation result = new Calculation(
                "RTLOC",
                "Raw Total Lines of Code Count",
                new BigDecimal(
                        end.get().line
                                - begin.get().line
                                + 1
                )
        );

        return Sets.newHashSet(result);
    }
}
