package org.jasome.metrics;

import org.jscience.mathematics.number.FloatingPoint;
import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Number;
import org.jscience.mathematics.number.Rational;

public class MathOps {

    public static Number plus(Number l, Number r) {
        if(l instanceof LargeInteger) {
            if(r instanceof LargeInteger) {
                return ((LargeInteger) l).plus((LargeInteger)r);
            } else if(r instanceof Rational) {
                return ((Rational)r).plus(Rational.valueOf((LargeInteger)l, LargeInteger.ONE));
            }

        } else if (l instanceof Rational) {
            if(r instanceof LargeInteger) {
                return ((Rational)l).plus(Rational.valueOf((LargeInteger)r, LargeInteger.ONE));
            } else if(r instanceof Rational) {
                return ((Rational)r).plus((Rational)l);
            }

        } else if( l instanceof FloatingPoint) {

        }

        throw new UnsupportedOperationException("Newp");
    }
}
