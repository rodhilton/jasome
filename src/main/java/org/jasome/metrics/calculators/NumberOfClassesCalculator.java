package org.jasome.metrics.calculators;

import com.google.common.collect.ImmutableSet;
import org.jasome.input.Package;
import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.Set;

/**
 * Simply counts the number of classes in a package.  Only counts top-level classes, not inner or anonymous classes.
 *
 * @author Rod Hilton
 * @since 0.3
 */
public class NumberOfClassesCalculator implements Calculator<Package> {

    @Override
    public Set<Metric> calculate(Package aPackage) {
        return ImmutableSet.of(Metric.of("NOC", "Number of Classes", aPackage.getTypes().size()));
    }
}
