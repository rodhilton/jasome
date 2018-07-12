package org.jasome.metrics.calculators;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.common.collect.ImmutableSet;
import org.jasome.input.Type;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

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
public class RawTotalLinesOfCodeCalculator implements Calculator<Type> {

    @Override
    public Set<Metric> calculate(Type type) {

        ClassOrInterfaceDeclaration decl = type.getSource();

        Optional<Position> end = decl.getEnd();
        Optional<Position> begin = decl.getBegin();

        if (!begin.isPresent()) return ImmutableSet.of();
        if (!end.isPresent()) return ImmutableSet.of();
        
        return ImmutableSet.of(Metric.of("RTLOC", "Raw Total Lines of Code", end.get().line - begin.get().line + 1));
    }
}
