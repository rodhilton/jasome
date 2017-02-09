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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode treeNode = (TreeNode) o;
        return Objects.equal(getName(), treeNode.getName()) &&
                Objects.equal(getParent(), treeNode.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), getParent());
    }
}

