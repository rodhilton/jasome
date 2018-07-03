package org.jasome.metrics.value;

import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Number;
import org.jscience.mathematics.number.Rational;
import org.jscience.mathematics.number.Real;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * NumericValue is meant to encapsulate and hide differences between Integers, Rational numbers, and Real numbers,
 * keeping perfect precision as long as possible until a mathematical operation forces coercion to a floating point Real
 * number.
 *
 * It accomplishes this by clunkily checking what type of jscience Number instances are being operated upon and wraps
 * the resulting type in a NumericValue
 */
public class NumericValue implements Comparable<NumericValue> {
    private static final DecimalFormat METRIC_VALUE_FORMAT = new DecimalFormat("0.0########");

    private Number value;

    public static final NumericValue ZERO=new NumericValue(LargeInteger.ZERO);
    public static final NumericValue ONE=new NumericValue(LargeInteger.ONE);

    public static NumericValue of(long l) {
        return new NumericValue(LargeInteger.valueOf(l));
    }

    public static NumericValue ofRational(long numerator, long denominator) {
        return new NumericValue(Rational.valueOf(numerator, denominator));
    }

    public static NumericValue of(double d) {
        return new NumericValue(Real.valueOf(d));
    }

    public static NumericValue of(BigInteger value) {
        return new NumericValue(LargeInteger.valueOf(value));
    }

    public static NumericValue of(Number n) {
        assert(n!=null);
        return new NumericValue(n);
    }

    private NumericValue(Number value) {
        assert(value!=null);
        this.value=value;
    }

    public NumericValue plus(NumericValue that) {
        assert(that!=null);

        Number other = that.value;
        if(value instanceof LargeInteger) {
            if(other instanceof LargeInteger) {
                return new NumericValue(((LargeInteger) value).plus((LargeInteger)other));
            } else if(other instanceof Rational) {
                return new NumericValue(((Rational)other).plus(Coerce.toRational((LargeInteger)value)));
            } else if(other instanceof Real) {
                return new NumericValue(((Real)other).plus(Coerce.toReal((LargeInteger)value)));
            }

        } else if (value instanceof Rational) {
            if(other instanceof LargeInteger) {
                return that.plus(this);
            } else if(other instanceof Rational) {
                return new NumericValue(((Rational)other).plus((Rational) value));
            } else if(other instanceof Real) {
                return new NumericValue(((Real)other).plus(Coerce.toReal((Rational)value)));
            }

        } else if( value instanceof Real) {
            if(other instanceof LargeInteger) {
                return that.plus(this);
            } else if (other instanceof Real) {
                return new NumericValue(((Real)other).plus((Real) value));
            } else if(other instanceof Rational) {
                return that.plus(this);
            }
        }

        throw new UnsupportedOperationException("Unable to add "+value.getClass()+" to "+value.getClass());
    }

    public NumericValue negate() {
        if(value instanceof LargeInteger) {
            return new NumericValue(LargeInteger.ZERO.minus((LargeInteger)value));
        } else if (value instanceof Rational) {
            return new NumericValue(Rational.ZERO.minus((Rational)value));
        } else if(value instanceof Real) {
            return new NumericValue(Real.ZERO.minus((Real)value));
        }

        throw new UnsupportedOperationException("Unable to negate "+value.getClass());
    }

    public NumericValue minus(NumericValue that) {
        assert(that!=null);
        return this.plus(that.negate());
    }

    public NumericValue times(NumericValue that) {
        assert(that!=null);

        Number other = that.value;
        if(value instanceof LargeInteger) {
            if(other instanceof LargeInteger) {
                return new NumericValue(((LargeInteger) value).times((LargeInteger)other));
            } else if(other instanceof Rational) {
                return new NumericValue(((Rational)other).times(Coerce.toRational((LargeInteger)value)));
            } else if(other instanceof Real) {
                return new NumericValue(((Real)other).times(Coerce.toReal((LargeInteger)value)));
            }

        } else if (value instanceof Rational) {
            if(other instanceof LargeInteger) {
                return that.times(this);
            } else if(other instanceof Rational) {
                return new NumericValue(((Rational)other).times((Rational) value));
            } else if(other instanceof Real) {
                return new NumericValue(((Real)other).times(Coerce.toReal((Rational)value)));
            }

        } else if( value instanceof Real) {
            if(other instanceof LargeInteger) {
                return that.times(this);
            } else if (other instanceof Real) {
                return new NumericValue(((Real)other).times((Real) value));
            } else if(other instanceof Rational) {
                return that.times(this);
            }
        }

        throw new UnsupportedOperationException("Unable to multiply "+value.getClass()+" to "+value.getClass());
    }

    public NumericValue divide(NumericValue that) {
        assert(that!=null);

        Number other = that.value;
        if(value instanceof LargeInteger) {
            if(other instanceof LargeInteger) {
                return new NumericValue(Rational.valueOf((LargeInteger)value, (LargeInteger)other));
            } else if(other instanceof Rational) {
                return new NumericValue(Coerce.toRational((LargeInteger)value).divide((Rational)other));
            } else if(other instanceof Real) {
                return new NumericValue(Coerce.toReal((LargeInteger)value).divide((Real)other));
            }

        } else if (value instanceof Rational) {
            if(other instanceof LargeInteger) {
                return new NumericValue(((Rational) value).divide(Coerce.toRational((LargeInteger)other)));
            } else if(other instanceof Rational) {
                return new NumericValue(((Rational) value).divide((Rational) other));
            } else if(other instanceof Real) {
                return new NumericValue(Coerce.toReal((Rational)value).divide((Real)other));
            }

        } else if( value instanceof Real) {
            if(other instanceof LargeInteger) {
                return new NumericValue(((Real)value).divide(Coerce.toReal((LargeInteger)other)));
            } else if(other instanceof Rational) {
                return new NumericValue(((Real)value).divide(Coerce.toReal((Rational)other)));
            } else if (other instanceof Real) {
                return new NumericValue(((Real)value).divide((Real) other));
            }
        }

        throw new UnsupportedOperationException("Unable to divide "+value.getClass()+" to "+value.getClass());
    }

    public NumericValue pow(int exp) {
        return new NumericValue(value.pow(exp));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumericValue numericValue = (NumericValue) o;
        if(value instanceof Real && numericValue.value instanceof Real) {
            return ((Real)value).approximates((Real) numericValue.value);
        } else {
            return Objects.equals(value, numericValue.value);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if(value instanceof LargeInteger) {
            return value.toString();
        } else {
            return METRIC_VALUE_FORMAT.format(value.doubleValue());
        }
    }

    public long longValue() {
        return value.longValue();
    }

    public double doubleValue() {
        return value.doubleValue();
    }

    public static NumericValue max(NumericValue left, NumericValue right) {
        if(left.minus(right).compareTo(NumericValue.ZERO) < 0) return right;
        else return left;
    }

    public static NumericValue min(NumericValue left, NumericValue right) {
        if(left.minus(right).compareTo(NumericValue.ZERO) < 0) return left;
        else return right;
    }

    @Override
    public int compareTo(NumericValue that) {
        assert(that!=null);
        Number other = that.value;
        if(value instanceof LargeInteger) {
            if(other instanceof LargeInteger) {
                return ((LargeInteger) value).compareTo((LargeInteger)other);
            } else if(other instanceof Rational) {
                return Coerce.toRational((LargeInteger)value).compareTo((Rational)other);
            } else if(other instanceof Real) {
                return compareReals(Coerce.toReal((LargeInteger)value), (Real)other);
            }

        } else if (value instanceof Rational) {
            if(other instanceof LargeInteger) {
                return 0-that.compareTo(this);
            } else if(other instanceof Rational) {
                return ((Rational) value).compareTo((Rational) other);
            } else if(other instanceof Real) {
                return compareReals(Coerce.toReal((Rational)value), (Real)other);
            }

        } else if( value instanceof Real) {
            if(other instanceof LargeInteger) {
                return 0-that.compareTo(this);
            } else if(other instanceof Rational) {
                return 0-that.compareTo(this);
            } else if (other instanceof Real) {
                return compareReals((Real) this.value, (Real) other);
            }
        }

        throw new UnsupportedOperationException("Unable to compare "+value.getClass()+" to "+value.getClass());
    }

    private int compareReals(Real left, Real right) {
        if(left.approximates(right))
            return 0;
        else
            return left.compareTo(right);
    }

    public static Collector<? super NumericValue, NumericValueSummaryStatistics, NumericValueSummaryStatistics> summarizingCollector() {
        return new Collector<NumericValue, NumericValueSummaryStatistics, NumericValueSummaryStatistics>() {
            @Override
            public Supplier<NumericValueSummaryStatistics> supplier() {
                return () -> new NumericValueSummaryStatistics();
            }

            @Override
            public BiConsumer<NumericValueSummaryStatistics, NumericValue> accumulator() {
                return NumericValueSummaryStatistics::accumulate;
            }

            @Override
            public BinaryOperator<NumericValueSummaryStatistics> combiner() {
                return NumericValueSummaryStatistics::combine;
            }

            @Override
            public Function<NumericValueSummaryStatistics, NumericValueSummaryStatistics> finisher() {
                return NumericValueSummaryStatistics::finish;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
            }
        };
    }

    public boolean isGreaterThan(NumericValue other) {
        return this.compareTo(other) > 0;
    }

    public boolean isLessThan(NumericValue other) {
        return this.compareTo(other) < 0;
    }

    public NumericValue abs() {
        if(this.isLessThan(NumericValue.ZERO)) {
            return this.negate();
        } else {
            return this;
        }
    }
}

