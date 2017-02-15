package org.jasome.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jasome.calculators.Metric;
import org.jscience.mathematics.number.Real;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

public class Matchers {

    public static Matcher<Set<Metric>> containsMetric(String name, Real value) {
        return new BaseMatcher<Set<Metric>>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                final Set<Metric> metrics = (Set<Metric>) item;
                Optional<Metric> namedMetric = metrics.stream().filter((m) -> m.getName().equalsIgnoreCase(name)).findFirst();
                return namedMetric.isPresent() &&
                        (
                                value.equals(namedMetric.get().getValue()) ||
                            value.approximates(namedMetric.get().getValue())
                        );


            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("expected metrics to contain").appendValue(name).appendValue(value);
            }
        };
    }

    public static Matcher<Set<Metric>> containsMetric(String name, long value) {
        return containsMetric(name, Real.valueOf(value));
    }
//
//    public static Matcher<Set<Metric>> containsMetric(String name, double value) {
//        return containsMetric(name, Real.valueOf(value));
//    }

    public static Matcher<Set<Metric>> doesNotContainMetric(String name) {
        return new BaseMatcher<Set<Metric>>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                final Set<Metric> metrics = (Set<Metric>) item;
                Optional<Metric> namedMetric = metrics.stream().filter((m) -> m.getName().equalsIgnoreCase(name)).findFirst();
                return !namedMetric.isPresent();
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("expected metrics to not contain").appendValue(name);
            }
        };
    }
}
