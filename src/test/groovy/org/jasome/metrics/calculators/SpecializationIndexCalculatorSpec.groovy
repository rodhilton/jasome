package org.jasome.metrics.calculators

import org.jasome.input.Type
import org.jasome.metrics.value.NumericValue
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class SpecializationIndexCalculatorSpec extends Specification {

    def "calculate simple metric"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class A {

        }

        interface I {

        }

        class B implements I {

        }

        class C extends B {

        }

        class D extends A implements I {

        }
        '''

        Type typeA = project.locateType("A")
        Type typeC = project.locateType("C")
        Type typeD = project.locateType("D")

        when:
        def resultA = new SpecializationIndexCalculator().calculate(typeA);
        def resultC = new SpecializationIndexCalculator().calculate(typeC);
        def resultD = new SpecializationIndexCalculator().calculate(typeD);

        then:
        expect resultA, containsMetric("DIT", 1)
        expect resultC, containsMetric("DIT", 3)
        expect resultD, containsMetric("DIT", 2)
    }

    def "calculate depth and use maximum even if a minimum path to a root node is shorter"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class A {

        }

        interface I {

        }

        interface J extends I {

        }

        interface K extends J {

        }

        class ShouldBeFour extends A implements K {

        }

        class ShouldBeTwo extends A {

        }
        '''

        Type typeFour = project.locateType("ShouldBeFour")
        Type typeTwo = project.locateType("ShouldBeTwo")

        when:
        def resultFour = new SpecializationIndexCalculator().calculate(typeFour);
        def resultTwo = new SpecializationIndexCalculator().calculate(typeTwo);

        then:
        expect resultFour, containsMetric("DIT", 4)
        expect resultTwo, containsMetric("DIT", 2)
    }

    def "calculate depth uses the correct class when classes have same name"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        interface I {

        }

        interface J extends I {

        }

        interface K extends J {

        }
        ''','''
        package org.whatever.stuff2;

        interface K { //short

        }

        class A extends K {

        }
        '''

        Type typeA = project.locateType("org.whatever.stuff2.A")
        
        when:
        def result = new SpecializationIndexCalculator().calculate(typeA);

        then:
        expect result, containsMetric("DIT", 2)
    }

    def "calculate depth uses the correct class even if it's not the closest class when classes have same name"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        interface I {

        }

        interface J extends I {

        }

        interface K extends J {

        }
        ''','''
        package org.whatever.stuff2;

        interface K { //short

        }

        class A extends org.whatever.stuff.K {

        }
        '''

        Type typeA = project.locateType("org.whatever.stuff2.A")

        when:
        def result = new SpecializationIndexCalculator().calculate(typeA);

        then:
        expect result, containsMetric("DIT", 4)
    }



    //TODO: need to make sure static inner classes work, I think they currently won't

    def "calculate number of overridden methods"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        interface I {
            public void methodOne(int i);
        }

        abstract class A implements I {
            public void methodTwo(int i) {

            }
        }

        abstract class B extends A {
            public void methodThree(int i) {

            }
        }

        abstract class C extends B {
            public void methodFour(int i) {

            }

            public void methodSix(int i) {

            }
        }

        class D extends C {
            public void methodOne(int i) {

            }

            public void methodTwo(int i) {

            }

            public void methodFour(int i) {

            }

            public void methodFive(int i) {

            }
        }
        '''

        Type typeD = project.locateType("org.whatever.stuff.D")

        when:
        def result = new SpecializationIndexCalculator().calculate(typeD);

        then:
        expect result, containsMetric("NORM", 3)
        expect result, containsMetric("NM", 4)
        expect result, containsMetric("NMA", 1)
        expect result, containsMetric("NMI", 2)
    }

    def "calculate number of overridden methods works when the overriding method doesn't use the same parameter names"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        abstract class A {
            public abstract void methodOne(int i) {

            }
            public String methodTwo(int i) {

            }
        }

        class C extends A {
            public void methodOne(int i) {

            }

            public String methodTwo(int integer) {

            }
        }
        '''

        Type typeC = project.locateType("org.whatever.stuff.C")

        when:
        def result = new SpecializationIndexCalculator().calculate(typeC);

        then:
        expect result, containsMetric("NORM", 2)
        expect result, containsMetric("NM", 2)
    }

    def "does not double count methods in the inheritance hierarchy"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        abstract class A {
            public abstract void methodOne(int i) {

            }
        }

        class B extends A {
            public void methodOne(int i) {

            }
        }

        class C extends B {
            public void methodOne(int i) {

            }
        }
        '''

        Type typeC = project.locateType("org.whatever.stuff.C")

        when:
        def result = new SpecializationIndexCalculator().calculate(typeC);

        then:
        expect result, containsMetric("NORM", 1)
        expect result, containsMetric("NM", 1)
    }

    def "calculate specialization index"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        abstract class A {
            public abstract void methodOne(int i) {

            }
        }

        class B extends A {
            public void methodTwo(int i) {

            }
        }

        class C extends B {
            public void methodOne(int i) {

            }

            public void methodThree(int i) {

            }
        }
        '''

        //TODO do we need to worry about cycles
        Type typeC = project.locateType("org.whatever.stuff.C")

        when:
        def result = new SpecializationIndexCalculator().calculate(typeC);

        then:
        expect result, containsMetric("SIX", 1.5)
        expect result, containsMetric("NMA", 1)
        expect result, containsMetric("NMI", 1)
    }
}

