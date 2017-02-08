package org.jasome.parsing;

import java.util.Set;

public class Project extends TreeNode {
    public Project() {
        super("root");
    }

    @SuppressWarnings("unchecked")
    public Set<Package> getPackages() {
        return (Set<Package>)(Set<?>)getChildren();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PROJECT;
    }

    public void addPackage(Package aPackage) {
        addChild(aPackage);
    }

    @Override
    public String toString() {
        return this.toString(0);
    }
}
