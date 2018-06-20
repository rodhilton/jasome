package org.jasome.metrics.calculators

import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.methodFromSnippet
import static org.jasome.util.TestUtil.packageFromSnippet
import static org.jasome.util.TestUtil.typeFromSnippet
import static org.jasome.util.TestUtil.typeFromStream
import static spock.util.matcher.HamcrestSupport.expect;


class TotalLinesOfCodeCalculatorSpec extends Specification {
    def "calculate simple metric"() {

        given:
        def type = typeFromSnippet('''
            class Example {
                public int stuff;
            }

        ''')

        when:
        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)

        then:
        type.parentPackage.name == "default"
        expect result, containsMetric("TLOC", 3)
    }

    def "calculate counts lines"() {

        given:
        def type = typeFromSnippet('''package org.whatever.stuff;

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
        ''')


        when:
        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)

        then:
        type.parentPackage.name == "org.whatever.stuff"
        expect result, containsMetric("TLOC", 9)
    }

    def "calculate class length when only two lines (open and close)"() {

        given:
        def type = typeFromSnippet('''
            interface Exampleable {}
        ''')

        when:
        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)

        then:
        expect result, containsMetric("TLOC", 2)
    }

    def "calculate counts for complex file"() {

        given:
        def stream = this.getClass().getResourceAsStream("/Hours.java")
        def type = typeFromStream(stream)

        when:
        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)

        then:
        expect result, containsMetric("TLOC", 159)
    }

    def "calculate counts lines in a synchronized block, but not for a synchronized variable or method"() {

        given:
        def type = typeFromSnippet('''package org.whatever.stuff;

            import lineone;
            import line2.stuff.junk;

            class Example {                      //1
                int x=5;                         //2

                synchronized void method() {     //3
                    System.out.println("test");  //4
                }                                //5

                void method2() {                 //6
                    synchronized(x) {            //7
                        System.out.println(x);   //8
                    }                            //9
                }                                //10
            }                                    //11
        ''')

        when:
        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)

        then:
        expect result, containsMetric("TLOC", 11)
    }

    def "calculate counts lines and can handle an empty declaration"() {

        given:
        def aPackage = packageFromSnippet('''package org.whatever.stuff;

            import lineone;
            import line2.stuff.junk;

            class Example {                          //1
                private interface MyJacksonView1 {}; //2-3
            }                                        //4
        ''')

        when:
        def type = aPackage.getTypes().find {t->t.getName() == "Example"}

        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)

        then:
        expect result, containsMetric("TLOC", 4)
    }

    def "calculate counts lines handles interfaces properly"() {

        given:
        def type = typeFromSnippet('''package org.apache.hc.client5.http.cookie;
            /**
             * Extension of {@link org.apache.hc.client5.http.cookie.CookieAttributeHandler} intended
             * to handle one specific common attribute whose name is returned with
             * {@link #getAttributeName()} method.
             *
             * @since 4.4
             */
            public interface CommonCookieAttributeHandler extends CookieAttributeHandler {

                String getAttributeName();

            }
        ''')

        when:
        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)

        then:
        expect result, containsMetric("TLOC", 3)
    }

    def "calculate counts try/catch/finally blocks properly"() {

        given:
        def type = typeFromSnippet '''package com.mkyong;

        import java.io.BufferedReader;
        import java.io.FileReader;
        import java.io.IOException;

        public class ReadFileExample1 {

            private static final String FILENAME = "E:\\\\test\\\\filename.txt";

            public static void main(String[] args) {

                BufferedReader br = null;
                FileReader fr = null;

                try {

                    fr = new FileReader(FILENAME);
                    br = new BufferedReader(fr);

                    String sCurrentLine;

                    br = new BufferedReader(new FileReader(FILENAME));

                    while ((sCurrentLine = br.readLine()) != null) {
                        System.out.println(sCurrentLine);
                    }

                } catch (IOException e) {

                    e.printStackTrace();

                } finally {

                    try {

                        if (br != null)     //This should count as 3 because it omits the braces which is a stylistic difference
                            br.close();

                        if (fr != null)     //same here
                            fr.close();

                    } catch (IOException ex) {

                        ex.printStackTrace();

                    }

                }

            }

        }
        '''

        when:
        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)

        then:
        expect result, containsMetric("TLOC", 29)
    }

    def "calculate counts complex if blocks properly"() {

        given:
        def type = typeFromSnippet '''package com.stuff;

        public class IfExample {

            public static void main(String[] args) {
                int x = (int)(Math.random() * 100);

                if(x > 79) {
                    System.out.println("higher than 79!");
                } else if(x < 4) {
                    System.out.println("smaller than 4");
                } else if(x > 50)
                    System.out.println("omitting braces!");
                else if(x < 20) {
                    System.out.println("whatever");
                } else {
                    System.out.println("hi");
                }

            }

        }
        '''

        //The above is actually syntactic sugar for the below, so we count the lines as below
        def equivalentType = typeFromSnippet '''package com.stuff;
        public class IfExample {

            public static void main(String[] args) {
                int x = (int)(Math.random() * 100);

                if(x > 79) {
                    System.out.println("higher than 79!");
                } else {
                    if(x < 4) {
                        System.out.println("smaller than 4");
                    } else {
                        if(x > 50) {
                            System.out.println("omitting braces!");
                        } else {
                            if(x < 20) {
                                System.out.println("whatever");
                            } else {
                                System.out.println("hi");
                            }
                        }
                    }
                }
            }
        }
        '''

        when:
        def result = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(type)
        def equivalentResult = new TotalLinesOfCodeCalculator.TypeCalculator().calculate(equivalentType)

        then:
        expect result, containsMetric("TLOC", 22)
        expect equivalentResult, containsMetric("TLOC", 22)

    }



    def "calculate counts for methods"() {

        given:
        def method = methodFromSnippet '''
            public void doStuff(String x, int y) {
                if(y > 10) {
                    System.out.println(x+" > 10!");
                } else {
                    System.out.println(x+" < 10 :(");
                }
            }
        '''

        when:
        def result = new TotalLinesOfCodeCalculator.MethodCalculator().calculate(method)

        then:
        expect result, containsMetric("TLOC", 7)
    }
}
