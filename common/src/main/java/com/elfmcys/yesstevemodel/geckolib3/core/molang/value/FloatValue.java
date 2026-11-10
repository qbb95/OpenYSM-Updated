package com.elfmcys.yesstevemodel.geckolib3.core.molang.value;

import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;

public class FloatValue implements IValue {

    public static final FloatValue ONE = new FloatValue(1.0f);

    public static final FloatValue ZERO = new FloatValue(0.0f);
    private static final FloatValue NEGATIVE_ZERO = new FloatValue(-0.0f);
    private static final int INTEGER_LOW = -16;
    private static final int INTEGER_HIGH = 16;
    private static final FloatValue[] INTEGERS = new FloatValue[INTEGER_HIGH - INTEGER_LOW + 1];

    static {
        for (int i = INTEGER_LOW; i <= INTEGER_HIGH; i++) {
            INTEGERS[i - INTEGER_LOW] = i == 0 ? ZERO : i == 1 ? ONE : new FloatValue(i);
        }
    }

    private final float value;

    public static FloatValue of(float value) {
        if (Float.isNaN(value)) return ZERO;
        if (value == 0.0f) return Float.floatToRawIntBits(value) == 0 ? ZERO : NEGATIVE_ZERO;
        int integer = (int) value;
        if (value == integer && integer >= INTEGER_LOW && integer <= INTEGER_HIGH) {
            return INTEGERS[integer - INTEGER_LOW];
        }
        return new FloatValue(value);
    }

    public FloatValue(float value) {
        if (!Float.isNaN(value)) {
            this.value = value;
        } else {
            this.value = 0.0f;
        }
    }

    @Override
    public float evalAsFloat(ExpressionEvaluator<?> evaluator) {
        return this.value;
    }

    @Override
    public int evalAsInt(ExpressionEvaluator<?> evaluator) {
        return (int) this.value;
    }

    @Override
    public boolean evalAsBoolean(ExpressionEvaluator<?> evaluator) {
        return this.value != 0.0f;
    }

    @Override
    public Object evalSafe(ExpressionEvaluator<?> evaluator) {
        return Float.valueOf(this.value);
    }

    @Override
    public Object evalUnsafe(ExpressionEvaluator<?> evaluator) {
        return Float.valueOf(this.value);
    }

    public float value() {
        return this.value;
    }
}
