package org.jasome.input

import spock.lang.Specification

import static org.jasome.util.TestUtil.projectFromSnippet

class ScannerSpec extends Specification {

    def "gracefully handles unparseable files"() {

        given:
        def project = projectFromSnippet '''
        package org.whatever.stuff;

        class ClassA {

        }
        ''', '''
        package org.whatever.stuff;

        cl4t 1  3ass q w4t A {

        }  r
        '''

        Package aPackage = (project.getPackages() as List<Package>).find { p -> p.name == "org.whatever.stuff" }

        when:
        Type classA = (aPackage.getTypes() as List<Type>).find { type -> type.name == "ClassA" }

        then:
        classA != null
    }

}
