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

}
