package org.jasome.output;

import org.jasome.parsing.Project;

public interface Outputter<T> {
    T output(Project project);
}
