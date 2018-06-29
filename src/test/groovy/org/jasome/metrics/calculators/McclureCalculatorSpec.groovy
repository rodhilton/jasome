package org.jasome.metrics.calculators

import org.jasome.input.Method
import org.jasome.input.Type
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.projectFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class McclureCalculatorSpec extends Specification {

    def "properly calculates mcclure complexity"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class SomeClass {
        
            public void doWork(int stop) {
                double rand = Math.random();
                int i = 0;
                boolean shouldRun = rand < 0.5;
                if(shouldRun) {
                    do {
                        i++;                   
                    } while(i < stop);   
                } else if(rand < 0.9) {
                    System.out.println("rare!");
                }   
                
                
                int q = 0;
                while(q < stop) {
                    System.out.println(factorial(q++));
                }         
            }
            
            public static int factorial(int n) {
                int result = 1;
                for(int i = 2; i <= n; i++)
                    result *= i;
                return result;
            }

        }      
        '''

        org.jasome.input.Package aPackage = (project.getPackages() as List<Package>)[0]

        Type classA = project.locateType("SomeClass");

        Method doWork = classA.lookupMethodBySignature("doWork(int)").get()
        Method factorial = classA.lookupMethodBySignature("factorial(int)").get()

        when:
        def resultDoWork = new McclureCalculator().calculate(doWork);
        def resultFactorial = new McclureCalculator().calculate(factorial);

        then:

        expect resultDoWork, containsMetric("NCOMP", 4)
        expect resultFactorial, containsMetric("NCOMP", 1)

        expect resultDoWork, containsMetric("NVAR", 5)
        expect resultFactorial, containsMetric("NVAR", 2)

        expect resultDoWork, containsMetric("MCLC", 9)
        expect resultFactorial, containsMetric("MCLC", 3)

    }
}
