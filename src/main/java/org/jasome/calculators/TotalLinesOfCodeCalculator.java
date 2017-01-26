package org.jasome.calculators;

import com.github.javaparser.Position;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.common.collect.Sets;
import org.jasome.Calculation;
import org.jasome.Calculator;
import org.jasome.SomeClass;

import java.math.BigDecimal;
import java.util.*;

public class TotalLinesOfCodeCalculator implements Calculator<SomeClass> {

    public Set<Calculation> calculate(SomeClass someClass) {

        ClassOrInterfaceDeclaration decl = someClass.getClassDeclaration();
        List<NodeList<?>> nodes = decl.getNodeLists();

        System.out.println(nodes);

        return Sets.newHashSet();
    }
}
