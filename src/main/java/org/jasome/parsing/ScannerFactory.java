package org.jasome.parsing;

import org.jasome.calculators.impl.*;

public class ScannerFactory {
    public static Scanner getScanner() {
        Scanner scanner = new Scanner();

        scanner.registerTypeCalculator(new RawTotalLinesOfCodeCalculator());

        scanner.registerTypeCalculator(new NumberOfFieldsCalculator());

        scanner.registerProjectCalculator(new TotalLinesOfCodeCalculator.ProjectCalculator());
        scanner.registerPackageCalculator(new TotalLinesOfCodeCalculator.PackageCalculator());
        scanner.registerTypeCalculator(new TotalLinesOfCodeCalculator.TypeCalculator());
        scanner.registerMethodCalculator(new TotalLinesOfCodeCalculator.MethodCalculator());

        scanner.registerMethodCalculator(new CyclomaticComplexityCalculator());
        scanner.registerTypeCalculator(new WeightedMethodsCalculator());

        scanner.registerMethodCalculator(new NumberOfParametersCalculator());
        scanner.registerPackageCalculator(new NumberOfClassesCalculator());

        scanner.registerTypeCalculator(new SpecializationIndexCalculator());
        return scanner;
    }
}
