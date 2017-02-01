package org.jasome.calculators

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.apache.commons.io.IOUtils
import org.jasome.calculators.impl.NumberOfFieldsCalculator
import org.jasome.calculators.impl.TotalLinesOfCodeCalculator
import spock.lang.Specification

class NumberOfFieldsCalculatorSpec extends Specification {
    NumberOfFieldsCalculator unit

    def setup() {
        unit = new NumberOfFieldsCalculator()
    }

    def "calculate simple metric"() {

        given:
        def sourceCode = '''
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

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result["NF"].value == 3
        result["NSF"].value == 1
        result["NPF"].value == 2
        result["NM"].value == 3
        result["NSM"].value == 1
        result["NPM"].value == 1
    }

    def "calculate simple metric with nested class"() {

        given:
        def sourceCode = '''
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

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result["NF"].value == 3
        result["NSF"].value == 1
        result["NPF"].value == 2
        result["NM"].value == 3
        result["NSM"].value == 1
        result["NPM"].value == 1
    }

    def "calculate simple metric with anonymous class"() {

        given:
        def sourceCode = '''
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

        CompilationUnit cu = JavaParser.parse(sourceCode);

        when:
        def result = unit.calculate(cu.getNodesByType(ClassOrInterfaceDeclaration.class).get(0), SourceContext.NONE)

        then:
        result["NF"].value == 3
        result["NSF"].value == 1
        result["NPF"].value == 2
        result["NM"].value == 3
        result["NSM"].value == 1
        result["NPM"].value == 1
    }

}
