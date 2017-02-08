package org.jasome.parsing;

import org.jasome.calculators.impl.*;

public class ScannerFactory {
    public static Scanner getScanner() {
        Scanner scanner = new Scanner();

        scanner.registerTypeCalculator(new RawTotalLinesOfCodeCalculator());
        scanner.registerTypeCalculator(new NumberOfFieldsCalculator());

        scanner.registerPackageCalculator(TotalLinesOfCodeCalculator.forPackage());
        scanner.registerTypeCalculator(TotalLinesOfCodeCalculator.forType());
        scanner.registerMethodCalculator(TotalLinesOfCodeCalculator.forMethod());

        scanner.registerMethodCalculator(new NumberOfParametersCalculator());
        scanner.registerPackageCalculator(new NumberOfClassesCalculator());
        return scanner;
    }
}
