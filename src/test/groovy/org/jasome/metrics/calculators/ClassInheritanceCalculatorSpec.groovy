package org.jasome.metrics.calculators

import org.jasome.input.Type
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class ClassInheritanceCalculatorSpec extends Specification {

    def "correctly calculates number of children and parents"() {

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

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classX = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassX" }
        Type classY = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassY" }
        Type classA = (aPackage.getTypes() as List<Type>).find { type -> type.name == "A" }
        Type classK = (aPackage.getTypes() as List<Type>).find { type -> type.name == "K" }

        when:
        def resultX = new ClassInheritanceCalculator().calculate(classX);
        def resultY = new ClassInheritanceCalculator().calculate(classY);
        def resultA = new ClassInheritanceCalculator().calculate(classA);
        def resultK = new ClassInheritanceCalculator().calculate(classK);

        then:
        expect resultX, containsMetric("NOPa", 2)
        expect resultY, containsMetric("NOPa", 1)
        expect resultA, containsMetric("NOCh", 2)
        expect resultK, containsMetric("NOCh", 1)
    }

    def "correctly calculates number of descendants"() {

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

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classA = (aPackage.getTypes() as List<Type>).find { type -> type.name == "A" }
        Type classK = (aPackage.getTypes() as List<Type>).find { type -> type.name == "K" }
        Type classI = (aPackage.getTypes() as List<Type>).find { type -> type.name == "I" }

        when:
        def resultA = new ClassInheritanceCalculator().calculate(classA);
        def resultK = new ClassInheritanceCalculator().calculate(classK);
        def resultI = new ClassInheritanceCalculator().calculate(classI);

        then:
        expect resultA, containsMetric("NOD", 2)
        expect resultK, containsMetric("NOD", 1)

        expect resultI, containsMetric("NOD", 3)
    }

    def "correctly calculates number of ancestors"() {

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

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classX = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassX" }
        Type classY = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassY" }

        when:
        def resultX = new ClassInheritanceCalculator().calculate(classX);
        def resultY = new ClassInheritanceCalculator().calculate(classY);

        then:

        expect resultX, containsMetric("NOA", 4)
        expect resultY, containsMetric("NOA", 1)
    }

    def "properly resolves class name conflicts"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        import org.whatever.stuff.far.away.*;

        class ClassY implements I2 {

        }
        ''', '''
        package org.whatever.stuff.far.away;
        
           
        interface I2 {
        
        }
        ''', '''
        package org.whatever.stuff.close;
        
        interface I1 {
        
        }
        
        interface I2 extends I1 {
        
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>).find { type -> type.name == "org.whatever.stuff" }

        Type classY = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassY" }

        when:
        def resultY = new ClassInheritanceCalculator().calculate(classY);

        then:

        expect resultY, containsMetric("NOA", 1)
        expect resultY, containsMetric("NOPa", 1)
    }

    def "completely ignores classes it doesn't know about"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassY implements Serializable {

        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>).find { type -> type.name == "org.whatever.stuff" }

        Type classY = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassY" }

        when:
        def resultY = new ClassInheritanceCalculator().calculate(classY);

        then:

        expect resultY, containsMetric("NOA", 0)
        expect resultY, containsMetric("NOPa", 0)
    }


    def "properly handles inner classes"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class Outer {

            public static class Inner {
                public void sayHello() {
                    System.out.println("hi!");
                }
            }     
        }        
        ''', '''
        package org.whatever.stuff2;
        
        import org.whatever.stuff.Outer;
           
        class ClassX extends Outer.Inner {
        
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>).find { type -> type.name == "org.whatever.stuff2" }

        Type classY = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassX" }

        when:
        def resultY = new ClassInheritanceCalculator().calculate(classY);

        then:

        expect resultY, containsMetric("NOA", 1)
        expect resultY, containsMetric("NOPa", 1)
    }

    def "properly handles inner classes even when statically imported"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        import org.whatever.stuff.far.away.*; //ignore the import

        class Outer {

            public static class Inner {
                public void sayHello() {
                    System.out.println("hi!");
                }
            }     
        }        
        ''', '''
        package org.whatever.stuff2;
        
        import static org.whatever.stuff.Outer.*;
           
        class ClassX extends Inner {
        
        }
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>).find { type -> type.name == "org.whatever.stuff2" }

        Type classY = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassX" }

        when:
        def resultY = new ClassInheritanceCalculator().calculate(classY);

        then:

        expect resultY, containsMetric("NOA", 1)
        expect resultY, containsMetric("NOPa", 1)
    }

}
