package org.jasome.metrics;

import org.jasome.input.Code;

import java.util.Set;

public interface Calculator<T extends Code> {

    Set<Metric> calculate(T t);

}
