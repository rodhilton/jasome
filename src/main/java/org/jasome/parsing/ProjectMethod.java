package org.jasome.parsing;

public class ProjectMethod extends TreeNode {
    public ProjectMethod(String name) {
        super(name);
    }

    public ProjectClass getParentClass() {
        return (ProjectClass)getParent();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.METHOD;
    }
}
