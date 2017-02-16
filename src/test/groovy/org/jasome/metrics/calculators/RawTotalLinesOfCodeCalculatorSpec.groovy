package org.jasome.metrics.calculators

import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.typeFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class RawTotalLinesOfCodeCalculatorSpec extends Specification {
    RawTotalLinesOfCodeCalculator unit

    def setup() {
        unit = new RawTotalLinesOfCodeCalculator()
    }

    def "calculate simple metric"() {

        given:
        def type = typeFromSnippet '''
            class Example {
                public int stuff;
            }

        '''

        when:
        def result = unit.calculate(type)

        then:
        expect result, containsMetric("RTLOC", 3)
    }

    def "calculate counts raw lines of code in a class including comments"() {

        given:
        def type = typeFromSnippet '''package org.whatever.stuff;

            import lineone;
            import line2.stuff.junk;

            class Example {                              //1
                                                         //2
                //This is a comment                      //3
                                                         //4
                public int test;                         //5
                                                         //6
                /*                                       //7
                    This is                              //8
                    also a comment                       //9
                */                                       //10
                                                         //11
                public int                               //12
                    test2;                               //13
                                                         //14
                public void aMethod() {                  //15
                    //More comments                      //16
                    for(int i=0;i<10;i++) {              //17
                        System.out.println("stuff");     //18
                    }                                    //19
                                                         //20
                }                                        //21
            }                                            //22
        '''

        when:
        def result = unit.calculate(type)

        then:
        expect result, containsMetric("RTLOC", 22)
    }

    def "calculate class length when only one line"() {

        given:
        def type = typeFromSnippet '''
            interface Exampleable {}
        '''

        when:
        def result = unit.calculate(type)

        then:
        expect result, containsMetric("RTLOC", 1)
    }

}
