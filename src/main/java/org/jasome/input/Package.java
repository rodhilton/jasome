package org.jasome.input;

import java.util.Set;

public class Package extends Code {
    public Package(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public Set<Type> getTypes() {
        return (Set<Type>)(Set<?>)getChildren();
    }

    public void addType(Type type) {
        addChild(type);
    }

    public Project getParentProject() {
        return (Project)getParent();
    }

    @Override
    public String toString() {
        return "Package("+this.getName()+")";
    }
}
