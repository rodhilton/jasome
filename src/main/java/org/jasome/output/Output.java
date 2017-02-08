package org.jasome.output;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jasome.calculators.Metric;
import org.jasome.parsing.TreeNode;

import java.util.*;
import java.util.stream.Collectors;

//Decorates the treenode stuff so it's all got attributes and whatnot
public class Output {

    private final TreeNode treeNode;
    private final Multimap<TreeNode, Metric> metrics;
    private final Multimap<TreeNode, Pair<String, String>> attributes;

    public Output(TreeNode treeNode, Multimap<TreeNode, Metric> metrics, Multimap<TreeNode, Pair<String, String>> attributes) {
        this.treeNode = treeNode;
        this.metrics = metrics;
        this.attributes = attributes;
    }

    Node getRoot() {
        return augmentNode(treeNode);
    }

    private Node augmentNode(TreeNode nodeToAugment) {
        Node node = new Node();
        node.name = nodeToAugment.getName();

        Map<String, String> attributesToSet = new HashMap<String, String>();
        for (Pair<String, String> pair : attributes.get(nodeToAugment)) {
            attributesToSet.put(pair.getKey(), pair.getValue());
        }

        node.attributes = attributesToSet;
        node.metrics = metrics.get(nodeToAugment);

        List<Node> childrenToSet = new ArrayList<Node>();
        for (TreeNode childNode : nodeToAugment.getChildren()) {
            childrenToSet.add(augmentNode(childNode));
        }
        node.children = childrenToSet;

        return node;
    }


    static class Node {
        private String name;
        //private SourceContext sourceContext;
        private Collection<Node> children = new HashSet<Node>();
        private Collection<Metric> metrics = new HashSet<Metric>();
        private Map<String, String> attributes = new HashMap<String, String>();

        public String getName() {
            return name;
        }

        public Collection<Node> getChildren() {
            return children;
        }

        public Collection<Metric> getMetrics() {
            return metrics;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public String toString(int level) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.repeat(' ', level));
            sb.append(name);
            if (!this.attributes.isEmpty()) {
                sb.append(" " + this.attributes.toString() + "");
            }
            sb.append("");
            sb.append("\n");
            //do all metrics
            List<Metric> sortedMetrics = metrics.stream().sorted(new Comparator<Metric>() {
                @Override
                public int compare(Metric o1, Metric o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            }).collect(Collectors.toList());

            for (Metric metric : sortedMetrics) {
                sb.append(StringUtils.repeat(' ', level) + "+");
                sb.append(metric.getName() + ": " + metric.getValue());
                sb.append("\n");
            }
            for (Node child : children) {
                sb.append(child.toString(level + 1));
            }
            return sb.toString();
        }
    }
}
