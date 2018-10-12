package org.jasome.metrics.value;

public class NumericValueSummaryStatistics {

    private NumericValue total = null;
    private NumericValue count = NumericValue.ZERO;

    public NumericValue getSum() {
        return total != null ? total : NumericValue.ZERO;
    }

    public NumericValue getCount() {
        return count;
    }

    public NumericValue getMax() {
        return max != null ? max : NumericValue.ZERO;
    }

    public NumericValue getMin() {
        return min != null ? min : NumericValue.ZERO;
    }

    public NumericValue getAverage() {
        return total != null ? total.divide(count) : NumericValue.ZERO;
    }

    private NumericValue max = null;
    private NumericValue min = null;

    public NumericValueSummaryStatistics() {

    }

    public NumericValueSummaryStatistics(NumericValue total, NumericValue count, NumericValue max, NumericValue min) {
        this.total = total;
        this.count = count;
        this.max = max;
        this.min = min;
    }

    public static void accumulate(NumericValueSummaryStatistics numericValueSummaryStatistics, NumericValue numericValue) {
        numericValueSummaryStatistics.add(numericValue);
    }

    private void add(NumericValue numericValue) {
        
        if(total == null) {
            total = numericValue;
        } else {
            total = total.plus(numericValue);
        }

        if(max==null) {
            max = numericValue;
        } else {
            max = NumericValue.max(max, numericValue);
        }

        if(min == null) {
            min = numericValue;
        } else {
            min = NumericValue.min(max, numericValue);
        }

        count = count.plus(NumericValue.ONE);

    }

    public static NumericValueSummaryStatistics combine(NumericValueSummaryStatistics left, NumericValueSummaryStatistics right) {
        if(left.count == NumericValue.ZERO) return right;
        if(right.count == NumericValue.ZERO) return left;

        return new NumericValueSummaryStatistics(
            left.total.plus(right.total),
                left.count.plus(right.count),
                NumericValue.max(left.max, right.max),
                NumericValue.min(left.min, right.min)
        );
    }

    public static NumericValueSummaryStatistics finish(NumericValueSummaryStatistics numericValueSummaryStatistics) {
        return numericValueSummaryStatistics;
    }
}
