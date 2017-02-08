package org.jasome.calculators.impl;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.calculators.TypeMetricCalculator;
import org.jasome.calculators.Metric;
import org.jasome.calculators.Metrics;
import org.jasome.parsing.Type;

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
public class RawTotalLinesOfCodeCalculator implements TypeMetricCalculator {

    @Override
    public Set<Metric> calculate(Type type) {

        ClassOrInterfaceDeclaration decl = type.getSource();

        Optional<Position> end = decl.getEnd();
        Optional<Position> begin = decl.getBegin();

        if (!begin.isPresent()) return Metrics.EMPTY;
        if (!end.isPresent()) return Metrics.EMPTY;

        return Metrics.builder().with("RTLOC", "Raw Total Lines of Code", end.get().line - begin.get().line + 1).build();
    }
}
