package org.jasome.util

import spock.lang.Specification

class DistinctTest extends Specification {
    void 'handles nulls'() {
        when:
        def d = Distinct.of(null);
        def e = Distinct.of(null);

        then:
        d == e


    }
}
