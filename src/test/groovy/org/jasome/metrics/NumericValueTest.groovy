package org.jasome.metrics

import com.google.common.collect.ImmutableList
import org.jasome.metrics.value.NumericValueSummaryStatistics
import org.jasome.metrics.value.NumericValue
import org.jscience.mathematics.number.Rational
import org.jscience.mathematics.number.Real
import spock.lang.Specification

class NumericValueTest extends Specification {

    def "can add"() {

        given:
        def integer = NumericValue.valueOf(10)
        def rational = NumericValue.valueOf(Rational.valueOf(5,3))
        def real = NumericValue.valueOf(3.1415)

        expect:
        integer.plus(integer) == NumericValue.valueOf(20);
        integer.plus(rational) == NumericValue.valueOf(Rational.valueOf(5,3).plus(Rational.valueOf(10,1)));
        integer.plus(real).toString() == "13.1415"

        rational.plus(integer) == integer.plus(rational)
        rational.plus(rational) == NumericValue.valueOf(Rational.valueOf(5,3).plus(Rational.valueOf(5,3)));
        rational.plus(real).toString() == "4.808166667"

        real.plus(integer) == integer.plus(real)
        real.plus(rational) == rational.plus(real);
        real.plus(real).toString() == "6.283"
    }

    def "can negate"() {

        given:
        def integer = NumericValue.valueOf(10)
        def rational = NumericValue.valueOf(Rational.valueOf(5,3))
        def real = NumericValue.valueOf(3.1415)

        expect:
        integer.negate() == NumericValue.valueOf(-10)
        rational.negate() == NumericValue.valueOf(Rational.valueOf(-5,3))
        real.negate() == NumericValue.valueOf(-3.1415)
    }


    def "can subtract"() {

        given:
        def integer = NumericValue.valueOf(10)
        def rational = NumericValue.valueOf(Rational.valueOf(5,3))
        def real = NumericValue.valueOf(3.1415)

        expect:
        integer.minus(integer) == NumericValue.valueOf(0);
        integer.minus(rational) == NumericValue.valueOf(Rational.valueOf(25,3))
        integer.minus(real).toString() == "6.8585"

        rational.minus(integer) == NumericValue.valueOf(Rational.valueOf(-25,3))
        rational.minus(rational) == NumericValue.valueOf(Rational.valueOf(0,1))
        rational.minus(real).toString() == "-1.474833333"

        real.minus(integer).toString() == "-6.8585"
        real.minus(rational).toString() == "1.474833333"
        real.minus(real) == NumericValue.valueOf(Real.ZERO)
    }

    def "can multiply"() {

        given:
        def integer = NumericValue.valueOf(10)
        def rational = NumericValue.valueOf(Rational.valueOf(5,3))
        def real = NumericValue.valueOf(3.1415)

        expect:
        integer.times(integer) == NumericValue.valueOf(100)
        integer.times(rational) == NumericValue.valueOf(Rational.valueOf(50,3))
        integer.times(real) == NumericValue.valueOf(31.415)

        rational.times(integer) == integer.times(rational)
        rational.times(rational) == NumericValue.valueOf(Rational.valueOf(25,9));
        rational.times(real).toString() == "5.235833333"

        real.times(integer) == integer.times(real)
        real.times(rational) == rational.times(real);
        real.times(real).toString() == "9.86902225"
    }

    def "can divide"() {

        given:
        def integer = NumericValue.valueOf(10)
        def rational = NumericValue.valueOf(Rational.valueOf(5,3))
        def real = NumericValue.valueOf(3.1415)

        def integer3 = NumericValue.valueOf(3)
        def rationalFourSevenths = NumericValue.valueOf(Rational.valueOf(4,7))

        expect:
        integer.divide(integer3) == NumericValue.valueOf(Rational.valueOf(10,3))
        integer.divide(rational) == NumericValue.valueOf(Rational.valueOf(6,1))
        integer.divide(real).toString() == "3.183192742"

        rational.divide(integer) == NumericValue.valueOf(Rational.valueOf(5, 30))
        rational.divide(rationalFourSevenths) == NumericValue.valueOf(Rational.valueOf(35,12));
        rational.divide(real).toString() == "0.530532124"

        real.divide(integer) == NumericValue.valueOf(0.31415)
        real.divide(rational) == NumericValue.valueOf(1.8849)
        real.divide(real).toString() == "1.0"
    }

    def "can compare"() {

        given:
        def integer = NumericValue.valueOf(10)
        def rational = NumericValue.valueOf(Rational.valueOf(5,3))
        def real = NumericValue.valueOf(3.1415)

        expect:
        integer.compareTo(integer) == 0
        integer.compareTo(rational) > 0
        integer.compareTo(real) > 0

        rational.compareTo(integer) < 0
        rational.compareTo(rational) == 0
        rational.compareTo(real) < 0

        real.compareTo(integer) < 0
        real.compareTo(rational) > 0
        real.compareTo(real) == 0
    }

    def "can max"() {

        given:
        def integer = NumericValue.valueOf(10)
        def rational = NumericValue.valueOf(Rational.valueOf(5,3))
        def real = NumericValue.valueOf(3.1415)

        expect:
        NumericValue.max(integer, rational) == integer
        NumericValue.max(rational, integer) == integer

        NumericValue.max(rational, real) == real
        NumericValue.max(real, rational) == real

        NumericValue.max(integer, real) == integer
        NumericValue.max(real, integer) == integer
    }

    def "can min"() {

        given:
        def integer = NumericValue.valueOf(10)
        def rational = NumericValue.valueOf(Rational.valueOf(5,3))
        def real = NumericValue.valueOf(3.1415)

        expect:
        NumericValue.min(integer, rational) == rational
        NumericValue.min(rational, integer) == rational

        NumericValue.min(rational, real) == rational
        NumericValue.min(real, rational) == rational

        NumericValue.min(integer, real) == real
        NumericValue.min(real, integer) == real
    }

    def "can collect"() {

        given:
        def vals = ImmutableList.of(
            NumericValue.valueOf(5.0),
            NumericValue.valueOf(1.0),
            NumericValue.valueOf(529.083333333),
            NumericValue.valueOf(1.0),
            NumericValue.valueOf(1.0),
            NumericValue.valueOf(2.0),
            NumericValue.valueOf(1.0)
        )

        when:
        NumericValueSummaryStatistics stats = vals.stream().collect(NumericValue.summarizingCollector())

        then:
        stats.getAverage().toString() == "77.154761905"
    }


}
