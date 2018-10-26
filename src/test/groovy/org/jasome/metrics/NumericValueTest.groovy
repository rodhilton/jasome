package org.jasome.metrics

import com.google.common.collect.ImmutableList
import org.jasome.metrics.value.NumericValueSummaryStatistics
import org.jasome.metrics.value.NumericValue
import org.jscience.mathematics.number.LargeInteger
import org.jscience.mathematics.number.Rational
import org.jscience.mathematics.number.Real
import spock.lang.Specification

import java.text.DecimalFormat

class NumericValueTest extends Specification {
    private static final DecimalFormat METRIC_VALUE_FORMAT = new DecimalFormat("0.0########")

    def "can add"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        expect:
        integer + integer == NumericValue.of(20)
        integer + rational == NumericValue.of(Rational.valueOf(5, 3) + Rational.valueOf(10, 1))
        (integer + real).toString() == METRIC_VALUE_FORMAT.format(13.1415)

        rational + integer == integer + rational
        rational + rational == NumericValue.of(Rational.valueOf(5, 3) + Rational.valueOf(5, 3))
        (rational + real).toString() == METRIC_VALUE_FORMAT.format(4.808166667)

        real + integer == integer + real
        real + rational == rational + real
        (real + real).toString() == METRIC_VALUE_FORMAT.format(6.283)
    }

    def "can negate"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        expect:
        integer.negate() == NumericValue.of(-10)
        rational.negate() == NumericValue.of(Rational.valueOf(-5,3))
        real.negate() == NumericValue.of(-3.1415)
    }


    def "can subtract"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        expect:
        integer - integer == NumericValue.of(0)
        integer - rational == NumericValue.of(Rational.valueOf(25,3))
        (integer - real).toString() == METRIC_VALUE_FORMAT.format(6.8585)

        rational - integer == NumericValue.of(Rational.valueOf(-25,3))
        rational - rational == NumericValue.of(Rational.valueOf(0,1))
        (rational - real).toString() == METRIC_VALUE_FORMAT.format(-1.474833333)

        (real - integer).toString() == METRIC_VALUE_FORMAT.format(-6.8585)
        (real - rational).toString() == METRIC_VALUE_FORMAT.format(1.474833333)
        real - real == NumericValue.of(Real.ZERO)
    }

    def "can multiply"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        expect:
        integer.times(integer) == NumericValue.of(100)
        integer.times(rational) == NumericValue.of(Rational.valueOf(50,3))
        integer.times(real) == NumericValue.of(31.415)

        rational.times(integer) == integer.times(rational)
        rational.times(rational) == NumericValue.of(Rational.valueOf(25,9))
        rational.times(real).toString() == METRIC_VALUE_FORMAT.format(5.235833333)

        real.times(integer) == integer.times(real)
        real.times(rational) == rational.times(real)
        real.times(real).toString() == METRIC_VALUE_FORMAT.format(9.86902225)
    }

    def "can divide"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        def integer3 = NumericValue.of(3)
        def rationalFourSevenths = NumericValue.of(Rational.valueOf(4,7))

        expect:
        integer.divide(integer3) == NumericValue.of(Rational.valueOf(10,3))
        integer.divide(rational) == NumericValue.of(Rational.valueOf(6,1))
        integer.divide(real).toString() == METRIC_VALUE_FORMAT.format(3.183192742)

        rational.divide(integer) == NumericValue.of(Rational.valueOf(5, 30))
        rational.divide(rationalFourSevenths) == NumericValue.of(Rational.valueOf(35,12))
        rational.divide(real).toString() == METRIC_VALUE_FORMAT.format(0.530532124)

        real.divide(integer) == NumericValue.of(0.31415)
        real.divide(rational) == NumericValue.of(1.8849)
        real.divide(real).toString() == METRIC_VALUE_FORMAT.format(1.0)
    }

    def "can exponentiate"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        expect:
        integer.pow(2) == NumericValue.of(LargeInteger.valueOf(10).pow(2))
        rational.pow(2) == NumericValue.of(Rational.valueOf(5, 3).pow(2))
        real.pow(2) == NumericValue.of(Real.valueOf(3.1415).pow(2))
    }

    def "can compare"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        expect:
        integer == integer
        integer > rational
        integer > real

        rational < integer
        rational == rational
        rational < real

        real < integer
        real > rational
        real == real
    }

    def "can max"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

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
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        expect:
        NumericValue.min(integer, rational) == rational
        NumericValue.min(rational, integer) == rational

        NumericValue.min(rational, real) == rational
        NumericValue.min(real, rational) == rational

        NumericValue.min(integer, real) == real
        NumericValue.min(real, integer) == real
    }

    def "can abs"() {

        given:
        def integer = NumericValue.of(10)
        def rational = NumericValue.of(Rational.valueOf(5,3))
        def real = NumericValue.of(3.1415)

        expect:
        integer.abs() == integer
        rational.abs() == rational
        real.abs() == real

        (NumericValue.ZERO - integer).abs() == integer
        (NumericValue.ZERO - rational).abs() == rational
        (NumericValue.ZERO - real).abs() == real
    }

    def "can collect"() {

        given:
        def vals = ImmutableList.of(
            NumericValue.of(5.0),
            NumericValue.of(1.0),
            NumericValue.of(529.083333333),
            NumericValue.of(1.0),
            NumericValue.of(1.0),
            NumericValue.of(2.0),
            NumericValue.of(1.0)
        )

        when:
        NumericValueSummaryStatistics stats = vals.stream().collect(NumericValue.summarizingCollector())

        then:
        stats.getAverage().toString() == METRIC_VALUE_FORMAT.format(77.154761905)
        stats.getCount() == NumericValue.of(7)
        stats.getSum() == NumericValue.of(540.083333333)
        stats.getMax() == NumericValue.of(529.083333333)
        stats.getMin() == NumericValue.of(1.0)
    }


}
