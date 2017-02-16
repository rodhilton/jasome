package org.jasome.util

import org.apache.commons.io.IOUtils
import org.jasome.input.Method
import org.jasome.input.Package
import org.jasome.input.Project
import org.jasome.input.TestScanner
import org.jasome.input.Type

class TestUtil {

    static Type typeFromSnippet(String sourceCode) {
        Package p = packageFromSnippet(sourceCode)
        assert p.getTypes().size() == 1

        return new ArrayList<Type>(p.types).get(0)
    }

    static Type typeFromStream(InputStream stream) {
        String snippet = IOUtils.toString(stream, "UTF-8");
        return typeFromSnippet(snippet);
    }

    static Method methodFromSnippet(String sourceCode) {
        String fakeSourceCode = """
        package org.test.example;
        
        public class MyClass {
            ${sourceCode}
        }
        """.trim()

        Type t = typeFromSnippet(fakeSourceCode)
        assert t.getMethods().size() == 1

        return new ArrayList<Method>(t.methods).get(0)
    }

    static Package packageFromSnippet(String sourceCode) {
        Project p = projectFromSnippet(sourceCode)
        assert p.getPackages().size() == 1

        return new ArrayList<Package>(p.packages).get(0)
    }

    static Project projectFromSnippet(String... sourceCodes) {
        return new TestScanner().scan(Arrays.asList(sourceCodes))
    }
}
