package org.jasome.input;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class TestScanner extends Scanner<String> {
    public Project scan(Collection<String> inputSources) {

        Collection<Pair<String, Map<String, String>>> sourceCodeWithAttributes = inputSources
                .stream()
                .<Pair<String, Map<String, String>>>map(source -> Pair.of(source, ImmutableMap.of())).collect(Collectors.toList());

        return this.doScan(sourceCodeWithAttributes);
    }
}
