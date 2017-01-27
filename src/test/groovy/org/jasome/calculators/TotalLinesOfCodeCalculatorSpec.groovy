package org.jasome.calculators

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.apache.commons.io.IOUtils
import org.jasome.SomeClass
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
        SomeClass someClass = new SomeClass(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0));

        when:
        def result = unit.calculate(someClass)

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
        SomeClass someClass = new SomeClass(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0));

        when:
        def result = unit.calculate(someClass)

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
        SomeClass someClass = new SomeClass(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0));

        when:
        def result = unit.calculate(someClass)

        then:
        result.size() == 1
        result[0].value.intValue() == 2
    }

    def "returns an empty Optional if class declaration is missing"() {

        given:
        def sourceCode = ''''''

        CompilationUnit cu = JavaParser.parse(sourceCode);
        SomeClass someClass = new SomeClass(null);

        when:
        def result = unit.calculate(someClass)

        then:
        result.size() == 0
    }

    def "returns an empty Optional if parse is invalid"() {

        given:
        def sourceCode = '''class Example {}'''

        CompilationUnit cu = JavaParser.parse(sourceCode);
        ClassOrInterfaceDeclaration node = cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0)
        node.setRange(null)

        SomeClass someClass = new SomeClass(null);

        when:
        def result = unit.calculate(someClass)

        then:
        result.size() == 0
    }

    def "calculate counts for complex file"() {

        given:
        def stream = this.getClass().getResourceAsStream("/Hours.java")
        def sourceCode = IOUtils.toString(stream, "UTF-8");

        CompilationUnit cu = JavaParser.parse(sourceCode);
        SomeClass someClass = new SomeClass(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0));

        when:
        def result = unit.calculate(someClass)

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
        SomeClass someClass = new SomeClass(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0));

        when:
        def result = unit.calculate(someClass)

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
        SomeClass someClass = new SomeClass(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0));

        when:
        def result = unit.calculate(someClass)

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
        SomeClass someClass = new SomeClass(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0));

        when:
        def result = unit.calculate(someClass)

        then:
        result.size() == 1
        result[0].value.intValue() == 3
    }


}
