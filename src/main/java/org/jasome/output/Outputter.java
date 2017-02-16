package org.jasome.output;

import org.jasome.input.Project;

public interface Outputter<T> {
    T output(Project project);
}
