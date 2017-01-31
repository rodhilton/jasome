package org.jasome.calculators

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.apache.commons.io.IOUtils
import org.jasome.calculators.impl.TotalLinesOfCodeCalculator
import spock.lang.Specification

class TotalLinesOfCodeCalculatorSpec extends Specification {
    TotalLinesOfCodeCalculator unit

    def setup() {
        unit = new TotalLinesOfCodeCalculator()
    }

    def "calculate simple metric"() {

        given:
        def sourceCode = '''
            class Example {
                public int stuff;
            }

        '''

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 3
    }

    def "calculate counts lines"() {

        given:
        def sourceCode = '''package org.whatever.stuff;

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

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 9
    }

    def "calculate class length when only two lines (open and close)"() {

        given:
        def sourceCode = '''
            interface Exampleable {}
        '''

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 2
    }

    def "returns an empty if class declaration is missing"() {

        given:
        def sourceCode = ''''''

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(null, SourceContext.NONE)

        then:
        thrown AssertionError
    }

    def "calculate counts for complex file"() {

        given:
        def stream = this.getClass().getResourceAsStream("/Hours.java")
        def sourceCode = IOUtils.toString(stream, "UTF-8");

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 159
    }

    def "calculate counts lines in a synchronized block, but not for a synchronized variable or method"() {

        given:
        def sourceCode = '''package org.whatever.stuff;

            import lineone;
            import line2.stuff.junk;

            class Example {                      //1
                synchronized int x=5;            //2

                synchronized void method() {     //3
                    System.out.println("test");  //4
                }                                //5

                void method2() {                 //6
                    synchronized(x) {            //7
                        System.out.println(x);   //8
                    }                            //9
                }                                //10
            }                                    //11
        '''

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 11
    }

    def "calculate counts lines and can handle an empty declaration"() {

        given:
        def sourceCode = '''package org.whatever.stuff;

            import lineone;
            import line2.stuff.junk;

            class Example {                          //1
                private interface MyJacksonView1 {}; //2-3
            }                                        //4
        '''

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 4
    }

    def "calculate counts lines handles interfaces properly"() {

        given:
        def sourceCode = '''package org.apache.hc.client5.http.cookie;
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
        '''

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 3
    }

    def "calculate counts try/catch/finally blocks properly"() {

        given:
        def sourceCode = '''package com.mkyong;

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

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 29
    }

    def "calculate counts complex if blocks properly"() {

        given:
        def sourceCode = '''package com.stuff;

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
        def equivalentSourceCode = '''package com.stuff;
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

        CompilationUnit cu = JavaParser.parse(sourceCode);
        CompilationUnit equivalentCu = JavaParser.parse(equivalentSourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)
        def equivalentResult = unit.calculate(equivalentCu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result[0].value.intValue() == 22

        equivalentResult.size() == 1
        equivalentResult[0].value.intValue() == 22
    }


}
