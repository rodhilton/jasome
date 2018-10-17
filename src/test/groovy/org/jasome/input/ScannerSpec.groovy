package org.jasome.input

import spock.lang.Specification

import static org.jasome.util.TestUtil.projectFromResources
import static org.jasome.util.TestUtil.projectFromSnippet

class ScannerSpec extends Specification {

    def "gracefully handles unparseable files"() {
        given:
        def project = projectFromResources("org/jasome/unparseable")

        when:
        Type stuffType = project.locateType("Stuff")
        Type fineType = project.locateType("Fine")

        then:
        stuffType == null
        fineType != null
    }

    def "correctly handles parseble files with UNIX path separators"() {
        given:
        def project = projectFromResources("org/jasome/resolver")

        when:
        Type aType = project.locateType("A")
        Type testType = project.locateType("Test")

        then:
        aType != null
        testType != null
    }

    def "correctly handles parseble files with Windows path separators"() {
        given:
        def project = projectFromResources("org\\jasome\\resolver")

        when:
        Type aType = project.locateType("A")
        Type testType = project.locateType("Test")

        then:
        aType != null
        testType != null
    }

}
