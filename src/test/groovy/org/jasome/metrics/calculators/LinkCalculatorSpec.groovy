package org.jasome.metrics.calculators

import org.jasome.input.Method
import org.jasome.input.Type
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect
import static spock.util.matcher.HamcrestSupport.expect
import static spock.util.matcher.HamcrestSupport.expect
import static spock.util.matcher.HamcrestSupport.expect

class LinkCalculatorSpec extends Specification {

    def "properly calculates link count"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        interface InterfaceI {
        
        }
        
        class ClassA {
        
            public ClassB getB() {
                return new ClassB();           
            }

        }
        
        class ClassB implements InterfaceI {
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


        Type mainClass = project.locateType("MainClass")
        Type classA = project.locateType("ClassA")
        Type classB = project.locateType("ClassB")
        Type classC = project.locateType("ClassC")

        when:
        def mainClassResult = new LinkCalculator().calculate(mainClass);
        def classAResult = new LinkCalculator().calculate(classA);
        def classBResult = new LinkCalculator().calculate(classB);
        def classCResult = new LinkCalculator().calculate(classC);

        then:
        expect classAResult, containsMetric("NOL", 1)
        expect classBResult, containsMetric("NOL", 2)
        expect classCResult, containsMetric("NOL", 0)
        expect mainClassResult, containsMetric("NOL", 3)
    }


    def "properly calculates link count in default package"() {

        given:
        def project = projectFromSnippet '''
        interface InterfaceI {
        
        }
        
        class ClassA {
        
            public ClassB getB() {
                return new ClassB();           
            }

        }
        
        class ClassB implements InterfaceI {
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


        Type mainClass = project.locateType("MainClass")
        Type classA = project.locateType("ClassA")
        Type classB = project.locateType("ClassB")
        Type classC = project.locateType("ClassC")

        when:
        def mainClassResult = new LinkCalculator().calculate(mainClass);
        def classAResult = new LinkCalculator().calculate(classA);
        def classBResult = new LinkCalculator().calculate(classB);
        def classCResult = new LinkCalculator().calculate(classC);

        then:
        expect classAResult, containsMetric("NOL", 1)
        expect classBResult, containsMetric("NOL", 2)
        expect classCResult, containsMetric("NOL", 0)
        expect mainClassResult, containsMetric("NOL", 3)
    }

    //TODO: ensure empty package names still work
}
