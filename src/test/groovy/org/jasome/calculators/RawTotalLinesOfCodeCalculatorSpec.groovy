package org.jasome.calculators

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.jasome.calculators.impl.RawTotalLinesOfCodeCalculator
import spock.lang.Specification

class RawTotalLinesOfCodeCalculatorSpec extends Specification {
    RawTotalLinesOfCodeCalculator unit

    def setup() {
        unit = new RawTotalLinesOfCodeCalculator()
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
        result["RTLOC"].value == 3
    }

    def "calculate counts raw lines of code in a class including comments"() {

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
        result["RTLOC"].value == 22
    }

    def "calculate class length when only one line"() {

        given:
        def sourceCode = '''
            interface Exampleable {}
        '''

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result.size() == 1
        result["RTLOC"].value == 1
    }

    def "throws error if class declaration is missing"() {

        given:
        def sourceCode = ''''''

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(null, SourceContext.NONE)

        then:
        thrown AssertionError
    }


}
