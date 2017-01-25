package org.jasome.calculators;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.jasome.Calculator;
import org.jasome.SomeClass;

import java.math.BigDecimal;
import java.util.Optional;

public class TotalLinesOfCodePlugin implements Calculator<SomeClass> {

    public Optional<BigDecimal> calculate(SomeClass someClass) {

        ClassOrInterfaceDeclaration decl = someClass.getClassDeclaration();
        return Optional.of(new BigDecimal(decl.getEnd().get().line - decl.getBegin().get().line));
    }
}
