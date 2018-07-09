package org.jasome.metrics.calculators

import org.jasome.input.Type
import org.jasome.metrics.value.NumericValue
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class CouplingFactorCalculatorTest extends Specification {

    def "properly calculates coupling factor"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public ClassB getB() {
                return new ClassB();           
            }

        }
        
        class ClassB {
            public ClassC getC() {
                return new ClassC();            
            }
        }
        
        class ClassC {
            public void print() {
                System.out.println("Hello");
            }
        }
        
        class MainClass {
            private ClassA classA;
        
            public MainClass(ClassA classA) {
                this.classA = classA;
            }
                            
            public void doPrint() {
                classA.getB().getC().print();
            }
        }
        '''


        Type classA = project.locateType("ClassA")
        Type classB = project.locateType("ClassB")
        Type classC = project.locateType("ClassC")
        Type mainClass = project.locateType("MainClass")

        when:
        def classAResult = new CouplingFactorCalculator().calculate(classA);
        def classBResult = new CouplingFactorCalculator().calculate(classB);
        def classCResult = new CouplingFactorCalculator().calculate(classC);
        def mainClassResult = new CouplingFactorCalculator().calculate(mainClass);

        then:
        expect classAResult, containsMetric("CF", NumericValue.ofRational(2,6))  //Uses ClassB, is used by MainClass
        expect classBResult, containsMetric("CF", NumericValue.ofRational(3,6))  //Uses ClassC, used by MainClass and A
        expect classCResult, containsMetric("CF", NumericValue.ofRational(2,6))  //Uses nothing, used by MainClass and b
        expect mainClassResult, containsMetric("CF", NumericValue.ofRational(3,6)) //Uses all, used by nothing
    }

    def "properly accounts for parameters"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public void print() {
                System.out.println("Hello");
            }

        }
        
        class ClassB {
            public void doPrint(ClassA classA) {
                classA.print();
            }
        }
        
        
        class MainClass {
            private ClassA classA;
        
            public MainClass(ClassA classA) {
                this.classA = classA;
            }
                            
            public void doPrint() {
                new ClassB().doPrint(classA);
            }
        }
        '''


        Type classA = project.locateType("ClassA")
        Type classB = project.locateType("ClassB")
        Type mainClass = project.locateType("MainClass")

        when:
        def classAResult = new CouplingFactorCalculator().calculate(classA);
        def classBResult = new CouplingFactorCalculator().calculate(classB);
        def mainClassResult = new CouplingFactorCalculator().calculate(mainClass);

        then:
        expect classAResult, containsMetric("CF", NumericValue.ofRational(2,4))
        expect classBResult, containsMetric("CF", NumericValue.ofRational(2,4))
        expect mainClassResult, containsMetric("CF", NumericValue.ofRational(2,4))
    }

    def "correctly calculates coupling coefficient with inheritance"() {

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

        class ClassX extends A implements K {

        }

        class ClassY extends A {

        }
        '''
        
        Type classA = project.locateType("A")
        Type interfaceI = project.locateType("I")
        Type interfaceJ = project.locateType("J")
        Type interfaceK = project.locateType("K")
        Type classX = project.locateType("ClassX")
        Type classY = project.locateType("ClassY")

        when:
        def resultA = new CouplingFactorCalculator().calculate(classA);
        def resultI = new CouplingFactorCalculator().calculate(interfaceI);
        def resultJ = new CouplingFactorCalculator().calculate(interfaceJ);
        def resultK = new CouplingFactorCalculator().calculate(interfaceK);
        def resultX = new CouplingFactorCalculator().calculate(classX);
        def resultY = new CouplingFactorCalculator().calculate(classY);

        then:
        expect resultA, containsMetric("CF", NumericValue.ofRational(2,10)) //Uses none, used by X and Y
        expect resultI, containsMetric("CF", NumericValue.ofRational(1,10)) //Uses none, used by J
        expect resultJ, containsMetric("CF", NumericValue.ofRational(2,10)) //Uses I, used by K
        expect resultK, containsMetric("CF", NumericValue.ofRational(2,10)) //Uses J, used by X
        expect resultX, containsMetric("CF", NumericValue.ofRational(2,10)) //Uses A and K, used by none
        expect resultY, containsMetric("CF", NumericValue.ofRational(1,10)) //Uses A, used by none
    }
}
