package org.jasome.metrics.value;

import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Rational;
import org.jscience.mathematics.number.Real;

class Coerce {
    public static Rational toRational(LargeInteger i) {
         return Rational.valueOf(i, LargeInteger.ONE);
    }

    public static Real toReal(LargeInteger value) {
        return Real.valueOf(value, 0, 0);
    }

    public static Real toReal(Rational value) {
        Rational rationalValue = (Rational)value;
        Real dividendReal = Real.valueOf(rationalValue.getDividend(), 0, 0);
        Real divisorReal = Real.valueOf(rationalValue.getDivisor(), 0, 0);
        return dividendReal.divide(divisorReal);
    }
}
