package org.jasome.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jasome.metrics.Metric;
import org.jasome.metrics.value.NumericValue;

import java.util.Optional;
import java.util.Set;

public class Matchers {

    public static Matcher<Set<Metric>> containsMetric(String name, NumericValue value) {
        return new BaseMatcher<Set<Metric>>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                final Set<Metric> metrics = (Set<Metric>) item;
                if(metrics == null || metrics.size() == 0) return false;
                Optional<Metric> namedMetric = metrics
                        .stream()
                        .filter((m) -> m.getName().equalsIgnoreCase(name))
                        .findFirst();
                return namedMetric.isPresent() &&
                        Math.abs(value.doubleValue() - namedMetric.get().getValue().doubleValue()) < 0.000000000001;


            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("expected metrics to contain").appendValue(name).appendValue(value);
            }
        };
    }

    public static Matcher<Set<Metric>> containsMetric(String name, double value) {
        return containsMetric(name, NumericValue.of(value));
    }

    public static Matcher<Set<Metric>> containsMetric(String name, long value) {
        return containsMetric(name, NumericValue.of(value));
    }
//
//    public static Matcher<Set<Metric>> containsMetric(String name, double value) {
//        return containsMetric(name, Real.of(value));
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
