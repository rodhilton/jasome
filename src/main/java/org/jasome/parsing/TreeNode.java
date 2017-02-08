package org.jasome.parsing;

import com.github.javaparser.ast.Node;
import com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class TreeNode {
    private String name;
    protected Set<TreeNode> children = new HashSet<TreeNode>();
    private TreeNode parent;

    public TreeNode(String name) {
        this.name = name;
    }

    public abstract NodeType getNodeType();

    public String getName() {
        return name;
    }

    public Set<TreeNode> getChildren() {
        return children;
    }

    protected TreeNode getParent() {
        return parent;
    }

    public void addChild(TreeNode child) {
        child.parent = this;
        this.children.add(child);
    }

    public String toString(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.repeat(' ', level));
        sb.append(name);
        sb.append("");
        sb.append("\n");
        for (TreeNode child : children) {
            sb.append(child.toString(level + 1));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode that = (TreeNode) o;
        return Objects.equal(getName(), that.getName()) &&
                getNodeType() == that.getNodeType();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), getNodeType());
    }
}

class ProjectNode extends TreeNode {
    public ProjectNode() {
        super("root");
    }

    @SuppressWarnings("unchecked")
    public Set<PackageNode> getPackages() {
        return (Set<PackageNode>)(Set<?>)getChildren();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PROJECT;
    }

    public void addPackage(PackageNode packageNode) {
        addChild(packageNode);
    }

    @Override
    public String toString() {
        return this.toString(0);
    }
}

class PackageNode extends TreeNode {
    public PackageNode(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public Set<ClassNode> getClasses() {
        return (Set<ClassNode>)(Set<?>)getChildren();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PACKAGE;
    }

    public void addClass(ClassNode classNode) {
        addChild(classNode);
    }

    public ProjectNode getParentProject() {
        return (ProjectNode)getParent();
    }
}

class ClassNode extends TreeNode {
    public ClassNode(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public Set<MethodNode> getMethods() {
        return (Set<MethodNode>)(Set<?>)getChildren();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CLASS;
    }

    public void addMethod(MethodNode methodNode) {
        addChild(methodNode);
    }

    public PackageNode getParentPackage() {
        return (PackageNode)getParent();
    }
}

class MethodNode extends TreeNode {
    public MethodNode(String name) {
        super(name);
    }

    public ClassNode getParentClass() {
        return (ClassNode)getParent();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.METHOD;
    }
}

enum NodeType {
    PROJECT, PACKAGE, CLASS, METHOD;

    public static NodeType fromDepth(int depth) {
        switch (depth) {
            case 0:
                return PROJECT;
            case 1:
                return PACKAGE;
            case 2:
                return CLASS;
            case 3:
                return METHOD;
            default:
                throw new RuntimeException("Not a valid tree depth");
        }
    }
}