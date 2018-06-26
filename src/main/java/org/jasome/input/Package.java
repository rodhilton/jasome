package org.jasome.input;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Package extends Code {
    private Map<String, Type> typeLookup;

    public Package(String name) {
        super(name);
        typeLookup = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public Set<Type> getTypes() {
        return (Set<Type>)(Set<?>)getChildren();
    }

    public void addType(Type type) {
        typeLookup.put(type.getName(), type);
        addChild(type);
    }

    public Project getParentProject() {
        return (Project)getParent();
    }

    @Override
    public String toString() {
        return "Package("+this.getName()+")";
    }

    public Optional<Type> lookupTypeByName(String typeName) {
        if(typeLookup.containsKey(typeName)) {
            return Optional.of(typeLookup.get(typeName));
        } else {
            return Optional.empty();
        }
    }
}
