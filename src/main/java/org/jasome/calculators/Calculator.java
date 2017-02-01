package org.jasome.calculators;

import java.util.Set;

public interface Calculator<T> {

    Set<Metric> calculate(T t);

}
