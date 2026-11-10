package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class Vector3v {

    private final IValue x;

    private final IValue y;

    private final IValue z;

    private Vector3f vector;

    public Vector3v(IValue x, IValue y, IValue z) {
        this.x = x;
        this.y = y;
        this.z = z;

    }

    public Vector3f eval(ExpressionEvaluator<?> evaluator) {
        // Only a small fraction of the catalog's frames is ever sampled.
        // Retain each frame's scratch vector only once it is actually used.
        if (this.vector == null) this.vector = new Vector3f();
        return this.vector.set(x.evalAsFloat(evaluator),
                y.evalAsFloat(evaluator),
                z.evalAsFloat(evaluator));
    }
}
