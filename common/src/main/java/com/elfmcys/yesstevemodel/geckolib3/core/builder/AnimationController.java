package com.elfmcys.yesstevemodel.geckolib3.core.builder;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

/**
 * 动画控制器解析
 */
public class AnimationController {
    // initial_state
    private final int stateId;

    // 控制器内容
    private final Int2ReferenceMap<AnimationState> states;

    public AnimationController(String initialState, AnimationState[] animationStates) {
        this.stateId = StringPool.computeIfAbsent(initialState);
        // 同名 state 在后出现的覆盖先出现的；不能用 Collectors.toMap，重名会直接抛异常让整个模型加载失败
        Int2ReferenceOpenHashMap<AnimationState> byId = new Int2ReferenceOpenHashMap<>(animationStates.length);
        for (AnimationState state : animationStates) {
            byId.put(state.getHashId(), state);
        }
        this.states = Int2ReferenceMaps.unmodifiable(byId);
    }

    public int getStateId() {
        return this.stateId;
    }

    public Int2ReferenceMap<AnimationState> getStates() {
        return this.states;
    }
}