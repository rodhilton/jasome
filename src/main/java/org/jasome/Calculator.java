package org.jasome;

import java.util.Set;

public interface Calculator<T> {

    Set<Calculation> calculate(T t);

}
