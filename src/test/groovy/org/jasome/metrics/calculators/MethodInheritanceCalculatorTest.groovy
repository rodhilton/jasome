package org.jasome.metrics.calculators

import org.jasome.input.Type
import org.jasome.metrics.value.NumericValue
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.Matchers.doesNotContainMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class MethodInheritanceCalculatorTest extends Specification {

    def "calculates inheritance factor"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class A {
            public int doStuff() { return 5; }
            protected void moreStuff() { }
            public void forFree() { }
        }
        
        interface I {
            int getThing();   
        }
        
        class B implements I {
            public void doBStuff() {}
            public int doStuff() { return 4; }
            public int getThing() { return 666; }
        }
        
        class C extends B implements Q {
            //Does not override doStuff()
            //gets forFree free
            //has moreStuff but it's not public
            //inherits getQ but does not override it
        }
        
        class D extends C implements Q {
            public int doStuff() { return 3; }
            public int getQ() { return 123; }
            //Has the same stuff as C but does override doStuff
            public D getAnotherD() { return new D(); }
        }
        
        interface Q {
            default int getQ() {
                return 5;
            }
        }
        '''

        Type typeA = project.locateType("A")
        Type typeB = project.locateType("B")
        Type typeC = project.locateType("C")
        Type typeD = project.locateType("D")
        Type interfaceI = project.locateType("I")
        Type interfaceQ = project.locateType("Q")

        when:
        def resultA = new MethodInheritanceCalculator().calculate(typeA);
        def resultB = new MethodInheritanceCalculator().calculate(typeB);
        def resultC = new MethodInheritanceCalculator().calculate(typeC);
        def resultD = new MethodInheritanceCalculator().calculate(typeD);
        def resultI = new MethodInheritanceCalculator().calculate(interfaceI);
        def resultQ = new MethodInheritanceCalculator().calculate(interfaceQ);

        then:
        expect resultA, containsMetric("Mit", 0)
        expect resultB, containsMetric("Mit", 0)
        expect resultC, containsMetric("Mit", 4)
        expect resultD, containsMetric("Mit", 4)
        expect resultI, containsMetric("Mit", 0)
        expect resultQ, containsMetric("Mit", 0)

        expect resultA, containsMetric("Mi", 0)
        expect resultB, containsMetric("Mi", 0) //"Inheriting" a method from an interface doesn't count unless the impl is defined on the interface
        expect resultC, containsMetric("Mi", 4)
        expect resultD, containsMetric("Mi", 2)
        expect resultI, containsMetric("Mi", 0)
        expect resultQ, containsMetric("Mi", 0)

        expect resultA, containsMetric("Md", 3)
        expect resultB, containsMetric("Md", 3)
        expect resultC, containsMetric("Md", 0)
        expect resultD, containsMetric("Md", 3)
        expect resultI, containsMetric("Md", 0)
        expect resultQ, containsMetric("Md", 1)

        expect resultA, containsMetric("Mo", 0)
        expect resultB, containsMetric("Mo", 0)
        expect resultC, containsMetric("Mo", 0)
        expect resultD, containsMetric("Mo", 2)
        expect resultI, containsMetric("Mo", 0)
        expect resultQ, containsMetric("Mo", 0)

        expect resultA, containsMetric("Ma", 3)
        expect resultB, containsMetric("Ma", 3)
        expect resultC, containsMetric("Ma", 4)
        expect resultD, containsMetric("Ma", 5)
        expect resultI, containsMetric("Ma", 0)
        expect resultQ, containsMetric("Ma", 1)

        expect resultA, containsMetric("MIF", 0)
        expect resultB, containsMetric("MIF", 0)
        expect resultC, containsMetric("MIF", 1)
        expect resultD, containsMetric("MIF", NumericValue.of(2).divide(NumericValue.of(5)))
        expect resultI, doesNotContainMetric("MIF")
        expect resultQ, containsMetric("MIF", 0)
    }

    def "calculates method hiding factor and public methods ratio"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class A {
            public int doStuff() { return 5; }
            protected void moreStuff() { }
            public void forFree() { }
        }
        
        interface I {
            int getThing();   
        }
        
        class B implements I {
            public void doBStuff() {}
            public int doStuff() { return 4; }
            public int getThing() { return 666; }
            private void invisible() {}
        }
        
        class C extends B implements Q {
            //Does not override doStuff()
            //gets forFree free
            //has moreStuff but it's not public
            //inherits getQ but does not override it
            void packageScope() { }
        }
        
        class D extends C implements Q {
            public int doStuff() { return 3; }
            public int getQ() { return 123; }
            //Has the same stuff as C but does override doStuff
            public D getAnotherD() { return new D(); }
        }
        
        interface Q {
            default int getQ() {
                return 5;
            }
        }
        '''

        Type typeA = project.locateType("A")
        Type typeB = project.locateType("B")
        Type typeC = project.locateType("C")
        Type typeD = project.locateType("D")
        Type interfaceI = project.locateType("I")
        Type interfaceQ = project.locateType("Q")

        when:
        def resultA = new MethodInheritanceCalculator().calculate(typeA);
        def resultB = new MethodInheritanceCalculator().calculate(typeB);
        def resultC = new MethodInheritanceCalculator().calculate(typeC);
        def resultD = new MethodInheritanceCalculator().calculate(typeD);
        def resultI = new MethodInheritanceCalculator().calculate(interfaceI);
        def resultQ = new MethodInheritanceCalculator().calculate(interfaceQ);

        then:
        expect resultA, containsMetric("PMd", 2)
        expect resultB, containsMetric("PMd", 3)
        expect resultC, containsMetric("PMd", 0)
        expect resultD, containsMetric("PMd", 3)
        expect resultI, containsMetric("PMd", 0) //The method isn't "defined" on the interface, only its contract
        expect resultQ, containsMetric("PMd", 0)

        expect resultA, containsMetric("PMi", 0)
        expect resultB, containsMetric("PMi", 0)
        expect resultC, containsMetric("PMi", 3)
        expect resultD, containsMetric("PMi", 2)
        expect resultI, containsMetric("PMi", 0)
        expect resultQ, containsMetric("PMi", 0)

        expect resultA, containsMetric("PMR", NumericValue.ofRational(2, 3))
        expect resultB, containsMetric("PMR", NumericValue.ofRational(3, 4))
        expect resultC, containsMetric("PMR", NumericValue.ofRational(3, 5))
        expect resultD, containsMetric("PMR", NumericValue.ofRational(5, 6))
        expect resultI, doesNotContainMetric("PMR")
        expect resultQ, containsMetric("PMR", NumericValue.of(0))

        expect resultA, containsMetric("HMd", 1)
        expect resultB, containsMetric("HMd", 1)
        expect resultC, containsMetric("HMd", 1)
        expect resultD, containsMetric("HMd", 0)
        expect resultI, containsMetric("HMd", 0)
        expect resultQ, containsMetric("HMd", 1)

        expect resultA, containsMetric("HMi", 0)
        expect resultB, containsMetric("HMi", 0)
        expect resultC, containsMetric("HMi", 1)
        expect resultD, containsMetric("HMi", 0)
        expect resultI, containsMetric("HMi", 0)
        expect resultQ, containsMetric("HMi", 0)

        expect resultA, containsMetric("MHF", NumericValue.ofRational(2,3))
        expect resultB, containsMetric("MHF", NumericValue.ofRational(3,4))
        expect resultC, containsMetric("MHF", 0)
        expect resultD, containsMetric("MHF", 1)
        expect resultI, doesNotContainMetric("MHF")
        expect resultQ, containsMetric("MHF", 0)
    }
}
