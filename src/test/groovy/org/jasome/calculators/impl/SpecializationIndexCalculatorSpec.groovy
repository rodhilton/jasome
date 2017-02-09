package org.jasome.calculators.impl

import org.jasome.parsing.Type
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.packageFromSnippet
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

        org.jasome.parsing.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type typeA = (aPackage.getTypes() as List<Type>).find{ type -> type.name == "A"}
        Type typeC = (aPackage.getTypes() as List<Type>).find{ type -> type.name == "C"}
        Type typeD = (aPackage.getTypes() as List<Type>).find{ type -> type.name == "D"}

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

        org.jasome.parsing.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type typeFour = (aPackage.getTypes() as List<Type>).find{ type -> type.name == "ShouldBeFour"}
        Type typeTwo = (aPackage.getTypes() as List<Type>).find{ type -> type.name == "ShouldBeTwo"}

        when:
        def resultFour = new SpecializationIndexCalculator().calculate(typeFour);
        def resultTwo = new SpecializationIndexCalculator().calculate(typeTwo);

        then:
        expect resultFour, containsMetric("DIT", 4)
        expect resultTwo, containsMetric("DIT", 2)
    }

    def "calculate depth uses the maximum possible path when there are classes with the same name and it's not clear which is used"() {

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
        //It "should" be obvious to use K since its the same package, but without resolving imports we can't know
        //So we want to assume the longest possible inheritance tree for duplicated names

        }
        '''

        org.jasome.parsing.Package firstPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff"}
        org.jasome.parsing.Package secondPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff2"}

        Type typeA = (secondPackage.getTypes() as List<Type>).find{ type -> type.name == "A"}

        when:
        def result = new SpecializationIndexCalculator().calculate(typeA);

        then:
        expect result, containsMetric("DIT", 4)
    }



    //TODO: need to make sure static inner classes work, I think they currently won't

}

