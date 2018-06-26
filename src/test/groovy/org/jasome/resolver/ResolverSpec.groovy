package org.jasome.resolver

import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.symbolsolver.javaparser.Navigator
import org.jasome.input.Project
import org.jasome.input.Type
import org.jasome.metrics.calculators.TotalLinesOfCodeCalculator
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.projectFromResources
import static org.jasome.util.TestUtil.typeFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class ResolverSpec extends Specification {

//    def "calculate class length when only two lines (open and close)"() {
//
//        given:
//        def project = projectFromResources("org/jasome/resolver")
//
//        when:
//        Type t = project.locateType("Test")
//
//        then:
//        //FieldDeclaration fieldDeclaration = Navigator.findNodeOfGivenClass(t.source, FieldDeclaration.class);
//    //    System.out.println("Field type: " + fieldDeclaration.getVariables().get(0).getType().resolve().asReferenceType().getQualifiedName());
//    }
}
