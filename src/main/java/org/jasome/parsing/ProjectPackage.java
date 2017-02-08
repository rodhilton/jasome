package org.jasome.parsing;

import java.util.Set;

public class ProjectPackage extends TreeNode {
    public ProjectPackage(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public Set<ProjectClass> getClasses() {
        return (Set<ProjectClass>)(Set<?>)getChildren();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PACKAGE;
    }

    public void addClass(ProjectClass projectClass) {
        addChild(projectClass);
    }

    public Project getParentProject() {
        return (Project)getParent();
    }
}
