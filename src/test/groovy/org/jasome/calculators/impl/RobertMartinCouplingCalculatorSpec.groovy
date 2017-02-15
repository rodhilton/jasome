package org.jasome.calculators.impl

import org.jasome.parsing.Type
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
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
        expect firstResult, containsMetric("NOI", 1)
    }

    //TODO: assumes overlapping names are within same package if there is a match
    //TODO: test that notices that anything that's not public isn't counted in the case of overlapping names
}
