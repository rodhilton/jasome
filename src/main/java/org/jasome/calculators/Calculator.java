package org.jasome.calculators;

import org.jasome.parsing.TreeNode;

import java.util.Set;

public interface Calculator<T extends TreeNode> {

    Set<Metric> calculate(T t);

}
