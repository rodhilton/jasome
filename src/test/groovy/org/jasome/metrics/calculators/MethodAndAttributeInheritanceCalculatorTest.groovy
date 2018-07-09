package org.jasome.metrics.calculators

import org.jasome.input.Type
import org.jasome.metrics.value.NumericValue
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.Matchers.doesNotContainMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class MethodAndAttributeInheritanceCalculatorTest extends Specification {

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
        def resultA = new MethodAndAttributeInheritanceCalculator().calculate(typeA);
        def resultB = new MethodAndAttributeInheritanceCalculator().calculate(typeB);
        def resultC = new MethodAndAttributeInheritanceCalculator().calculate(typeC);
        def resultD = new MethodAndAttributeInheritanceCalculator().calculate(typeD);
        def resultI = new MethodAndAttributeInheritanceCalculator().calculate(interfaceI);
        def resultQ = new MethodAndAttributeInheritanceCalculator().calculate(interfaceQ);

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

        expect resultA, doesNotContainMetric("NMIR")
        expect resultB, doesNotContainMetric("NMIR")
        expect resultC, containsMetric("NMIR", 100)
        expect resultD, containsMetric("NMIR", NumericValue.ofRational(1,2).times(NumericValue.of(100)))
        expect resultI, doesNotContainMetric("NMIR")
        expect resultQ, doesNotContainMetric("NMIR")

        expect resultA, doesNotContainMetric("PF")
        expect resultB, doesNotContainMetric("PF")
        expect resultC, doesNotContainMetric("PF")
        expect resultD, doesNotContainMetric("PF")
        expect resultI, doesNotContainMetric("PF")
        expect resultQ, doesNotContainMetric("PF")
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
        def resultA = new MethodAndAttributeInheritanceCalculator().calculate(typeA);
        def resultB = new MethodAndAttributeInheritanceCalculator().calculate(typeB);
        def resultC = new MethodAndAttributeInheritanceCalculator().calculate(typeC);
        def resultD = new MethodAndAttributeInheritanceCalculator().calculate(typeD);
        def resultI = new MethodAndAttributeInheritanceCalculator().calculate(interfaceI);
        def resultQ = new MethodAndAttributeInheritanceCalculator().calculate(interfaceQ);

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

    def "calculates attribute factors"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class A {
            public int doStuff = 5;
            protected String moreStuff = "more stuff";
            public String forFree = "for free";
        }
        
        interface I {
            int getThing = 15;   
        }
        
        class B implements I {
            public String doBStuff = "doBStuff";
            public int doStuff = 4;
            public int getThing = 666;
        }
        
        class C extends B {
            //Does not override doStuff
            //gets forFree free
            //has moreStuff but it's not public
        }
        
        class D extends C {
            public int doStuff = 19;
            //Has the same stuff as C but does override doStuff
        }
        
        interface Q {
            int q=5;
        }
        '''

        Type typeA = project.locateType("A")
        Type typeB = project.locateType("B")
        Type typeC = project.locateType("C")
        Type typeD = project.locateType("D")
        Type interfaceI = project.locateType("I")
        Type interfaceQ = project.locateType("Q")

        when:
        def resultA = new MethodAndAttributeInheritanceCalculator().calculate(typeA);
        def resultB = new MethodAndAttributeInheritanceCalculator().calculate(typeB);
        def resultC = new MethodAndAttributeInheritanceCalculator().calculate(typeC);
        def resultD = new MethodAndAttributeInheritanceCalculator().calculate(typeD);
        def resultI = new MethodAndAttributeInheritanceCalculator().calculate(interfaceI);
        def resultQ = new MethodAndAttributeInheritanceCalculator().calculate(interfaceQ);

        then:
        expect resultA, containsMetric("Ait", 0)
        expect resultB, containsMetric("Ait", 1)
        expect resultC, containsMetric("Ait", 3)
        expect resultD, containsMetric("Ait", 3)
        expect resultI, containsMetric("Ait", 0)
        expect resultQ, containsMetric("Ait", 0)

        expect resultA, containsMetric("Ai", 0)
        expect resultB, containsMetric("Ai", 0)
        expect resultC, containsMetric("Ai", 3)
        expect resultD, containsMetric("Ai", 2)
        expect resultI, containsMetric("Ai", 0)
        expect resultQ, containsMetric("Ai", 0)

        expect resultA, containsMetric("Ad", 3)
        expect resultB, containsMetric("Ad", 3)
        expect resultC, containsMetric("Ad", 0)
        expect resultD, containsMetric("Ad", 1)
        expect resultI, containsMetric("Ad", 1)
        expect resultQ, containsMetric("Ad", 1)

        expect resultA, containsMetric("Ao", 0)
        expect resultB, containsMetric("Ao", 1)
        expect resultC, containsMetric("Ao", 0)
        expect resultD, containsMetric("Ao", 1)
        expect resultI, containsMetric("Ao", 0)
        expect resultQ, containsMetric("Ao", 0)

        expect resultA, containsMetric("Aa", 3)
        expect resultB, containsMetric("Aa", 3)
        expect resultC, containsMetric("Aa", 3)
        expect resultD, containsMetric("Aa", 3)
        expect resultI, containsMetric("Aa", 1)
        expect resultQ, containsMetric("Aa", 1)

        expect resultA, containsMetric("AIF", 0)
        expect resultB, containsMetric("AIF", 0)
        expect resultC, containsMetric("AIF", 1)
        expect resultD, containsMetric("AIF", NumericValue.ofRational(2, 3))
        expect resultI, containsMetric("AIF", 0)
        expect resultQ, containsMetric("AIF", 0)

        expect resultA, containsMetric("Av", 2)
        expect resultB, containsMetric("Av", 3)
        expect resultC, containsMetric("Av", 0)
        expect resultD, containsMetric("Av", 1)
        expect resultI, containsMetric("Av", 1)
        expect resultQ, containsMetric("Av", 1)

        expect resultA, containsMetric("AHF", NumericValue.ofRational(2, 3))
        expect resultB, containsMetric("AHF", 1)
        expect resultC, doesNotContainMetric("AHF")
        expect resultD, containsMetric("AHF", 1)
        expect resultI, containsMetric("AHF", 1)
        expect resultQ, containsMetric("AHF", 1)
    }
}
