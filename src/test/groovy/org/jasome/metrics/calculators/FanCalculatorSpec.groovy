package org.jasome.metrics.calculators

import org.jasome.input.Method
import org.jasome.input.Type
import spock.lang.Ignore
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class FanCalculatorSpec extends Specification {

    def "properly counts fan-out"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public void printDouble(ClassB b) {
                System.out.println(b.getNumber() * getFactor());            
            }
            
            public int getFactor() {
                return 2;
            }

        }
        
        class ClassB {
            private int myNumber;
            
            public ClassB(int myNumber) {
                this.myNumber = myNumber;
            }
        
            public int getNumber() {
                return myNumber;
            }
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classA = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassA" }

        Method printDouble = (classA.getMethods() as List<Method>).find { method -> method.name == "public void printDouble(ClassB b)" }

        when:
        def result = new FanCalculator().calculate(printDouble);

        then:

        expect result, containsMetric("FOut", 2)
        expect result, containsMetric("Si", 4)
    }

    @Ignore("Not yet implemented")
    def "properly counts fan-out in lambda"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {

            public void doStuff() {
                IntStream.range(0, 9).forEach(ClassB::printNumber);
            }
        
        }
        
        class ClassB {
            public static void printNumber(int i) {
                System.out.println(i);
            }
        }
        '''

        Type classA = project.locateType("ClassA")

        Method doStuff = (classA.getMethods() as List<Method>).find { method -> method.name == "public void doStuff()" }

        Type classB = project.locateType("ClassB")

        Method printNumber = (classB.getMethods() as List<Method>).find { method -> method.name == "public static void printNumber(int i)" }

        when:
        def doStuffResult = new FanCalculator().calculate(doStuff)
        def printNumberResult = new FanCalculator().calculate(printNumber)

        then:
        expect doStuffResult, containsMetric("FOut", 1)
        expect doStuffResult, containsMetric("Si", 2)
        expect printNumberResult, containsMetric("Fin", 1)
    }

    def "properly counts fan-in within a class"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public void printSquare(ClassB b) {
                System.out.println(b.getNumber() * b.getNumber());            
            }

        }
        
        class ClassB {
            private int myNumber;
            
            public ClassB(int myNumber) {
                this.myNumber = myNumber;
            }
        
            public int getNumber() {
                return myNumber;
            }
            
            public int getDoubleNumber() {
                return getNumber() + this.getNumber();
            }
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classB = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassB" }

        Method getDoubleNumber = (classB.getMethods() as List<Method>).find { method -> method.name == "public int getDoubleNumber()" }

        when:
        def result = new FanCalculator().calculate(getDoubleNumber);

        then:

        expect result, containsMetric("Fin", 0)
        expect result, containsMetric("Fout", 2)
        expect result, containsMetric("Si", 4)
    }

    def "properly counts fan-in outside of class"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public void printSquare(ClassB b) {
                System.out.println(b.getNumber() * b.getNumber());            
            }

        }
        
        class ClassB {
            private int myNumber;
            
            public ClassB(int myNumber) {
                this.myNumber = myNumber;
            }
        
            public int getNumber() {
                return myNumber;
            }
            
            public int getDoubleNumber() {
                return getNumber() + this.getNumber();
            }
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classB = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassB" }

        Method getNumber = (classB.getMethods() as List<Method>).find { method -> method.name == "public int getNumber()" }

        when:
        def result = new FanCalculator().calculate(getNumber);

        then:
        expect result, containsMetric("Fin", 4)
    }

    def "properly figures out correct types"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {
        
            public void print() {
                System.out.println("A");           
            }

        }
        
        class ClassB {
            public void print() {
                System.out.println("B");            
            }
        }
        
        class ClassC {
            private ClassA thingy, thingy2;
            
            public ClassC(ClassA thingy) {
                this.thingy = thingy;
            }
        
            public void doPrint(ClassB thingy) {
                ClassA otherThing = new ClassA();
                thingy.print();
                otherThing.print();
            }
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classC = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassC" }

        Method getNumber = (classC.getMethods() as List<Method>).find { method -> method.name == "public void doPrint(ClassB thingy)" }

        when:
        def result = new FanCalculator().calculate(getNumber);

        then:
        expect result, containsMetric("Fout", 2)
    }

    //TODO: tests for chained method calls
    //TODO: tests for method references in lambdas rather than direct calls
    //TODO: tests for class resolution on complex cross calls, lots of logic in the utils that aren't really tested here
    //TODO: check for toString() being called when using string concatenation?  is this doable?
}
