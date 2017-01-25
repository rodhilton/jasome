package org.jasome;

import java.math.BigDecimal;
import java.util.Optional;

public interface Calculator<T> {

    Optional<BigDecimal> calculate(T t);

}
