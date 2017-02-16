package org.jasome.metrics.calculators

import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.methodFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class NumberOfParametersCalculatorSpec extends Specification {
    NumberOfParametersCalculator unit

    def setup() {
        unit = new NumberOfParametersCalculator()
    }

    def "calculate simple metric"() {

        given:
        def type = methodFromSnippet '''
                public void method() {

                }
        '''

        when:
        def result = unit.calculate(type)

        then:
        expect result, containsMetric("NOP", 0)
    }

    def "calculate simple metric with parameters"() {

        given:
        def type = methodFromSnippet '''
                public void method(String one, int two, double three) {

                }
        '''

        when:
        def result = unit.calculate(type)

        then:
        expect result, containsMetric("NOP", 3)
    }

}
