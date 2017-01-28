package org.jasome.calculators;

import java.util.Set;

public interface Calculator<T> {

    Set<Calculation> calculate(T t);

}
