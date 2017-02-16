package org.jasome.input;

import org.jasome.metrics.Calculator;
import org.jasome.metrics.Metric;

import java.util.HashSet;
import java.util.Set;


//TODO: try to figure out a way to move this to org.jasome.metrics - it needs to access a package-level method on Code which prevents this
public class Processor {
    private Set<Calculator<Project>> projectCalculators;
    private Set<Calculator<Package>> packageCalculators;
    private Set<Calculator<Type>> typeCalculators;
    private Set<Calculator<Method>> methodCalculators;

    public Processor() {
        projectCalculators = new HashSet<>();
        packageCalculators = new HashSet<>();
        typeCalculators = new HashSet<>();
        methodCalculators = new HashSet<>();
    }

    public void registerProjectCalculator(Calculator<Project> calculator) {
        projectCalculators.add(calculator);
    }

    public void registerPackageCalculator(Calculator<Package> calculator) {
        packageCalculators.add(calculator);
    }

    public void registerTypeCalculator(Calculator<Type> calculator) {
        typeCalculators.add(calculator);
    }

    public void registerMethodCalculator(Calculator<Method> calculator) {
        methodCalculators.add(calculator);
    }

    public void process(Project project) {

        for (Package aPackage : project.getPackages()) {

            for (Type type : aPackage.getTypes()) {

                for (Method method : type.getMethods()) {

                    for (Calculator<Method> methodMetricCalculator : methodCalculators) {
                        Set<Metric> methodMetrics = methodMetricCalculator.calculate(method);
                        method.addMetrics(methodMetrics);
                    }
                }

                for (Calculator<Type> typeMetricCalculator : typeCalculators) {
                    Set<Metric> classMetrics = typeMetricCalculator.calculate(type);
                    type.addMetrics(classMetrics);
                }
            }

            for (Calculator<Package> packageMetricCalculator : packageCalculators) {
                Set<Metric> packageMetrics = packageMetricCalculator.calculate(aPackage);
                aPackage.addMetrics(packageMetrics);
            }
        }

        for (Calculator<Project> projectMetricCalculator : projectCalculators) {
            Set<Metric> projectMetrics = projectMetricCalculator.calculate(project);
            project.addMetrics(projectMetrics);
        }


        for (Package aPackage : project.getPackages()) {
            System.out.println(aPackage.getName());
            System.out.println("+" + aPackage.getMetrics());

            for (Type type : aPackage.getTypes()) {

                System.out.println("  " + type.getName());
                System.out.println("  +" + type.getMetrics());

                for (Method method : type.getMethods()) {

                    System.out.println("    " + method.getName());
                    System.out.println("    +" + aPackage.getMetrics());

                }
            }

        }
    }
}
