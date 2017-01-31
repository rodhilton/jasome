package org.jasome.output;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.jasome.calculators.Calculation;

import java.util.*;
import java.util.stream.Collectors;

//TODO: this is awful, write some tests against it and refactor heavily
public class Output {
    private Node root;

    public Output() {
        root = new Node();
        root.name = "root";
    }

    public String toString() {
        return root.toString(0);
    }

    public void addCalculations(Set<Calculation> metrics, String... navigation) {
        addCalculations(metrics, Maps.newHashMap(), navigation);
    }


    public void addCalculations(Set<Calculation> metrics, Map<String, String> attributes, String... navigation) {
        synchronized (root) {
            addCalculations(metrics, attributes, root, navigation);
        }
    }

    private void addCalculations(Set<Calculation> metrics, Map<String, String> attributes, Node root, String... navigation) {

        Optional<Node> foundNodeOpt = root.children.stream().filter(c->c.name.equals(navigation[0])).findFirst();

        Node correctNode;
        if(!foundNodeOpt.isPresent()) {
            correctNode = new Node();
            correctNode.name = navigation[0];
            correctNode.attributes.putAll(attributes);
            root.children.add(correctNode);
        } else {
            correctNode = foundNodeOpt.get();
        }

        if(navigation.length == 1) { //at the end
            correctNode.metrics.addAll(metrics);
        } else {
            addCalculations(metrics, attributes, correctNode, Arrays.copyOfRange(navigation, 1, navigation.length));
        }
    }

    Node getRoot() {
        return root;
    }


    static class Node {
        private String name;
        //private SourceContext sourceContext;
        private Set<Node> children = new HashSet<Node>();
        private Set<Calculation> metrics = new HashSet<Calculation>();
        private Map<String, String> attributes = new HashMap<String, String>();

        public String getName() {
            return name;
        }

        public Set<Node> getChildren() {
            return children;
        }

        public Set<Calculation> getMetrics() {
            return metrics;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public String toString(int level) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.repeat(' ', level));
            sb.append(name);
            if(!this.attributes.isEmpty()) {
                sb.append(" "+this.attributes.toString()+"");
            }
            sb.append("");
            sb.append("\n");
            //do all metrics
            List<Calculation> sortedMetrics = metrics.stream().sorted(new Comparator<Calculation>() {
                @Override
                public int compare(Calculation o1, Calculation o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            }).collect(Collectors.toList());

            for(Calculation metric: sortedMetrics) {
                sb.append(StringUtils.repeat(' ', level)+"+");
                sb.append(metric.getName()+": "+metric.getValue());
                sb.append("\n");
            }
            for(Node child: children) {
                sb.append(child.toString(level+1));
            }
            return sb.toString();
        }
    }
}
