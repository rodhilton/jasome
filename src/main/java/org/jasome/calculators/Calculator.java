package org.jasome.calculators;

import org.jasome.parsing.Code;

import java.util.Set;

public interface Calculator<T extends Code> {

    Set<Metric> calculate(T t);

}
