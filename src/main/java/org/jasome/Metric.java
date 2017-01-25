package org.jasome;

import java.math.BigDecimal;

public interface Metric<T> {

    BigDecimal calculate(T t);

}
