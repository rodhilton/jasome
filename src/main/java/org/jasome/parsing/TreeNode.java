package org.jasome.parsing;

import com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class TreeNode {
    private String name;
    protected Set<TreeNode> children = new HashSet<TreeNode>();
    private TreeNode parent = null;

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
        TreeNode treeNode = (TreeNode) o;
        return Objects.equal(getName(), treeNode.getName()) &&
                getNodeType() == treeNode.getNodeType() &&
                Objects.equal(getParent(), treeNode.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), getParent(), getNodeType());
    }

    enum NodeType {
        PROJECT, PACKAGE, TYPE, METHOD;

        public static NodeType fromDepth(int depth) {
            switch (depth) {
                case 0:
                    return PROJECT;
                case 1:
                    return PACKAGE;
                case 2:
                    return TYPE;
                case 3:
                    return METHOD;
                default:
                    throw new RuntimeException("Not a valid tree depth");
            }
        }
    }
}

