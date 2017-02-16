package org.jasome.metrics.calculators

import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.methodFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class CyclomaticComplexityCalculatorSpec extends Specification {

    def "calculate simple metric"() {

        given:
        def type = methodFromSnippet '''
            public void method(int x) {
                if(x < 10) {
                    if(x > 3) {
                        System.out.println("between 3 and 10");
                    }

                    if(x > 5) {
                        System.out.println("between 5 and 10");
                    } else {
                        System.out.println("less than 5");
                    }
                }

                if(x > 100) {
                    if(x < 50) {
                      System.out.println("between 50 and 100");
                    }
                }
            }
        '''

        when:
        def result = new CyclomaticComplexityCalculator().calculate(type)

        then:
        expect result, containsMetric("VG", 6)
    }

    def "calculate includes for statments"() {

        given:
        def type = methodFromSnippet '''
            public void method(int x) {
                if(x < 10) {
                    for(int i=0;i<x;i++) {
                        System.out.println("Hello!");
                    }
                }
            }
        '''

        when:
        def result = new CyclomaticComplexityCalculator().calculate(type)

        then:
        expect result, containsMetric("VG", 3)
    }

    def "calculate uses case statements but doesn't count switch or default"() {

        given:
        def type = methodFromSnippet '''
            public static Hours hours(int hours) {
                switch (hours) {
                    case 0:
                        return ZERO;
                    case 1:
                        return ONE;
                    case 2:
                        return TWO;
                    case 3:
                        return THREE;
                    case 4:
                        return FOUR;
                    case 5:
                        return FIVE;
                    case 6:
                        return SIX;
                    case 7:
                        return SEVEN;
                    case 8:
                        return EIGHT;
                    case Integer.MAX_VALUE:
                        return MAX_VALUE;
                    case Integer.MIN_VALUE:
                        return MIN_VALUE;
                    default:
                        return new Hours(hours);
                }
            }
        '''

        when:
        def result = new CyclomaticComplexityCalculator().calculate(type)

        then:
        expect result, containsMetric("VG", 12)
    }


    def "calculate takes && and || into account"() {

        given:
        def type = methodFromSnippet '''
            public void method(int x) {
                if(x > 3 && x < 10) {
                    System.out.println("between 3 and 10");
                }

                if(x > 100 || x < 0 && x != 0) {
                    System.out.println("it's either huge or small, and it's not zero");
                }
            }
        '''

        when:
        def result = new CyclomaticComplexityCalculator().calculate(type)

        then:
        expect result, containsMetric("VG", 6)
    }

    def "calculate looks at ternary"() {

        given:
        def type = methodFromSnippet '''
            public void method(int x) {
                int y = (x < 10 && x > 3 || x == 0) ? 10 : 50;
            }
        '''

        when:
        def result = new CyclomaticComplexityCalculator().calculate(type)

        then:
        expect result, containsMetric("VG", 4)
    }

    def "calculate for loops"() {

        given:
        def type = methodFromSnippet '''
            public void method(int x) {
                int y = x;
                while(y > 0) {
                    System.out.println("wee");
                    y--;
                }

                do {
                    System.out.println("woo");
                    y++;
                } while(y < x);
            }
        '''

        when:
        def result = new CyclomaticComplexityCalculator().calculate(type)

        then:
        expect result, containsMetric("VG", 3)
    }
}
