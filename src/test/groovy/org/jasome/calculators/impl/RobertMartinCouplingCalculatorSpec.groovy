package org.jasome.calculators.impl

import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.Matchers.doesNotContainMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class RobertMartinCouplingCalculatorSpec extends Specification {

    def "calculate simple coupling metrics"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;
        
        import org.whatever.stuff2.*;

        public class A {
            public void wee() {
                System.out.println(D.utilMethod(10));
            }
        }
        ''','''
        package org.whatever.stuff;
        
        import org.whatever.stuff2.*;
        
        public class B {
            public <T extends C> void blah(T stuff) {
                System.out.println(stuff);
            }
        }
        
        ''','''
        package org.whatever.stuff;
        
        import org.whatever.stuff2.*;
        
        public interface I {
            
        }
        
        ''','''
        package org.whatever.stuff;
              
        public class CustomException extends Exception {
        
        }        
        ''','''
        package org.whatever.stuff2;
        
        import org.whatever.stuff.*;

        public class C {
            private A field;
        
            public A returnsA() {
                return new A();
            }
        }
        ''','''
        package org.whatever.stuff2;

        import org.whatever.stuff.*;
        
        abstract public class D {
       
            static {
                System.out.println("Some stuff "+new CustomException());
            } 
       
            public void usesB(B in) throws CustomException {
                System.out.println(in);
            }
            
            public static int utilMethod(int i) {
                return i*2;
            }
        }
        ''','''
        package org.whatever.stuff2;

        import org.whatever.stuff.*;
        
        public class E {
            public void usesD() {
                D d = new D();
                System.out.println(d);
            }
        }
        '''

        org.jasome.parsing.Package firstPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff"}
        org.jasome.parsing.Package secondPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff2"}


        when:
        def firstResult = new RobertMartinCouplingCalculator().calculate(firstPackage);
        def secondResult = new RobertMartinCouplingCalculator().calculate(secondPackage);

        then:
        expect firstResult, containsMetric("Ce", 2)
        expect firstResult, containsMetric("Ca", 2)

        expect secondResult, containsMetric("Ce", 2)
        expect secondResult, containsMetric("Ca", 2)

        expect firstResult, containsMetric("I", new BigDecimal(0.500000))
        expect firstResult, containsMetric("A", new BigDecimal(0.250000))
        expect firstResult, containsMetric("DMS", new BigDecimal(0.250000))
        expect firstResult, containsMetric("NOI", BigDecimal.ONE)
    }


    def "calculate assumes contradictory names are used within the package"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;
        
        import org.whatever.stuff2.*;
        
        public class A {
            public void helloA() {
                System.out.println("Hello A");
            }
        }
        
        ''','''
        package org.whatever.stuff2;
        
        import org.whatever.stuff.*;

        public class A {
            public void otherHelloA() {
                System.out.println("Hello A 2");
            }
        }
        ''','''
        package org.whatever.stuff2;

        import org.whatever.stuff.*;
        
        public class B {
            public static int utilMethod(A a) {
                return 100;
            }
        }
        '''

        org.jasome.parsing.Package firstPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff"}
        org.jasome.parsing.Package secondPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff2"}


        when:
        def firstResult = new RobertMartinCouplingCalculator().calculate(firstPackage);
        def secondResult = new RobertMartinCouplingCalculator().calculate(secondPackage);

        then:
        expect firstResult, containsMetric("Ce", BigDecimal.ZERO)
        expect firstResult, containsMetric("Ca", BigDecimal.ZERO)

        expect secondResult, containsMetric("Ce", BigDecimal.ZERO)
        expect secondResult, containsMetric("Ca", BigDecimal.ZERO)

        expect firstResult, doesNotContainMetric("I")
        expect secondResult, containsMetric("Ca", new BigDecimal("0.000000"))
        expect firstResult, doesNotContainMetric("DMS")
        expect firstResult, containsMetric("NOI", BigDecimal.ZERO)
    }

    def "doesn't count non-public classes"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;
        
        import org.whatever.stuff2.*;
        
        public class A {
            public void helloA() {
                System.out.println("Hello A");
            }
        }
        
        class B {
            public void helloB() {
                System.out.println("Hello B");
            }
        }        
        ''','''
        package org.whatever.stuff2;
        
        import org.whatever.stuff.*;

        public class C {
            public void sayHello() {
                new B().helloB();
            }
        }
        '''

        org.jasome.parsing.Package firstPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff"}
        org.jasome.parsing.Package secondPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff2"}


        when:
        def firstResult = new RobertMartinCouplingCalculator().calculate(firstPackage);
        def secondResult = new RobertMartinCouplingCalculator().calculate(secondPackage);

        then:
        expect firstResult, containsMetric("Ce", BigDecimal.ZERO)
        expect firstResult, containsMetric("Ca", BigDecimal.ZERO)

        expect secondResult, containsMetric("Ce", BigDecimal.ZERO)
        expect secondResult, containsMetric("Ca", BigDecimal.ZERO)

        expect firstResult, doesNotContainMetric("I")
        expect secondResult, containsMetric("Ca", new BigDecimal("0.000000"))
        expect firstResult, doesNotContainMetric("DMS")
        expect firstResult, containsMetric("NOI", BigDecimal.ZERO)
    }

    def "able to see public static classes"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;
        
        import org.whatever.stuff2.*;
        
        public class A {
            public void helloA() {
                System.out.println("Hello A");
            }
            
            public static class Inner {
                public static void sayHello() {
                    System.out.println("hi");
                }
            }
        }            
        ''','''
        package org.whatever.stuff2;
        
        import org.whatever.stuff.*;

        public class B {
            public void sayHello() {
                A.Inner.sayHello();
            }
        }
        '''

        org.jasome.parsing.Package firstPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff"}
        org.jasome.parsing.Package secondPackage = (project.getPackages() as List<Package>).find{p -> p.name=="org.whatever.stuff2"}


        when:
        def firstResult = new RobertMartinCouplingCalculator().calculate(firstPackage);
        def secondResult = new RobertMartinCouplingCalculator().calculate(secondPackage);

        then:
        expect firstResult, containsMetric("Ce", BigDecimal.ZERO)
        expect firstResult, containsMetric("Ca", BigDecimal.ONE)

        expect secondResult, containsMetric("Ce", BigDecimal.ONE)
        expect secondResult, containsMetric("Ca", BigDecimal.ZERO)
    }
}
