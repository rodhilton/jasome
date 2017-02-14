package org.jasome.parsing;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.calculators.Metric;

import java.util.*;

public abstract class Code {
    private String name;
    protected Set<Code> children = new HashSet<Code>();
    private Code parent = null;
    private final Map<String, Metric> metrics;
    private final Set<Pair<String, String>> attributes;

    public Code(String name) {
        this.name = name;
        this.children = new HashSet<Code>();
        this.metrics = new HashMap<String, Metric>();
        this.attributes = new HashSet<Pair<String, String>>();
    }

    public String getName() {
        return name;
    }

    public Set<Metric> getMetrics() {
        return ImmutableSet.copyOf(metrics.values());
    }

    public Optional<Metric> getMetric(String name) {
        return Optional.of(this.metrics.get(name));
    }

    public Set<Pair<String, String>> getAttributes() {
        return ImmutableSet.copyOf(attributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Code code = (Code) o;
        return Objects.equal(getName(), code.getName()) &&
                Objects.equal(getParent(), code.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), getParent());
    }

    //* * * Package-level, only the scanner can add metrics and attributes, not calculators

    void addMetric(Metric metric) {
        metrics.put(metric.getName(), metric);
    }

    void addAttribute(String key, String value) {
        addAttributes(Pair.of(key, value));
    }

    void addAttributes(Pair<String, String>... attributes) {
        this.attributes.addAll(Arrays.asList(attributes));
    }

    void addMetrics(Set<Metric> metrics) {
        for(Metric metric: metrics) {
            this.addMetric(metric);
        }
    }

    //* * * Protected, only subclasses should be able to directly add/access parent and children, other callers should use addMethod, addPackage, etc
    
    protected Set<Code> getChildren() {
        return children;
    }

    protected Code getParent() {
        return parent;
    }

    protected void addChild(Code child) {
        child.parent = this;
        this.children.add(child);
    }

}

