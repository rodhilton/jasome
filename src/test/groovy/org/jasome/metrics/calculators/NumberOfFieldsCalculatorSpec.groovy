package org.jasome.metrics.calculators

import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.packageFromSnippet
import static org.jasome.util.TestUtil.typeFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class NumberOfFieldsCalculatorSpec extends Specification {
    NumberOfFieldsCalculator unit

    def setup() {
        unit = new NumberOfFieldsCalculator()
    }

    def "calculate simple metric"() {

        given:
        def type = typeFromSnippet '''
            class Example {
                public int stuff;
                public static int stuff2;
                private long stuff3;

                public void method() {

                }

                private void method2() {

                }

                static void method3() {

                }
            }
        '''

        when:
        def result = unit.calculate(type)

        then:
        expect result, containsMetric("NF", 3)
        expect result, containsMetric("NSF", 1)
        expect result, containsMetric("NPF", 2)
        expect result, containsMetric("NM", 3)
        expect result, containsMetric("NSM", 1)
        expect result, containsMetric("NPM", 1)
    }

    def "calculate simple metric with nested class"() {

        given:
        def aPackage = packageFromSnippet '''
            class Example {
                public int stuff;
                public static int stuff2;
                private long stuff3;

                public void method() {

                }

                private void method2() {

                }

                static void method3() {

                }

                public static class Inner {
                    public int inner;
                    private static long innerlong;

                    public void innerMethod() {

                    }
                }
            }
        '''

        when:
        def type = aPackage.types.find{t->t.name == "Example"}
        def result = unit.calculate(type)

        then:
        expect result, containsMetric("NF", 3)
        expect result, containsMetric("NSF", 1)
        expect result, containsMetric("NPF", 2)
        expect result, containsMetric("NM", 3)
        expect result, containsMetric("NSM", 1)
        expect result, containsMetric("NPM", 1)
    }

    def "calculate simple metric with anonymous class"() {

        given:
        def type = typeFromSnippet '''
            class Example {
                public int stuff;
                public static int stuff2;
                private long stuff3;

                public void method() {

                }

                private void method2() {

                }

                static void method3() {
                    Button btn = new Button();
                    btn.setOnAction(new EventHandler<ActionEvent>() {

                        public int stuffAndJunk;

                        @Override
                        public void handle(ActionEvent event) {
                            System.out.println("Hello World!");
                        }
                    });
                }
            }
        '''

        when:
        def result = unit.calculate(type)

        then:
        expect result, containsMetric("NF", 3)
        expect result, containsMetric("NSF", 1)
        expect result, containsMetric("NPF", 2)
        expect result, containsMetric("NM", 3)
        expect result, containsMetric("NSM", 1)
        expect result, containsMetric("NPM", 1)
    }

}
