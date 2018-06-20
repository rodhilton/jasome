package org.jasome.input;

import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;

import java.util.Set;

public class Project extends Code {

    private JavaSymbolSolver symbolSolver;

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
        return "Project("+this.getName()+")";
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

    public void setSymbolSolver(JavaSymbolSolver symbolSolver) {
        this.symbolSolver = symbolSolver;
    }

    public JavaSymbolSolver getSymbolSolver() {
        return symbolSolver;
    }
}
