package org.jasome.parsing;

import org.jasome.calculators.impl.*;

public class ScannerFactory {
    public static Scanner getScanner() {
        Scanner scanner = new Scanner();

        scanner.registerTypeCalculator(new RawTotalLinesOfCodeCalculator());

        scanner.registerTypeCalculator(new NumberOfFieldsCalculator());

        scanner.registerTypeCalculator(new TotalLinesOfCodeCalculator());

        scanner.registerMethodCalculator(new NumberOfParametersCalculator());
        scanner.registerPackageCalculator(new NumberOfClassesCalculator());

        scanner.registerTypeCalculator(new SpecializationIndexCalculator());
        return scanner;
    }
}
