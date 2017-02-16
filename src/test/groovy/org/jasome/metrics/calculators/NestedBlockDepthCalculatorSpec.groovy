package org.jasome.metrics.calculators

import org.jasome.input.Type
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.TestUtil.methodFromSnippet
import static org.jasome.util.TestUtil.packageFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class NestedBlockDepthCalculatorSpec extends Specification {

    def "calculate simple metric when empty"() {

        given:
        def method = methodFromSnippet '''
            public void method() {               

            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 1)
    }


    def "calculate complex metric"() {

        given:
        def method = methodFromSnippet '''
            public void method() {
                for(int i=0;i<10;i++) {
                    if(i < 5) 
                        switch(i) {
                            case 0: 
                                System.out.println("it's nothing");
                                break;
                            default:
                                int x=i;
                                while(x>0) {
                                    System.out.println("it's not nothing");
                                    x--;
                                }
                                break;
                        } 
                        
                    if(i > 9) {
                        try {
                            System.out.println("Almost there");
                        } catch(Exception e) {
                            //shouldn't get here
                        }
                    }   
                }

            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 6)
    }

    def "calculate simple if metric"() {

        given:
        def method = methodFromSnippet '''
            public void method() {
                if(9 < 10) {
                    System.out.println("Duh");
                }

            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 2)
    }

    def "calculate simple try metric"() {

        given:
        def method = methodFromSnippet '''
            public void method() {
                if(9 < 10) {
                    try {
                        System.out.println("Almost there");
                    } catch(Exception e) {
                        //shouldn't get here
                    }
                }

            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 3)
    }

    def "calculate complex try"() {

        given:
        def method = methodFromSnippet '''
            public void method() {
                if(9 < 10) {
                    try {
                        System.out.println("Almost there");
                    } catch(Exception e) {
                        if(4 > 2) {
                            System.out.println("Wat");                        
                        }
                    }
                }

            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 4)
    }

    def "calculate complex metric again"() {

        given:
        def method = methodFromSnippet '''
            public void method() {
                for(int i=0;i<10;i++) {
                    if(i < 5) {
                         System.out.println("a");
                    } else {
                        if( i >6 ) {
                            synchronized(i) {
                                System.out.println("b");
                            }
                        } else {
                            System.out.println("c");
                        }
                    }   
                }

            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 5)
    }

    def "calculate blocks in lambdas"() {

        given:
        def method = methodFromSnippet '''
            public int triple(int x) {
                List<Integer> t = new ArrayList<Integer>();
                t.add(x);
                return t.stream().mapToInt(q -> {
                    return q * 3;
                }).findFirst().getAsInt();
            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 2)
    }

    def "calculate blocks in lambdas with no braces (so no nesting)"() {

        given:
        def method = methodFromSnippet '''
            public int triple(int x) {
                List<Integer> t = new ArrayList<Integer>();
                t.add(x);
                return t.stream().mapToInt(q -> q * 3).findFirst().getAsInt();
            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 1)
    }

    def "calculate blocks in deeply nested lambdas"() {

        given:
        def method = methodFromSnippet '''
            public int triple(int x) {
                List<Integer> t = new ArrayList<Integer>();
                t.add(x);
                return t.stream().mapToInt(q -> {
                    if( q > 2 ) {
                        return q * 3;   
                    } else {
                        if ( q > 4) {
                            return q * 4;
                        } else {
                            return q * 5;
                        }
                    }
                }).findFirst().getAsInt();
            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 4)
    }

    def "calculate handles differently formatted if else statements identically"() {

        given:
        def method = methodFromSnippet '''
            public int triple(int x) {
                List<Integer> t = new ArrayList<Integer>();
                t.add(x);
                return t.stream().mapToInt(q -> {
                    if( q > 2 ) {
                        return q * 3;   
                    } else if ( q > 4) {
                        return q * 4;
                    } else {
                        return q * 5;
                    }
                }).findFirst().getAsInt();
            }
        '''

        when:
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 4)
    }

    def "calculate anoynmous classes properly"() {

        given:
        def aPackage = packageFromSnippet '''
            package whatever.stuff;
            
            class Hello {
    
                public void sayHello() {
                    
                    class EnglishGreeting implements HelloWorld {
                        String name = "world";
                        public void greet() {
                            greetSomeone("world");
                        }
                        public void greetSomeone(String someone) {
                            name = someone;
                            System.out.println("Hello " + name);
                        }
                    }
                  
                    HelloWorld englishGreeting = new EnglishGreeting();
                    
                    HelloWorld frenchGreeting = new HelloWorld() {
                        String name = "tout le monde";
                        public void greet() {
                            greetSomeone("tout le monde");
                        }
                        public void greetSomeone(String someone) {
                            name = someone;
                            System.out.println("Salut " + name);
                        }
                    };
                    
                    HelloWorld spanishGreeting = new HelloWorld() {
                        String name = "mundo";
                        public void greet() {
                            greetSomeone("mundo");
                        }
                        public void greetSomeone(String someone) {
                            name = someone;
                            System.out.println("Hola, " + name);
                        }
                    };
                    englishGreeting.greet();
                    frenchGreeting.greetSomeone("Fred");
                    spanishGreeting.greet();
                }
                
            }
        '''

        when:
        Type type = aPackage.types.find{ t->t.name=="Hello"}
        def method = type.methods.find{m -> m.name == "public void sayHello()"}
        def result = new NestedBlockDepthCalculator().calculate(method)

        then:
        expect result, containsMetric("NBD", 3)
    }

}
