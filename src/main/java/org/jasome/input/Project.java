package org.jasome.input;

import java.util.Set;

public class Project extends Code {
    public Project(String name) {
        super(name);
    }
    
    @SuppressWarnings("unchecked")
    public Set<Package> getPackages() {
        return (Set<Package>)(Set<?>)getChildren();
    }

    public void addPackage(Package aPackage) {
        addChild(aPackage);
    }

    @Override
    public String toString() {
        return "Project";
    }


    //Normally equals is just, a matching name and parent
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
