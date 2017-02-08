package org.jasome.parsing;

import java.util.Set;

public class Project extends TreeNode {
    public Project() {
        super("root");
    }

    @SuppressWarnings("unchecked")
    public Set<ProjectPackage> getPackages() {
        return (Set<ProjectPackage>)(Set<?>)getChildren();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PROJECT;
    }

    public void addPackage(ProjectPackage projectPackage) {
        addChild(projectPackage);
    }

    @Override
    public String toString() {
        return this.toString(0);
    }
}
