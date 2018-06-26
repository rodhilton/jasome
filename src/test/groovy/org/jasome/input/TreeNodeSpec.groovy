package org.jasome.input

import spock.lang.Specification

import static org.jasome.util.TestUtil.projectFromSnippet

class TreeNodeSpec extends Specification {

    def "correctly computes equals and hashcodes for objects using their parents"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff1;

        class Thing1 {
            public String toString() {
                return "thing";
            }
        }
        ''','''
        package org.whatever.stuff2;

        class Thing2 {
            public String toString() {
                return "Thing 2";
            }
        }
        '''

        when:
        Set<Package> packages = project.getPackages()

        then:
        packages.size() == 2
        Package firstPackage = packages.find{p -> p.name == "org.whatever.stuff1"}
        Package secondPackage = packages.find{p -> p.name == "org.whatever.stuff2"}

        def firstType = (firstPackage.getTypes() as Type[])[0]
        firstType.name == "Thing1"
        def secondType = (secondPackage.getTypes() as Type[])[0]
        secondType.name == "Thing2"

        def firstMethod = (firstType.getMethods() as Method[])[0]
        firstMethod.name == "public String toString()"
        def secondMethod = (secondType.getMethods() as Method[])[0]
        secondMethod.name == "public String toString()"

        firstMethod != secondMethod //They have the same name and are both Methods, but they're not the same since they have different parents
        firstMethod.hashCode() != secondMethod.hashCode()

        firstMethod.parentType.name == "Thing1"
        secondMethod.parentType.name == "Thing2"

        firstType.parentPackage.name == "org.whatever.stuff1"
        secondType.parentPackage.name == "org.whatever.stuff2"

        firstPackage.parentProject == secondPackage.parentProject

        firstMethod.toString() contains "public String toString()"
        firstType.toString() contains "Thing1"
        firstPackage.toString() contains "org.whatever.stuff1"
    }

    def "correctly names classes and methods"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class Thing1 {
            public String toString() {
                return "thing";
            }

            public String toString(int param) {
                return "thing"+param;
            }

            public static class InnerClass {
                public String toString() {
                    return "Inner class baby";
                }
            }
        }
        '''

        when:
        Set<Package> packages = project.getPackages()

        then:
        packages.size() == 1
        Package firstPackage = packages.toArray()[0]

        firstPackage.getTypes().size() == 2;

        Type thing1 = firstPackage.getTypes().find{t->t.name == "Thing1"}
        Type innerClass = firstPackage.getTypes().find{t->t.name == "Thing1.InnerClass"}

        thing1 != null
        innerClass != null
    }
}
