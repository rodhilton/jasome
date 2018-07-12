package org.jasome.metrics;

import org.jasome.input.Code;
import org.jasome.util.ProjectMetadata;

import java.util.Set;

public interface Calculator<T extends Code> {

    Set<Metric> calculate(T t);

}
