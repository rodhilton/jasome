package org.jasome.parsing;

import org.jasome.calculators.impl.*;

public class ScannerFactory {
    public static Scanner getScanner() {
        Scanner scanner = new Scanner();

        scanner.registerClassCalculator(new RawTotalLinesOfCodeCalculator());
        scanner.registerClassCalculator(new NumberOfFieldsCalculator());

        TotalLinesOfCodeCalculator totalLinesOfCodeCalculator = new TotalLinesOfCodeCalculator();
        scanner.registerPackageCalculator(totalLinesOfCodeCalculator);
        scanner.registerClassCalculator(totalLinesOfCodeCalculator);
        scanner.registerMethodCalculator(totalLinesOfCodeCalculator);

        scanner.registerMethodCalculator(new NumberOfParametersCalculator());
        scanner.registerPackageCalculator(new NumberOfClassesCalculator());
        return scanner;
    }
}
