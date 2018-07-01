package org.jasome.metrics.calculators

import org.jasome.metrics.value.NumericValue
import spock.lang.Specification

import static org.jasome.util.Matchers.containsMetric
import static org.jasome.util.Matchers.doesNotContainMetric
import static org.jasome.util.TestUtil.typeFromSnippet
import static spock.util.matcher.HamcrestSupport.expect

class LackOfCohesionMethodsCalculatorSpec extends Specification {

    def "calculate simple LCOM"() {

        given:
        def type = typeFromSnippet '''
            public class Rectangle {
              private double width;
              private double height;
            
              public Rectangle(double width, double height) {
                super();
                this.width = width;
                this.height = height;
              }
            
              public double getArea() {
                return width * height;
              }
            
              public double getPerimeter() {
                return this.width * 2 + this.height * 2;
              }
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 0)
    }

    def "calculate simple LCOM for non-perfect example"() {

        given:
        def type = typeFromSnippet '''
            public class Circle {
              private double x, y;
              private double radius;
            
              public Circle(double x, double y, double radius) {
                this.x = x;
                this.y = y;
                this.radius = radius;
              }
            
              public double getArea() {
                return Math.PI * this.radius * this.radius;
              }
            
              public boolean contains(double x, double y) {
                double distance = Math.sqrt(
                      (x - this.x) * (x - this.x) +
                      (y - this.y) * (y - this.y));
                return distance <= this.radius;
              }
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", NumericValue.of(1).divide(NumericValue.of(3)));
    }

    def "calculate simple LCOM sees variable use in for loops"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;          
            
              public void printAll() {
                for(int i=0;i<x;i++) {
                    System.out.println(i);
                }
              }
              
              public void printAllAgain() {
                for(int i=0;i<x;i++) {
                    System.out.println("yes"+i);
                }
              }            
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 0)
    }

    def "LCOM is undefined when only one method and one field"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;          
            
              public void printAll() {
                System.out.println(x);
              }
                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, doesNotContainMetric("LCOM*")
    }

    def "detects when usages have been shadowed by identically-named variables"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;          
            
              public void printAll() {
                int x = 0;
                System.out.println(x);
              }
              
              public void doNothing() {
              
              }
                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 2)
    }

    def "detects when usages have been shadowed by identically-named variables outside of the scope in which they are used"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;          
            
              public void printAll() {
                if(9 < 10) {
                  int x = 0;
                  if(10 < 100) {
                    System.out.println(x);    
                  }
                }
              }
              
              public void doNothing() {
              
              }
                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 2)
    }

    def "simple baseline metric for straight variable access"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;
              private double y;          
            
              public void printX() {
                //System.out.println(x);
              }
              
              public void printY() {
                System.out.println(y);
              }                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 1.5)
    }


    def "simple baseline metric for low cohesion"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;
              private double y;          
            
              public void printX() {
                System.out.println(x);
              }
              
              public void printY() {
                System.out.println(y);
              }                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 1)
    }

    def "detects shadowing in for loop initializer"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;
              private double y;          
            
              public void printX() {
                for(int i=0, x=8;i<10;i++) {
                    System.out.println(x);
                }
              }
              
              public void printY() {
                System.out.println(y);
              }                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 1.5)
    }


    def "baseline for field access when shadowed in lower scope"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;          
            
              public void printAll() {
                if( this.x < 10.0) {
                    int x=0;
                    System.out.println(x);
                }
              }
              
              public void doNothing() {
              
              }                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 1)
    }


    def "detects when usages have been shadowed by identically-named variables but still counts the method if it has a genuine access"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;          
            
              public void printAll() {
                if( x < 10.0) {
                    int x=0;
                    System.out.println(x);
                }
              }
              
              public void doNothing() {
              
              }                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 1)
    }

    def "detects when usages have been shadowed by identically-named variables but still counts the method if it has a genuine access even if shadowed declaration is in different scope from use"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;          
            
              public void printAll() {
                if( x < 10.0) {
                    int x=0;
                    if(9 < 10) {
                       System.out.println(x);
                    }
                }
              }
              
              public void doNothing() {
              
              }                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 1)
    }

    def "detects when usages have been shadowed in else statement"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;          
            
              public void printAll() {
                if( 5 < 10.0) {
                    if(9 < 10) {
                       System.out.println(x);
                    } else {
                        int x=0;
                        System.out.println(x);
                    }                    
                }
              }
              
              public void doNothing() {
              
              }                         
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 1)
    }

    def "calculate LCOM counts assignments as usages"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;
              private double y;          
            
              public void printAll() {
                System.out.println(x+" "+y);
              }
              
              public void setStuff() {
                x=Math.random();
                this.y=Math.random();
              }            
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 0)
    }

    def "detects variable shadowing via method parameters"() {

        given:
        def type = typeFromSnippet '''
            public class Class {
              private double x;
              private double y;          
            
              public void printX(int x) {
                System.out.println(x);
              }
              
              public void printY(int y) {
                System.out.println(y);
              }            
            
            }
        '''

        when:
        def result = new LackOfCohesionMethodsCalculator().calculate(type)

        then:
        expect result, containsMetric("LCOM*", 2)
    }
    
    //TODO: don't factor in interface or abstract methods? should they not even count in the total?
    //TODO: one method, one variable - this is high cohesion right? so why is it 0/0 which is NaN or 1?
}
