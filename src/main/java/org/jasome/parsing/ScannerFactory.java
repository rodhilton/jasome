package org.jasome.parsing;

import org.jasome.calculators.impl.*;

public class ScannerFactory {
    public static Scanner getScanner() {
        Scanner scanner = new Scanner();

        scanner.registerTypeCalculator(new RawTotalLinesOfCodeCalculator());
        scanner.registerTypeCalculator(new NumberOfFieldsCalculator());

        TotalLinesOfCodeCalculator totalLinesOfCodeCalculator = new TotalLinesOfCodeCalculator();
        scanner.registerPackageCalculator(totalLinesOfCodeCalculator);
        scanner.registerTypeCalculator(totalLinesOfCodeCalculator);
        scanner.registerMethodCalculator(totalLinesOfCodeCalculator);

        scanner.registerMethodCalculator(new NumberOfParametersCalculator());
        scanner.registerPackageCalculator(new NumberOfClassesCalculator());
        return scanner;
    }
}
