package org.jasome.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.EqualsVisitor;

import java.util.Optional;

/**
 * A lot of the JavaParser node types have their own equals and hashCode methods which use the EqualsVisitor to determine if they are equal,
 * but often they only include certain aspects of the element in the equality consideration.  For example, if you have two distinct method calls
 * to the same method they will both be instances of a MethodCallExpr but they will have the same scope, the same name, the same arguments, and
 * the same type arguments and thus be considered equal and having the same hashCode.  This is a problem when using the elements as keys or values
 * in hashes or graphs or other collection types because the distinctiveness of the elements is lost.
 *
 * This class essentially un-equals two different nodes that would otherwise be considered equal according to the EqualsVisitor.  Equals now requires
 * the objects actually be identical instances, and hashCode uses System.identityHashCode
 *
 * @param <T>
 */
public class DistinctNode<T extends Node> {
    private Node wrapped;

    private DistinctNode(T wrapped) {
        this.wrapped = wrapped;
    }

    public static <T extends Node> DistinctNode<T> of(T toWrap) {
        return new DistinctNode<>(toWrap);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof DistinctNode)) return false;
        else {
            DistinctNode other = (DistinctNode)o;
            return areEqual(Optional.of(other.wrapped), Optional.of(this.wrapped));
        }
    }

    private boolean areEqual(Optional<Node> one, Optional<Node> two) {
        if(!one.isPresent() && !two.isPresent()) return true;
        else if(!one.isPresent() || !two.isPresent()) return false;
        else return EqualsVisitor.equals(one.get(), two.get()) && areEqual(one.get().getParentNode(), two.get().getParentNode());
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(wrapped);
    }

    public Node get() {
        return wrapped;
    }


}
