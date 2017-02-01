package org.jasome.output;

import org.jasome.calculators.Metric;
import org.jasome.executive.Outputter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class XMLOutputter implements Outputter<Document> {

    @Override
    public Document output(Output output) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Packages");
            doc.appendChild(rootElement);

            Output.Node rootNode = output.getRoot();

            for(Output.Node packageNode: sortChildren(rootNode.getChildren())) {
                Element packageElement = doc.createElement("Package");
                packageElement.setAttribute("name", packageNode.getName());
                rootElement.appendChild(packageElement);

                addAttributes(packageNode, packageElement);
                addMetricsForNode(doc, packageElement, packageNode);

                //if(packageNode.getChildren().size() > 0) {
                    Element classesElement = doc.createElement("Classes");
                    packageElement.appendChild(classesElement);

                    for (Output.Node classNode : sortChildren(packageNode.getChildren())) {
                        Element classElement = doc.createElement("Class");
                        classElement.setAttribute("name", classNode.getName());
                        classesElement.appendChild(classElement);

                        addAttributes(classNode, classElement);
                        addMetricsForNode(doc, classElement, classNode);

                       // if(classNode.getChildren().size() > 0) {
                            Element methodsElement = doc.createElement("Methods");
                            classElement.appendChild(methodsElement);

                            for (Output.Node methodNode : sortChildren(classNode.getChildren())) {
                                Element methodElement = doc.createElement("Method");
                                methodElement.setAttribute("name", methodNode.getName());
                                methodsElement.appendChild(methodElement);

                                addAttributes(methodNode, methodElement);
                                addMetricsForNode(doc, methodElement, methodNode);
                            }
                        //}
                    }
                //}
            }

            return doc;

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

    }

    private List<Output.Node> sortChildren(Set<Output.Node> children) {
        return children.stream().sorted(new Comparator<Output.Node>() {
                            @Override
                            public int compare(Output.Node o1, Output.Node o2) {
                                return o1.getName().compareTo(o2.getName());
                            }

                        }).collect(Collectors.toList());
    }

    private void addAttributes(Output.Node classNode, Element classElement) {
        for(Map.Entry<String, String> attribute: classNode.getAttributes().entrySet()) {
            classElement.setAttribute(attribute.getKey(), attribute.getValue());
        }
    }

    private void addMetricsForNode(Document doc, Node parentElement, Output.Node node) {
//        if(node.getMetrics().size() > 0) {
            Element metricsContainer = doc.createElement("Metrics");
            for (Metric metric : node.getMetrics().values()) {
                Element metrics = doc.createElement("Metric");

                metrics.setAttribute("name", metric.getName());
                metrics.setAttribute("description", metric.getDescription());
                metrics.setAttribute("value", metric.getValue().toString());

                metricsContainer.appendChild(metrics);
            }
            parentElement.appendChild(metricsContainer);
//        }
    }
}
