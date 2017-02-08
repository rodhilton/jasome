package org.jasome.parsing;

import java.util.Set;

public class ProjectClass extends TreeNode {
    public ProjectClass(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public Set<ProjectMethod> getMethods() {
        return (Set<ProjectMethod>)(Set<?>)getChildren();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CLASS;
    }

    public void addMethod(ProjectMethod projectMethod) {
        addChild(projectMethod);
    }

    public ProjectPackage getParentPackage() {
        return (ProjectPackage)getParent();
    }
}
