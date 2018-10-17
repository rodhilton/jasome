package org.jasome.util

import spock.lang.Specification

class PathUtilsTest extends Specification {

    void 'a unix path should stay the same if converted to a unix path'() {
        given:
        def path = "/a/b/c"

        when:
        def result = PathUtils.toUnixPath(path)

        then:
        result == path
    }

    void 'a mixed path should be correctly converted to a unix path'() {
        given:
        def path = "/a\\b/c"

        when:
        def result = PathUtils.toUnixPath(path)

        then:
        result == "/a/b/c"
    }

    void 'a windows path should be correctly converted to a unix path'() {
        given:
        def path = "\\a\\b\\c"

        when:
        def result = PathUtils.toUnixPath(path)

        then:
        result == "/a/b/c"
    }

    void 'a windows path should be correctly converted to a system path'() {
        given:
        def path = "\\a\\b\\c"

        when:
        def result = PathUtils.toSystemPath(path)

        then:
        result == ["","a","b","c"].join(File.separator);
    }

    void 'a mixed path should be correctly converted to a system path'() {
        given:
        def path = "\\a/b\\c"

        when:
        def result = PathUtils.toSystemPath(path)

        then:
        result == ["","a","b","c"].join(File.separator);
    }

    void 'a unix path should be correctly converted to a system path'() {
        given:
        def path = "/a/b/c"

        when:
        def result = PathUtils.toSystemPath(path)

        then:
        result == ["","a","b","c"].join(File.separator);
    }



}
